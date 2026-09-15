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
class CustomPaintRealSdkTest {
 @TempDir(cleanup=org.junit.jupiter.api.io.CleanupMode.ON_SUCCESS) Path project;
 private Path sdk,flutter,dart,lib;
 private static final StableId GROUP_ID=StableId.random(),NESTED_ID=StableId.random();
 private static final SlotName CHILDREN=new SlotName("children");
 private final Map<String,String> cases=new LinkedHashMap<>();
 private int runtimeCaseCount;
 private static final List<String> FIELDS=List.of("painter","foregroundPainter","size");
 @Test void allSourcesPersistenceStrictTypesAndGeneratedNativePaint() throws Exception {
  initialize();
  Files.writeString(lib.resolve("references.dart"), references());
  var baseline=open(group(GROUP_ID,List.of()),"probe.dart", """
static CustomPainter? get painter=>refs.painter;
static CustomPainter? painterFactory()=>painter;
static CustomPainter? get foregroundPainter=>refs.foregroundPainter;
static CustomPainter? foregroundPainterFactory()=>foregroundPainter;
static Size get size=>refs.size;
static Size sizeFactory()=>size;
CustomPainter? get _painter=>painter;
CustomPainter? _painterFactory()=>painter;
CustomPainter? get _foregroundPainter=>foregroundPainter;
CustomPainter? _foregroundPainterFactory()=>foregroundPainter;
Size get _size=>size;
Size _sizeFactory()=>size;
CustomPainter? get _nullPainter=>null;
Size? get _nullableSize=>null;
String get _wrong=>'wrong';
dynamic get _dynamic=>null;
void _callback(Canvas canvas,Size size) {}
// User member is preserved.
int _userValue() => 73;
""");
  baseline=reopen(baseline,"import 'package:flutter/material.dart';\nimport 'references.dart' as refs;\n"+source(baseline));
  var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(CustomPaintWidgetPropertySchema.TYPE).orElseThrow(),NESTED_ID);
  var base=apply(baseline,new AddWidget(place(GROUP_ID,0),prototype));
  assertAnalysis(baseline,base,"probe.dart",true); record("creation",base,"empty");
  assertArrayEquals(baseline.current().dartCandidateBytes(),base.undo().session().current().dartCandidateBytes());
  assertArrayEquals(base.current().dartCandidateBytes(),base.undo().session().redo().session().current().dartCandidateBytes());
  base=reopen(base,source(base));
  DesignerCommandSession painted=null;
  for(boolean imported:List.of(false,true)) for(boolean member:List.of(false,true)) for(boolean factory:List.of(false,true)) {
   var patches=new ArrayList<PatchProperties.Patch>();
   for(String field:FIELDS) patches.add(new PatchProperties.SetPatch(new PropertyName(field),reference(field,imported,member,factory)));
   patches.add(new PatchProperties.SetPatch(new PropertyName("isComplex"),new PropertyValue.BooleanValue(true)));
   patches.add(new PatchProperties.SetPatch(new PropertyName("willChange"),new PropertyValue.BooleanValue(true)));
   var changed=apply(base,new PatchProperties(NESTED_ID,patches));
   assertAnalysis(base,changed,"probe.dart",true); record("sources_"+imported+"_"+member+"_"+factory,changed,"source");
   if(painted==null) painted=reopen(changed,source(changed));
  }
  for (String field:List.of("painter","foregroundPainter")) {
   var constant=new PropertyValue.DartObjectReferenceValue(Optional.of("package:custom_paint_contract/references.dart"),"ConstPainter",
       Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION,Optional.of(true));
   var changed=apply(base,new SetProperty(NESTED_ID,new PropertyName(field),constant));
   assertAnalysis(base,changed,"probe.dart",true); saveCase("constant_"+field.toLowerCase(),changed);
   assertTrue(source(changed).contains("const "));
  }
  var local=apply(base,new SetProperty(NESTED_ID,new PropertyName("size"),new PropertyValue.SizeValue(new BigDecimal("48"),new BigDecimal("32"))));
  assertAnalysis(base,local,"probe.dart",true); record("local",local,"local");
  for(String field:List.of("painter","foregroundPainter")) {
   var explicit=apply(base,new SetProperty(NESTED_ID,new PropertyName(field),new PropertyValue.NullValue()));
   assertAnalysis(base,explicit,"probe.dart",true); record("null_"+field.toLowerCase(),explicit,"empty");
   var nullable=apply(base,new SetProperty(NESTED_ID,new PropertyName(field),ref("_nullPainter")));
   assertAnalysis(base,nullable,"probe.dart",true); record("nullable_"+field.toLowerCase(),nullable,"empty");
  }
  for(String field:FIELDS) for(String bad:List.of("_wrong","_dynamic","_callback"))
   assertAnalysis(base,apply(base,new SetProperty(NESTED_ID,new PropertyName(field),ref(bad))),"probe.dart",false);
  assertAnalysis(base,apply(base,new SetProperty(NESTED_ID,new PropertyName("size"),ref("_nullableSize"))),"probe.dart",false);
  var child=adapter("Child",16);
  var added=apply(painted,new AddWidget(new WidgetPlacement(NESTED_ID,new SlotName("child"),0),child));
  assertAnalysis(painted,added,"probe.dart",true); record("child",added,"child");
  assertArrayEquals(painted.current().dartCandidateBytes(),added.undo().session().current().dartCandidateBytes());
  var afterChild=reopen(added,source(added));
  var removedChild=apply(afterChild,new RemoveWidget(child.id()));
  assertAnalysis(afterChild,removedChild,"probe.dart",true); record("removed_child",removedChild,"source");
  var withLocal=reopen(local,source(local));
  var reset=apply(withLocal,new ResetProperty(NESTED_ID,new PropertyName("size")));
  assertAnalysis(withLocal,reset,"probe.dart",true); record("reset",reset,"empty");
  var removed=apply(base,new RemoveWidget(NESTED_ID));
  assertAnalysis(base,removed,"probe.dart",true);
  assertArrayEquals(base.current().dartCandidateBytes(),removed.undo().session().current().dartCandidateBytes());
  Files.writeString(Files.createDirectories(project.resolve("test")).resolve("custom_paint_test.dart"),runtime());
  run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");
 }
 private void record(String name,DesignerCommandSession s,String mode) throws Exception { saveCase(name,s); cases.put(name,mode); }
 private static PropertyValue.DartObjectReferenceValue ref(String name) {
  return new PropertyValue.DartObjectReferenceValue(Optional.empty(),name,Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
 }
 private static PropertyValue.DartObjectReferenceValue reference(String field,boolean imported,boolean member,boolean factory) {
  return new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:custom_paint_contract/references.dart"):Optional.empty(),
   member?(imported?"Values":"Storage"):(imported?"":"_")+field+(factory?"Factory":""),
   member?Optional.of(field+(factory?"Factory":"")):Optional.empty(),
   factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
   factory?Optional.of(false):Optional.empty());
 }
 private String references() {return """
import 'package:flutter/material.dart';
import 'package:flutter/semantics.dart';
class PaintSignal extends ChangeNotifier {
 int adds=0,removes=0;
 @override void addListener(VoidCallback listener){adds++;super.addListener(listener);}
 @override void removeListener(VoidCallback listener){removes++;super.removeListener(listener);}
 void tick()=>notifyListeners();
}
final signal=PaintSignal();
final order=<String>[];
class TestPainter extends CustomPainter {
 TestPainter(this.front):super(repaint:signal);
 final bool front;
 @override void paint(Canvas canvas,Size size){
  order.add(front?'front':'back');
  canvas.drawRect(front?const Rect.fromLTWH(0,0,8,8):Offset.zero&size,Paint()..color=front?const Color(0xff0000ff):const Color(0xffff0000));
 }
 @override bool shouldRepaint(TestPainter old)=>old.front!=front;
 @override bool hitTest(Offset p)=>p.dx<8&&p.dy<8;
 @override bool shouldRebuildSemantics(TestPainter old)=>old.front!=front;
 @override SemanticsBuilderCallback get semanticsBuilder => (size)=>[
  CustomPainterSemantics(rect:Offset.zero&size,properties:SemanticsProperties(label:front?'front art':'back art',textDirection:TextDirection.ltr))
 ];
}
CustomPainter? get painter=>TestPainter(false);
CustomPainter? painterFactory()=>painter;
CustomPainter? get foregroundPainter=>TestPainter(true);
CustomPainter? foregroundPainterFactory()=>foregroundPainter;
Size get size=>const Size(48,32);
Size sizeFactory()=>size;
class ConstPainter extends CustomPainter {
 const ConstPainter();
 @override void paint(Canvas canvas,Size size) {}
 @override bool shouldRepaint(ConstPainter old)=>false;
}
class Values {
 static CustomPainter? get painter=>TestPainter(false);
 static CustomPainter? painterFactory()=>painter;
 static CustomPainter? get foregroundPainter=>TestPainter(true);
 static CustomPainter? foregroundPainterFactory()=>foregroundPainter;
 static Size get size=>const Size(48,32);
 static Size sizeFactory()=>size;
}
""";}
 private String runtime() {
  StringBuilder out=new StringBuilder("""
import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:custom_paint_contract/references.dart' as refs;
""");
  for(String name:cases.keySet()) out.append("import 'package:custom_paint_contract/"+name+".dart' as "+name+";\n");
  out.append("void main(){\n"); runtimeCaseCount=0;
  for(var entry:cases.entrySet()) for(String direction:List.of("ltr","rtl")){
   String name=entry.getKey(),mode=entry.getValue(); boolean painted=mode.equals("source")||mode.equals("child");
   out.append("testWidgets('"+name+" "+direction+"',(tester)async{\n");
   out.append("""
 final semantics=tester.ensureSemantics();refs.order.clear();
 final adds=refs.signal.adds,removes=refs.signal.removes;
 const boundaryKey=ValueKey('pixels');
""");
   out.append("await tester.pumpWidget(MaterialApp(home:RepaintBoundary(key:boundaryKey,child:Center(child:Directionality(textDirection:TextDirection."+direction+",child:const "+name+".Sample())))));\n");
   out.append("""
""");
   out.append(" final finder=find.descendant(of:find.byType("+name+".Sample),matching:find.byType(CustomPaint));\n");
   out.append("""

 final w=tester.widget<CustomPaint>(finder),r=tester.renderObject<RenderCustomPaint>(finder);
 expect(tester.takeException(),isNull);
""");
   out.append("expect(w.size,"+(mode.equals("empty")?"Size.zero":"const Size(48,32)")+");\n");
   out.append("expect(r.size,"+(mode.equals("empty")?"Size.zero":mode.equals("child")?"const Size(16,16)":"const Size(48,32)")+");\n");
   if(painted)out.append("""
 expect(w.isComplex,isTrue);expect(w.willChange,isTrue);
 expect(refs.order,containsAllInOrder(['back','front']));
 expect(w.painter!.hitTest(const Offset(2,2)),isTrue);
 expect(w.painter!.hitTest(const Offset(12,12)),isFalse);
 final semanticsTree=tester.getSemantics(finder).toStringDeep();
 expect(semanticsTree,contains('back art'));expect(semanticsTree,contains('front art'));
 final renderBefore=r, count=refs.order.length;refs.signal.tick();await tester.pump();
 expect(refs.order.length,greaterThan(count));expect(tester.renderObject(finder),same(renderBefore));
 final boundary=tester.renderObject<RenderRepaintBoundary>(find.byKey(boundaryKey));
 final origin=r.localToGlobal(Offset.zero,ancestor:boundary);
 final image=(await tester.runAsync(()=>boundary.toImage()))!;
 try {
  final bytes=(await tester.runAsync(()=>image.toByteData(format:ui.ImageByteFormat.rawRgba)))!;
  final offset=((origin.dy+2).floor()*image.width+(origin.dx+2).floor())*4;
  expect([bytes.getUint8(offset),bytes.getUint8(offset+1),bytes.getUint8(offset+2)],[0,0,255]);
 } finally {image.dispose();}
""");
   else out.append("expect(w.painter,isNull);expect(w.foregroundPainter,isNull);expect(w.isComplex,isFalse);expect(w.willChange,isFalse);\n");
   out.append("""
 await tester.pumpWidget(const SizedBox());
 expect(refs.signal.adds-adds,refs.signal.removes-removes);
 expect(tester.takeException(),isNull);semantics.dispose();
});
""");
   runtimeCaseCount++;
  }
  return out.append("}\n").toString();
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
                name: custom_paint_contract
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
            System.out.println("CustomPaint pinned SDK: "+runtimeCaseCount+" generated/native runtime cases passed.");
        }
    }
}
