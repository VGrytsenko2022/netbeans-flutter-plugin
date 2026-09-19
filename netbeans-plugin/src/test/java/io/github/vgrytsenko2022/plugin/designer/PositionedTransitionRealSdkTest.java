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
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="flutter.events.sdk",matches=".+")
class PositionedTransitionRealSdkTest {
    @TempDir Path project;
    private Path sdk,flutter,dart,lib;
    private static final StableId GROUP_ID=StableId.random(),NESTED_ID=StableId.random();
    private static final SlotName CHILDREN=new SlotName("children");
    private final Map<String,String> cases=new LinkedHashMap<>();
    private int runtimeCaseCount;
    @Test void fullRelativeRectSourcesTransactionalWrapAndLiveNativeLifecycle()throws Exception{
        initialize();
        Files.writeString(lib.resolve("references.dart"),"""
            import 'package:flutter/widgets.dart';
            Animation<RelativeRect> get rect=>RectValues.current;
            Animation<RelativeRect> rectFactory()=>RectValues.current;
            class RectValues {
              static Animation<RelativeRect> current=const AlwaysStoppedAnimation<RelativeRect>(RelativeRect.fromLTRB(10,20,30,40));
              static Animation<RelativeRect> rectFactory()=>current;
            }
            """);
        var body=adapter("Sized body",100);
        var baseline=open(group(GROUP_ID,List.of(body,adapter("Sibling",20))),"probe.dart","""
            // User member is preserved.
            int _userValue()=>73;
            static Animation<RelativeRect> current=const AlwaysStoppedAnimation<RelativeRect>(RelativeRect.fromLTRB(10,20,30,40));
            static Animation<RelativeRect> rectFactory()=>current;
            Animation<RelativeRect> get _rect=>current;
            Animation<RelativeRect> _rectFactory()=>current;
            RelativeRect get _plain=>RelativeRect.fill;
            Animation<RelativeRect>? get _nullableOuter=>current;
            Animation<RelativeRect?> get _nullableValue=>const AlwaysStoppedAnimation<RelativeRect?>(null);
            Animation<Rect> get _wrongRect=>const AlwaysStoppedAnimation<Rect>(Rect.zero);
            Animation<Object> get _objectAnimation=>const AlwaysStoppedAnimation<Object>(RelativeRect.fill);
            Animation<dynamic> get _dynamicAnimation=>const AlwaysStoppedAnimation<dynamic>('bad');
            ValueNotifier<RelativeRect> get _notAnimation=>ValueNotifier<RelativeRect>(RelativeRect.fill);
            dynamic get _dynamic=>current;
            Object get _object=>current;
            """.replace("int _userValue()=>73;","int _userValue() => 73;"));
        var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(PositionedTransitionWidgetPropertySchema.TYPE).orElseThrow(),NESTED_ID);
        var wrapped=apply(baseline,new WrapWidget(body.id(),prototype,new SlotName("child"),0));
        assertAnalysis(baseline,wrapped,"probe.dart",true);
        assertArrayEquals(baseline.current().dartCandidateBytes(),wrapped.undo().session().current().dartCandidateBytes());
        assertArrayEquals(wrapped.current().dartCandidateBytes(),wrapped.undo().session().redo().session().current().dartCandidateBytes());
        recordCase("fill",wrapped,"0,0,0,0");wrapped=reopen(wrapped,source(wrapped));
        var locals=List.of(List.of("10","20","30","40"),List.of("-10","-20","-30","-40"),
            List.of("10.5","20.25","30.75","40.125"),List.of("10","20","230","140"),List.of("300","200","80","60"));
        int index=0;
        for(var values:locals){
            var patches=new ArrayList<PatchProperties.Patch>();
            for(int i=0;i<4;i++)patches.add(new PatchProperties.SetPatch(new PropertyName(PositionedTransitionWidgetPropertySchema.EDGES.get(i)),
                new PropertyValue.DoubleValue(new java.math.BigDecimal(values.get(i)))));
            var local=apply(wrapped,new PatchProperties(NESTED_ID,patches));
            assertAnalysis(wrapped,local,"probe.dart",true);recordCase("local"+index++,local,String.join(",",values));
        }
        for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
            var changed=apply(wrapped,new SetProperty(NESTED_ID,new PropertyName("rect"),reference(imported,member,factory)));
            assertAnalysis(wrapped,changed,"probe.dart",true);recordCase(name(imported,member,factory),changed,"10,20,30,40");
        }
        var live=apply(wrapped,new SetProperty(NESTED_ID,new PropertyName("rect"),reference(true,true,false)));
        assertAnalysis(wrapped,live,"probe.dart",true);recordCase("live",live,"10,20,30,40");live=reopen(live,source(live));
        var inactive=apply(live,new SetProperty(NESTED_ID,new PropertyName("rectRight"),new PropertyValue.DoubleValue(new java.math.BigDecimal("-99"))));
        assertAnalysis(live,inactive,"probe.dart",true);recordCase("inactive",inactive,"10,20,30,40");inactive=reopen(inactive,source(inactive));
        var localAgain=apply(inactive,new SetProperty(NESTED_ID,new PropertyName("rect"),new PropertyValue.StringValue("local")));
        assertAnalysis(inactive,localAgain,"probe.dart",true);recordCase("local_again",localAgain,"0,0,-99,0");
        var replaced=apply(wrapped,new ReplaceSlotChild(NESTED_ID,new SlotName("child"),body.id(),new ReplaceSlotChild.NewSubtree(adapter("Sized body",100))));
        assertAnalysis(wrapped,replaced,"probe.dart",true);recordCase("replaced",replaced,"0,0,0,0");
        var moved=apply(wrapped,new MoveWidget(NESTED_ID,new WidgetPlacement(GROUP_ID,CHILDREN,1)));
        assertAnalysis(wrapped,moved,"probe.dart",true);recordCase("moved",moved,"0,0,0,0");
        assertFalse(wrapped.apply(new RemoveWidget(body.id())).changed());
        assertFalse(wrapped.apply(new MoveWidget(body.id(),new WidgetPlacement(GROUP_ID,CHILDREN,2))).changed());
        assertFalse(wrapped.apply(new ResetProperty(NESTED_ID,new PropertyName("rect"))).changed());
        for(String bad:List.of("_plain","_nullableOuter","_nullableValue","_wrongRect","_objectAnimation","_dynamicAnimation","_notAnimation","_dynamic","_object")){
            assertAnalysis(wrapped,apply(wrapped,new SetProperty(NESTED_ID,new PropertyName("rect"),ref(bad,false))),"probe.dart",false);
        }
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("position_test.dart"),runtime());
        run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");
    }
    private void recordCase(String name,DesignerCommandSession session,String insets)throws Exception{saveCase(name,session);cases.put(name,insets);}
    private static String name(boolean imported,boolean member,boolean factory){return "ref_"+(imported?"imported":"current")+"_"+(member?"member":"root")+"_"+(factory?"factory":"getter");}
    private static PropertyValue.DartObjectReferenceValue reference(boolean imported,boolean member,boolean factory){
        return new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:positioned_transition_contract/references.dart"):Optional.empty(),
            member?(imported?"RectValues":"Storage"):(imported?"":"_")+(factory?"rectFactory":"rect"),
            member?Optional.of(factory?"rectFactory":"current"):Optional.empty(),
            factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
            factory?Optional.of(false):Optional.empty());
    }
    private String runtime(){
        runtimeCaseCount=cases.size()*2+11;
        var imports=new StringBuilder("""
            import 'dart:math' as math;
            import 'package:flutter/material.dart';
            import 'package:flutter/rendering.dart';
            import 'package:flutter_test/flutter_test.dart';
            import 'package:positioned_transition_contract/references.dart' as refs;
            """);
        var map=new StringBuilder("final cases=<String,(Widget,RelativeRect)>{");
        cases.forEach((name,rect)->{imports.append("import '../lib/").append(name).append(".dart' as case_").append(name).append(";\n");
            map.append("'").append(name).append("':(const case_").append(name).append(".Sample(),const RelativeRect.fromLTRB(").append(rect).append(")),");});
        return imports+"""
            Widget host(Widget child,{bool rtl=false,double width=240})=>MaterialApp(home:Directionality(
              textDirection:rtl?TextDirection.rtl:TextDirection.ltr,child:Center(child:SizedBox(width:width,height:160,child:child))));
            Finder native()=>find.byType(PositionedTransition);
            void check(WidgetTester tester,RelativeRect rect,{double width=240}){
              final n=tester.widget<PositionedTransition>(native());expect(n.rect.value,rect);
              final box=tester.renderObject<RenderBox>(native()),pd=box.parentData as StackParentData,parent=box.parent as RenderStack;
              expect(parent.size,Size(width,160));
              expect(box.size,Size(math.max(0.0,width-rect.right-rect.left),math.max(0.0,160-rect.bottom-rect.top)));
              expect(pd.offset,Offset(rect.left,rect.top));expect(pd.left,rect.left);expect(pd.right,rect.right);
              expect(pd.top,rect.top);expect(pd.bottom,rect.bottom);expect(pd.width,isNull);expect(pd.height,isNull);
              expect(tester.takeException(),isNull);
            }
            class ProbeAnimation extends Animation<RelativeRect>{
              ProbeAnimation(this._value);RelativeRect _value;final listeners=<VoidCallback>{};
              @override RelativeRect get value=>_value;
              void update(RelativeRect value){_value=value;for(final f in List<VoidCallback>.of(listeners)){f();}}
              @override AnimationStatus get status=>AnimationStatus.forward;
              @override void addListener(VoidCallback f)=>listeners.add(f);
              @override void removeListener(VoidCallback f)=>listeners.remove(f);
              @override void addStatusListener(AnimationStatusListener f){}
              @override void removeStatusListener(AnimationStatusListener f){}
            }
            """+"void main(){"+map+"};"+"""
            for(final entry in cases.entries)for(final rtl in [false,true]){
              testWidgets('generated saved RelativeRect ${entry.key} rtl=$rtl',(tester)async{
                await tester.pumpWidget(host(entry.value.$1,rtl:rtl));await tester.pumpAndSettle();check(tester,entry.value.$2);
                expect(find.text('Sized body'),findsOneWidget);
                final state=tester.state(native()),child=tester.element(find.text('Sized body'));
                await tester.pumpWidget(host(entry.value.$1,rtl:rtl,width:300));check(tester,entry.value.$2,width:300);
                expect(identical(state,tester.state(native())),true);expect(identical(child,tester.element(find.text('Sized body'))),true);
              });
            }
            testWidgets('live animation replacement detaches old listener and preserves child',(tester)async{
              final first=ProbeAnimation(const RelativeRect.fromLTRB(10,20,30,40)),second=ProbeAnimation(RelativeRect.fill);
              refs.RectValues.current=first;await tester.pumpWidget(host(case_live.Sample()));await tester.pumpAndSettle();
              final state=tester.state(native()),child=tester.element(find.text('Sized body'));expect(first.listeners,hasLength(1));
              first.update(const RelativeRect.fromLTRB(10,20,230,140));await tester.pump();check(tester,first.value);
              refs.RectValues.current=second;await tester.pumpWidget(host(case_live.Sample()));
              expect(first.listeners,isEmpty);expect(second.listeners,hasLength(1));check(tester,RelativeRect.fill);
              first.update(const RelativeRect.fromLTRB(99,99,99,99));await tester.pump();check(tester,RelativeRect.fill);
              second.update(const RelativeRect.fromLTRB(-10,-20,-30,-40));await tester.pump();check(tester,second.value);
              expect(identical(state,tester.state(native())),true);expect(identical(child,tester.element(find.text('Sized body'))),true);
              await tester.pumpWidget(const SizedBox());expect(second.listeners,isEmpty);
            });
            for(final rtl in [false,true]){
              testWidgets('real RelativeRectTween controller frames and reversal rtl=$rtl',(tester)async{
                final controller=AnimationController(vsync:tester,duration:const Duration(milliseconds:100));
                const begin=RelativeRect.fromLTRB(10,20,30,40),end=RelativeRect.fromLTRB(50,60,70,80);
                refs.RectValues.current=RelativeRectTween(begin:begin,end:end).animate(controller);
                await tester.pumpWidget(host(case_live.Sample(),rtl:rtl));await tester.pumpAndSettle();
                final state=tester.state(native()),child=tester.element(find.text('Sized body'));
                controller.forward();await tester.pump();
                for(var i=1;i<=3;i++){await tester.pump(const Duration(milliseconds:20));check(tester,RelativeRect.lerp(begin,end,controller.value)!);expect(controller.value,closeTo(i*.2,1e-8));}
                controller.reverse();await tester.pump();await tester.pump(const Duration(milliseconds:20));
                expect(controller.value,lessThan(.6));check(tester,RelativeRect.lerp(begin,end,controller.value)!);
                await tester.pumpAndSettle();check(tester,begin);
                controller.forward();await tester.pumpAndSettle();check(tester,end);
                expect(identical(state,tester.state(native())),true);expect(identical(child,tester.element(find.text('Sized body'))),true);
                await tester.pumpWidget(const SizedBox());controller.dispose();expect(tester.takeException(),isNull);
              });
              for(final clip in Clip.values){
                testWidgets('pointer dispatch follows physical layout and Stack bounds rtl=$rtl clip=$clip',(tester)async{
                  var taps=0;final animation=ProbeAnimation(const RelativeRect.fromLTRB(10,20,30,40));
                  await tester.pumpWidget(host(Stack(clipBehavior:clip,children:[PositionedTransition(rect:animation,
                    child:GestureDetector(behavior:HitTestBehavior.opaque,onTap:()=>taps++,child:const SizedBox(key:ValueKey('body'))))]),rtl:rtl));
                  await tester.tapAt(tester.getCenter(find.byKey(const ValueKey('body'))));expect(taps,1);
                  animation.update(const RelativeRect.fromLTRB(-20,20,0,20));await tester.pump();taps=0;
                  final box=tester.renderObject<RenderBox>(native()),parent=box.parent as RenderStack;
                  await tester.tapAt(parent.localToGlobal(const Offset(-10,50)));expect(taps,0);
                  await tester.tapAt(parent.localToGlobal(const Offset(5,50)));expect(taps,1);
                  animation.update(const RelativeRect.fromLTRB(10,20,230,140));await tester.pump();taps=0;
                  await tester.tapAt(parent.localToGlobal(const Offset(10,20)));expect(taps,0);
                  expect(tester.takeException(),isNull);await tester.pumpWidget(const SizedBox());expect(animation.listeners,isEmpty);
                });
              }
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
        return new WidgetNode(id, new WidgetTypeId("flutter.widgets.Stack"), Map.of(), Map.of(CHILDREN, new WidgetSlot.ListSlot(children)));
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
                name: positioned_transition_contract
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
            System.out.println("PositionedTransition pinned SDK: "+runtimeCaseCount+" generated/native runtime cases passed.");
        }
    }
}
