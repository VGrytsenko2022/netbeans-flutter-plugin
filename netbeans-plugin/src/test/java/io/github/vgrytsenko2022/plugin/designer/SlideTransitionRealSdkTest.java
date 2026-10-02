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
class SlideTransitionRealSdkTest {
    @TempDir Path project;
    private Path sdk,flutter,dart,lib;
    private static final StableId GROUP_ID=StableId.random(),NESTED_ID=StableId.random();
    private static final SlotName CHILDREN=new SlotName("children");
    @Test void fullGenerationExactAnimationEvidenceReopenHistoryAndNativeLifecycle() throws Exception {
        initialize();
        Files.writeString(lib.resolve("references.dart"), """
            import 'package:flutter/widgets.dart';
            Animation<Offset> get animation => SlideValues.current;
            Animation<Offset> animationFactory() => SlideValues.current;
            class SlideValues {
              static Animation<Offset> current = const AlwaysStoppedAnimation<Offset>(Offset(.75,.25));
              static Animation<Offset> create() => current;
            }
            """);
        var body=adapter("Sliding body",100);
        var baseline=open(group(GROUP_ID,List.of(body)),"probe.dart","""
            // User member is preserved.
            int _userValue() => 73;
            static Animation<Offset> currentAnimation = const AlwaysStoppedAnimation<Offset>(Offset(.75,.25));
            static Animation<Offset> animationFactory() => currentAnimation;
            Animation<Offset> get _animation => currentAnimation;
            Animation<Offset> _animationFactory() => currentAnimation;
            Offset get _plain => const Offset(.75,.25);
            Animation<Offset>? get _nullableAnimation => currentAnimation;
            Animation<Offset?> get _nullableValue => const AlwaysStoppedAnimation<Offset?>(null);
            Animation<double> get _wrongValue => const AlwaysStoppedAnimation<double>(1);
            Animation<dynamic> get _genericDynamic => const AlwaysStoppedAnimation<dynamic>('bad');
            dynamic get _dynamic => currentAnimation;
            ValueNotifier<Offset> get _notAnimation => ValueNotifier<Offset>(Offset.zero);
            """);
        var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(SlideTransitionWidgetPropertySchema.TYPE).orElseThrow(),NESTED_ID);
        var current=apply(baseline,new WrapWidget(body.id(),prototype,new SlotName("child"),0));
        assertAnalysis(baseline,current,"probe.dart",true);
        assertArrayEquals(baseline.current().dartCandidateBytes(),current.undo().session().current().dartCandidateBytes());
        assertArrayEquals(current.current().dartCandidateBytes(),current.undo().session().redo().session().current().dartCandidateBytes());
        saveCase("zero",current);current=reopen(current,source(current));
        var changed=apply(current,new SetProperty(NESTED_ID,new PropertyName("position"),new PropertyValue.OffsetValue(new java.math.BigDecimal("0.75"),new java.math.BigDecimal("0.25"))));
        assertAnalysis(current,changed,"probe.dart",true);saveCase("fractional",changed);var fractional=reopen(changed,source(changed));
        for(String direction:List.of("ltr","rtl","null")){
            var directed=apply(fractional,new SetProperty(NESTED_ID,new PropertyName("textDirection"),direction.equals("null")?new PropertyValue.NullValue():new PropertyValue.EnumValue("TextDirection",direction)));
            assertAnalysis(fractional,directed,"probe.dart",true);saveCase(direction,directed);
        }
        var hit=apply(fractional,new SetProperty(NESTED_ID,new PropertyName("transformHitTests"),new PropertyValue.BooleanValue(false)));
        assertAnalysis(fractional,hit,"probe.dart",true);saveCase("hit_false",hit);
        hit=reopen(hit,source(hit));
        var hitReset=apply(hit,new ResetProperty(NESTED_ID,new PropertyName("transformHitTests")));
        assertAnalysis(hit,hitReset,"probe.dart",true);saveCase("hit_reset",hitReset);
        var empty=apply(current,new RemoveWidget(body.id()));assertAnalysis(current,empty,"probe.dart",true);saveCase("empty",empty);
        var moved=apply(current,new MoveWidget(body.id(),new WidgetPlacement(GROUP_ID,CHILDREN,1)));assertAnalysis(current,moved,"probe.dart",true);saveCase("moved",moved);
        assertFalse(current.apply(new ResetProperty(NESTED_ID,new PropertyName("position"))).changed());
        for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
            changed=apply(current,new SetProperty(NESTED_ID,new PropertyName("position"),animationReference(imported,member,factory)));
            assertAnalysis(current,changed,"probe.dart",true);saveCase(referenceCase(imported,member,factory),changed);
        }
        for(String bad:List.of("_plain","_nullableAnimation","_nullableValue","_wrongValue","_genericDynamic","_dynamic","_notAnimation"))
            assertAnalysis(current,apply(current,new SetProperty(NESTED_ID,new PropertyName("position"),ref(bad,false))),"probe.dart",false);
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("slide_test.dart"),runtime());
        run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");
    }
    private static String referenceCase(boolean imported,boolean member,boolean factory) {
        return "ref_"+(imported?"imported":"current")+"_"+(member?"member":"root")+"_"+(factory?"factory":"getter");
    }
    private static PropertyValue.DartObjectReferenceValue animationReference(boolean imported,boolean member,boolean factory) {
        String root=member?(imported?"SlideValues":"Storage"):(imported?(factory?"animationFactory":"animation"):(factory?"_animationFactory":"_animation"));
        String accessor=imported?(factory?"create":"current"):(factory?"animationFactory":"currentAnimation");
        return new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:slide_transition_contract/references.dart"):Optional.empty(),
            root,member?Optional.of(accessor):Optional.empty(),
            factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
            factory?Optional.of(false):Optional.empty());
    }
    private static String runtime(){
        var imports=new StringBuilder("""
            import 'package:flutter/material.dart';
            import 'package:flutter/rendering.dart';
            import 'package:flutter_test/flutter_test.dart';
            import 'package:slide_transition_contract/references.dart' as refs;
            """);
        var names=new ArrayList<>(List.of("zero","fractional","ltr","rtl","null","hit_false","hit_reset","empty","moved"));
        for(boolean i:List.of(false,true))for(boolean m:List.of(false,true))for(boolean f:List.of(false,true))names.add(referenceCase(i,m,f));
        var cases=new StringBuilder("final cases=<String,Widget>{");
        for(String name:names){
            imports.append("import '../lib/").append(name).append(".dart' as case_").append(name).append(";\n");
            cases.append("'").append(name).append("':const case_").append(name).append(".Sample(),");
        }
        return imports.toString()+"""
            Widget host(Widget child,{bool rtl=false})=>MaterialApp(home:Directionality(textDirection:rtl?TextDirection.rtl:TextDirection.ltr,child:child));
            Finder native(Widget owner)=>find.descendant(of:find.byType(owner.runtimeType,skipOffstage:false),
              matching:find.byType(SlideTransition,skipOffstage:false),skipOffstage:false);
            class ProbeAnimation extends Animation<Offset>{
              ProbeAnimation(this._value);
              Offset _value;
              final listeners=<VoidCallback>{};
              @override Offset get value=>_value;
              void update(Offset value){_value=value;for(final f in List<VoidCallback>.of(listeners)){f();}}
              @override AnimationStatus get status=>AnimationStatus.forward;
              @override void addListener(VoidCallback f)=>listeners.add(f);
              @override void removeListener(VoidCallback f)=>listeners.remove(f);
              @override void addStatusListener(AnimationStatusListener listener){}
              @override void removeStatusListener(AnimationStatusListener listener){}
            }
            """+"void main(){"+cases+"};"+"""
            for(final e in cases.entries)for(final rtl in [false,true]){
              testWidgets('generated ${e.key} rtl=$rtl',(tester)async{
                await tester.pumpWidget(host(e.value,rtl:rtl));await tester.pumpAndSettle();
                final finder=native(e.value),n=tester.widget<SlideTransition>(finder);
                final empty=e.key=='empty'||e.key=='moved';
                final offset=e.key=='zero'||empty?Offset.zero:const Offset(.75,.25);
                expect(n.position.value,offset);expect(n.transformHitTests,e.key!='hit_false');
                expect(n.textDirection,e.key=='rtl'?TextDirection.rtl:e.key=='ltr'?TextDirection.ltr:null);
                expect(n.child==null,empty);
                final parent=tester.renderObject<RenderBox>(finder);
                expect(parent.size,empty?Size.zero:const Size(100,100));
                if(!empty){
                  final text=tester.renderObject<RenderBox>(find.text('Sliding body'));
                  expect(text.localToGlobal(Offset.zero,ancestor:parent),Offset(e.key=='rtl'?-100*offset.dx:100*offset.dx,100*offset.dy));
                  // This text-only generated tree still produces native paragraph hit-test entries.
                  final original=BoxHitTestResult(),translated=BoxHitTestResult();
                  parent.hitTest(original,position:const Offset(50,50));
                  parent.hitTest(translated,position:Offset(50+(e.key=='rtl'?-100:100)*offset.dx,50+100*offset.dy));
                  if(offset.dx!=0){
                    expect(original.path.any((h)=>identical(h.target,text)),e.key=='hit_false');
                    expect(translated.path.any((h)=>identical(h.target,text)),e.key!='hit_false');
                  }
                }
                expect(tester.takeException(),isNull);
              });
            }
            testWidgets('source changes replace listener without recreating State or child',(tester)async{
              final first=ProbeAnimation(const Offset(.25,.5)),second=ProbeAnimation(const Offset(-.5,.25));
              refs.SlideValues.current=first;
              final widget=case_ref_imported_member_getter.Sample();
              await tester.pumpWidget(host(widget));await tester.pumpAndSettle();
              final finder=native(widget),state=tester.state(native(widget)),child=tester.element(find.text('Sliding body'));
              expect(first.listeners,hasLength(1));
              first.update(const Offset(1.5,-.5));await tester.pump();
              final parent=tester.renderObject<RenderBox>(finder);
              expect(tester.renderObject<RenderBox>(find.text('Sliding body')).localToGlobal(Offset.zero,ancestor:parent),const Offset(150,-50));
              refs.SlideValues.current=second;
              await tester.pumpWidget(host(case_ref_imported_member_getter.Sample()));
              expect(first.listeners,isEmpty);expect(second.listeners,hasLength(1));
              expect(identical(state,tester.state(finder)),true);expect(identical(child,tester.element(find.text('Sliding body'))),true);
              first.update(const Offset(99,99));await tester.pump();expect(tester.widget<SlideTransition>(finder).position.value,const Offset(-.5,.25));
              second.update(const Offset(.25,.75));await tester.pump();
              expect(tester.renderObject<RenderBox>(find.text('Sliding body')).localToGlobal(Offset.zero,ancestor:parent),const Offset(25,75));
              await tester.pumpWidget(const SizedBox());expect(second.listeners,isEmpty);expect(tester.takeException(),isNull);
            });
            testWidgets('source AnimationController drives intermediate forward and reverse frames',(tester)async{
              final controller=AnimationController(vsync:tester,duration:const Duration(milliseconds:100));
              refs.SlideValues.current=Tween<Offset>(begin:const Offset(-.5,-.25),end:const Offset(.5,.75)).animate(controller);
              final widget=case_ref_imported_member_getter.Sample();
              await tester.pumpWidget(host(widget));
              final finder=native(widget),state=tester.state(native(widget)),child=tester.element(find.text('Sliding body'));
              controller.forward();await tester.pump();
              for(final fraction in [.25,.5,.75,1.0]){
                await tester.pump(const Duration(milliseconds:25));
                final value=tester.widget<SlideTransition>(finder).position.value;
                expect(value.dx,closeTo(-.5+fraction,1e-8));expect(value.dy,closeTo(-.25+fraction,1e-8));
              }
              controller.reverse();await tester.pump();await tester.pump(const Duration(milliseconds:50));
              expect(tester.widget<SlideTransition>(finder).position.value.dx,closeTo(0,1e-8));
              expect(identical(state,tester.state(finder)),true);expect(identical(child,tester.element(find.text('Sliding body'))),true);
              await tester.pumpWidget(const SizedBox());controller.dispose();expect(tester.takeException(),isNull);
            });
            for(final rtl in [false,true])for(final hit in [false,true]){
              testWidgets('native pointer routing rtl=$rtl hit=$hit',(tester)async{
                var taps=0;
                await tester.pumpWidget(host(Center(child:SlideTransition(position:const AlwaysStoppedAnimation<Offset>(Offset(.75,0)),
                  transformHitTests:hit,textDirection:rtl?TextDirection.rtl:TextDirection.ltr,
                  child:GestureDetector(behavior:HitTestBehavior.opaque,onTap:()=>taps++,child:const SizedBox(key:ValueKey('tap'),width:100,height:100))))));
                await tester.pumpAndSettle();
                final translated=tester.getCenter(find.byKey(const ValueKey('tap')));
                final original=translated-Offset(rtl?-75:75,0);
                await tester.tapAt(hit?translated:original);expect(taps,1);
                await tester.tapAt(hit?original:translated);expect(taps,1);
                expect(tester.takeException(),isNull);
              });
            }
            testWidgets('ancestor hit bounds constrain translated painting',(tester)async{
              var taps=0;
              await tester.pumpWidget(host(Center(child:SizedBox(width:100,height:100,
                child:SlideTransition(position:const AlwaysStoppedAnimation<Offset>(Offset(2,0)),
                  child:GestureDetector(behavior:HitTestBehavior.opaque,onTap:()=>taps++,
                    child:const SizedBox(key:ValueKey('outside'),width:100,height:100)))))));
              await tester.tapAt(tester.getCenter(find.byKey(const ValueKey('outside'))));
              expect(taps,0);expect(tester.takeException(),isNull);
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
                name: slide_transition_contract
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
            assertTrue(output.contains("+41: All tests passed!"), output);
            System.out.println("SlideTransition pinned SDK: 41 generated/native runtime cases passed.");
        }
    }
}
