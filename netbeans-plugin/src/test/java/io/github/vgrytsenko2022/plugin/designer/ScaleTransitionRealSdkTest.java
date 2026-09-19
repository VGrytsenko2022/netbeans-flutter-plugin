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
class ScaleTransitionRealSdkTest {
    @TempDir Path project;
    private Path sdk,flutter,dart,lib;
    private static final StableId GROUP_ID=StableId.random(),NESTED_ID=StableId.random();
    private static final SlotName CHILDREN=new SlotName("children");
    @Test void exactConstructorEvidenceSaveReopenHistoryAndLiveNativeAnimation() throws Exception {
        initialize();
        Files.writeString(lib.resolve("references.dart"), """
            import 'package:flutter/widgets.dart';
            Animation<double> get animation => ScaleValues.current;
            Animation<double> animationFactory() => ScaleValues.current;
            Alignment get alignment => ScaleValues.alignment;
            Alignment alignmentFactory() => ScaleValues.alignment;
            class ScaleValues {
              static Animation<double> current = const AlwaysStoppedAnimation<double>(1.5);
              static Animation<double> create() => current;
              static Alignment alignment = const Alignment(2,-1);
              static Alignment createAlignment() => alignment;
            }
            """);
        var body=adapter("Scaled body",100);
        var baseline=open(group(GROUP_ID,List.of(body)),"probe.dart","""
            // User member is preserved.
            int _userValue() => 73;
            static Animation<double> currentAnimation = const AlwaysStoppedAnimation<double>(1.5);
            static Animation<double> animationFactory() => currentAnimation;
            Animation<double> get _animation => currentAnimation;
            Animation<double> _animationFactory() => currentAnimation;
            static Alignment currentAlignment = const Alignment(2,-1);
            static Alignment alignmentFactory() => currentAlignment;
            Alignment get _alignment => currentAlignment;
            Alignment _alignmentFactory() => currentAlignment;
            double get _plain => 1.5;
            Animation<double>? get _nullableAnimation => currentAnimation;
            Animation<double?> get _nullableValue => const AlwaysStoppedAnimation<double?>(null);
            Animation<int> get _wrongValue => const AlwaysStoppedAnimation<int>(1);
            Animation<dynamic> get _genericDynamic => const AlwaysStoppedAnimation<dynamic>('bad');
            dynamic get _dynamic => currentAnimation;
            ValueNotifier<double> get _notAnimation => ValueNotifier<double>(1);
            Alignment? get _nullableAlignment => currentAlignment;
            AlignmentDirectional get _directionalAlignment => AlignmentDirectional.center;
            AlignmentGeometry get _geometryAlignment => currentAlignment;
            dynamic get _dynamicAlignment => currentAlignment;
            """);
        var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(ScaleTransitionWidgetPropertySchema.TYPE).orElseThrow(),NESTED_ID);
        var current=apply(baseline,new WrapWidget(body.id(),prototype,new SlotName("child"),0));
        assertAnalysis(baseline,current,"probe.dart",true);
        assertArrayEquals(baseline.current().dartCandidateBytes(),current.undo().session().current().dartCandidateBytes());
        assertArrayEquals(current.current().dartCandidateBytes(),current.undo().session().redo().session().current().dartCandidateBytes());
        saveCase("identity",current);current=reopen(current,source(current));
        for(var e:Map.of("negative","-1","zero","0","half","0.5","two","2").entrySet()){
            var changed=apply(current,new SetProperty(NESTED_ID,new PropertyName("scale"),number(e.getValue())));
            assertAnalysis(current,changed,"probe.dart",true);saveCase(e.getKey(),changed);
        }
        var aligned=apply(current,new PatchProperties(NESTED_ID,List.of(
            new PatchProperties.SetPatch(new PropertyName("scale"),number("0.5")),
            new PatchProperties.SetPatch(new PropertyName("alignment"),new PropertyValue.AlignmentGeometryValue(
                PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL,new java.math.BigDecimal("2"),new java.math.BigDecimal("-1"))))));
        assertAnalysis(current,aligned,"probe.dart",true);saveCase("aligned",aligned);
        for(String q:List.of("null","none","low","medium","high")){
            var value=q.equals("null")?new PropertyValue.NullValue():new PropertyValue.EnumValue("FilterQuality",q);
            var changed=apply(current,new SetProperty(NESTED_ID,new PropertyName("filterQuality"),value));
            assertAnalysis(current,changed,"probe.dart",true);saveCase("quality_"+q,changed);
        }
        var empty=apply(current,new RemoveWidget(body.id()));assertAnalysis(current,empty,"probe.dart",true);saveCase("empty",empty);
        var moved=apply(current,new MoveWidget(body.id(),new WidgetPlacement(GROUP_ID,CHILDREN,1)));assertAnalysis(current,moved,"probe.dart",true);saveCase("moved",moved);
        assertFalse(current.apply(new ResetProperty(NESTED_ID,new PropertyName("scale"))).changed());
        for(String kind:List.of("scale","alignment"))for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
            var changed=apply(current,new SetProperty(NESTED_ID,new PropertyName(kind),reference(kind,imported,member,factory)));
            assertAnalysis(current,changed,"probe.dart",true);saveCase(referenceCase(kind,imported,member,factory),changed);
        }
        var live=apply(current,new PatchProperties(NESTED_ID,List.of(
            new PatchProperties.SetPatch(new PropertyName("scale"),reference("scale",true,true,false)),
            new PatchProperties.SetPatch(new PropertyName("alignment"),reference("alignment",true,true,false)),
            new PatchProperties.SetPatch(new PropertyName("filterQuality"),new PropertyValue.EnumValue("FilterQuality","high")))));
        assertAnalysis(current,live,"probe.dart",true);saveCase("live",live);
        var liveBaseline=reopen(live,source(live));
        var reset=apply(liveBaseline,new PatchProperties(NESTED_ID,List.of(
            new PatchProperties.ResetPatch(new PropertyName("alignment")),new PatchProperties.ResetPatch(new PropertyName("filterQuality")))));
        assertAnalysis(liveBaseline,reset,"probe.dart",true);saveCase("reset",reset);
        for(String bad:List.of("_plain","_nullableAnimation","_nullableValue","_wrongValue","_genericDynamic","_dynamic","_notAnimation"))
            assertAnalysis(current,apply(current,new SetProperty(NESTED_ID,new PropertyName("scale"),ref(bad,false))),"probe.dart",false);
        for(String bad:List.of("_nullableAlignment","_directionalAlignment","_geometryAlignment","_dynamicAlignment"))
            assertAnalysis(current,apply(current,new SetProperty(NESTED_ID,new PropertyName("alignment"),ref(bad,false))),"probe.dart",false);
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("scale_test.dart"),runtime());
        run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");
    }
    private static PropertyValue.DoubleValue number(String value){return new PropertyValue.DoubleValue(new java.math.BigDecimal(value));}
    private static String referenceCase(String kind,boolean imported,boolean member,boolean factory){
        return kind+"_"+(imported?"imported":"current")+"_"+(member?"member":"root")+"_"+(factory?"factory":"getter");
    }
    private static PropertyValue.DartObjectReferenceValue reference(String kind,boolean imported,boolean member,boolean factory){
        boolean scale=kind.equals("scale");
        String field=scale?"animation":"alignment";
        String root=member?(imported?"ScaleValues":"Storage"):(imported?(factory?field+"Factory":field):(factory?"_"+field+"Factory":"_"+field));
        String accessor=imported?(factory?(scale?"create":"createAlignment"):(scale?"current":"alignment")):(factory?field+"Factory":scale?"currentAnimation":"currentAlignment");
        return new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:scale_transition_contract/references.dart"):Optional.empty(),
            root,member?Optional.of(accessor):Optional.empty(),factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
            factory?Optional.of(false):Optional.empty());
    }
    private static String runtime(){
        var imports=new StringBuilder("""
            import 'package:flutter/material.dart';
            import 'package:flutter/rendering.dart';
            import 'package:flutter_test/flutter_test.dart';
            import 'package:scale_transition_contract/references.dart' as refs;
            """);
        var names=new ArrayList<>(List.of("identity","negative","zero","half","two","aligned","empty","moved",
            "quality_null","quality_none","quality_low","quality_medium","quality_high","live","reset"));
        for(String kind:List.of("scale","alignment"))for(boolean i:List.of(false,true))for(boolean m:List.of(false,true))for(boolean f:List.of(false,true))names.add(referenceCase(kind,i,m,f));
        var cases=new StringBuilder("final cases=<String,Widget>{");
        for(String name:names){
            imports.append("import '../lib/").append(name).append(".dart' as case_").append(name).append(";\n");
            cases.append("'").append(name).append("':const case_").append(name).append(".Sample(),");
        }
        return imports.toString()+"""
            Widget host(Widget child,{bool rtl=false})=>MaterialApp(home:Directionality(textDirection:rtl?TextDirection.rtl:TextDirection.ltr,child:child));
            Finder native(Widget owner)=>find.descendant(of:find.byType(owner.runtimeType,skipOffstage:false),
              matching:find.byType(ScaleTransition,skipOffstage:false),skipOffstage:false);
            Finder transform(Finder n)=>find.descendant(of:n,matching:find.byType(Transform)).first;
            class ProbeAnimation extends Animation<double>{
              ProbeAnimation(this._value,{this.phase=AnimationStatus.forward});
              double _value;
              AnimationStatus phase;
              final listeners=<VoidCallback>{};
              @override double get value=>_value;
              void update(double value,{AnimationStatus? status}){_value=value;phase=status??phase;for(final f in List<VoidCallback>.of(listeners)){f();}}
              @override AnimationStatus get status=>phase;
              @override void addListener(VoidCallback f)=>listeners.add(f);
              @override void removeListener(VoidCallback f)=>listeners.remove(f);
              @override void addStatusListener(AnimationStatusListener listener){}
              @override void removeStatusListener(AnimationStatusListener listener){}
            }
            """+"void main(){"+cases+"};"+"""
            for(final e in cases.entries)for(final rtl in [false,true]){
              testWidgets('generated ${e.key} rtl=$rtl',(tester)async{
                await tester.pumpWidget(host(e.value,rtl:rtl));await tester.pumpAndSettle();
                final finder=native(e.value),n=tester.widget<ScaleTransition>(finder);
                final empty=e.key=='empty'||e.key=='moved';
                final scale=e.key=='negative'?-1.0:e.key=='zero'?0.0:e.key=='half'||e.key=='aligned' ? 0.5:e.key=='two'?2.0:
                  e.key.startsWith('scale_')||e.key=='live'||e.key=='reset'?1.5:1.0;
                final align=e.key=='aligned'||e.key.startsWith('alignment_')||e.key=='live'?const Alignment(2,-1):Alignment.center;
                final quality=e.key=='live'?FilterQuality.high:e.key.startsWith('quality_')&&e.key!='quality_null'?FilterQuality.values.byName(e.key.substring(8)):null;
                expect(n.scale.value,scale);expect(n.alignment,align);expect(n.filterQuality,quality);expect(n.child==null,empty);
                expect(n.scale.status,AnimationStatus.forward);
                final t=tester.widget<Transform>(transform(finder));
                expect(t.filterQuality,quality);expect(t.transformHitTests,true);
                final parent=tester.renderObject<RenderBox>(finder);expect(parent.size,empty?Size.zero:const Size(100,100));
                if(!empty){
                  final text=tester.renderObject<RenderBox>(find.text('Scaled body')),pivot=align.alongSize(parent.size);
                  final delta=text.localToGlobal(Offset.zero,ancestor:parent);
                  expect(delta.dx,closeTo(pivot.dx*(1-scale),1e-8));expect(delta.dy,closeTo(pivot.dy*(1-scale),1e-8));
                  final point=text.localToGlobal(const Offset(10,20),ancestor:parent)-delta;
                  expect(point.dx,closeTo(10*scale,1e-8));expect(point.dy,closeTo(20*scale,1e-8));
                  final result=BoxHitTestResult();
                  parent.hitTest(result,position:text.localToGlobal(const Offset(50,50),ancestor:parent));
                  expect(result.path.any((h)=>identical(h.target,text)),scale!=0);
                }
                expect(tester.takeException(),isNull);
              });
            }
            testWidgets('live source replacement detaches listeners and immediate alignment keeps State',(tester)async{
              final first=ProbeAnimation(.5),second=ProbeAnimation(-1);
              refs.ScaleValues.current=first;refs.ScaleValues.alignment=Alignment.center;
              final widget=case_live.Sample();
              await tester.pumpWidget(host(widget));await tester.pumpAndSettle();
              final finder=native(widget),state=tester.state(native(widget)),child=tester.element(find.text('Scaled body'));
              expect(first.listeners,hasLength(1));first.update(2);await tester.pump();
              expect(tester.widget<Transform>(transform(finder)).transform.storage[0],2);
              refs.ScaleValues.current=second;refs.ScaleValues.alignment=const Alignment(2,-1);
              await tester.pumpWidget(host(case_live.Sample()));
              expect(first.listeners,isEmpty);expect(second.listeners,hasLength(1));
              expect(identical(state,tester.state(finder)),true);expect(identical(child,tester.element(find.text('Scaled body'))),true);
              expect(tester.widget<ScaleTransition>(finder).alignment,const Alignment(2,-1));
              first.update(99);await tester.pump();expect(tester.widget<ScaleTransition>(finder).scale.value,-1);
              second.update(.25);await tester.pump();expect(tester.widget<Transform>(transform(finder)).transform.storage[0],.25);
              await tester.pumpWidget(const SizedBox());expect(second.listeners,isEmpty);expect(tester.takeException(),isNull);
            });
            for(final quality in FilterQuality.values){
              testWidgets('real controller intermediate frames and status filtering $quality',(tester)async{
                final controller=AnimationController(vsync:tester,duration:const Duration(milliseconds:100));
                refs.ScaleValues.current=Tween<double>(begin:-1,end:1).animate(controller);
                final widget=case_scale_imported_member_getter.Sample();
                // Use generated animation source, with the configured native quality under test.
                await tester.pumpWidget(host(widget));await tester.pumpAndSettle();
                final generated=tester.widget<ScaleTransition>(native(widget));
                await tester.pumpWidget(host(Center(child:ScaleTransition(key:const ValueKey('controlled'),scale:generated.scale,
                  filterQuality:quality,alignment:const Alignment(1,-1),child:const SizedBox(key:ValueKey('body'),width:100,height:100)))));
                final finder=find.byKey(const ValueKey('controlled')),state=tester.state(find.byKey(const ValueKey('controlled'))),child=tester.element(find.byKey(const ValueKey('body')));
                expect(tester.widget<Transform>(transform(finder)).filterQuality,isNull);
                controller.forward();await tester.pump();
                for(final fraction in [.25,.5,.75,1.0]){
                  await tester.pump(const Duration(milliseconds:25));
                  final t=tester.widget<Transform>(transform(finder));
                  expect(t.transform.storage[0],closeTo(-1+2*fraction,1e-8));expect(t.transform.storage[5],closeTo(-1+2*fraction,1e-8));
                  expect(t.filterQuality,controller.isAnimating?quality:null);
                }
                await tester.pumpAndSettle();expect(controller.status,AnimationStatus.completed);
                expect(tester.widget<Transform>(transform(finder)).filterQuality,isNull);
                controller.reverse();await tester.pump();await tester.pump(const Duration(milliseconds:25));
                expect(tester.widget<Transform>(transform(finder)).filterQuality,quality);
                await tester.pumpAndSettle();expect(tester.widget<Transform>(transform(finder)).filterQuality,isNull);
                expect(identical(state,tester.state(finder)),true);expect(identical(child,tester.element(find.byKey(const ValueKey('body')))),true);
                await tester.pumpWidget(const SizedBox());controller.dispose();expect(tester.takeException(),isNull);
              });
              testWidgets('all statuses and signed/zero filter matrices $quality',(tester)async{
                final animation=ProbeAnimation(1);
                for(final phase in AnimationStatus.values)for(final value in [-1.0,0.0,.5,2.0]){
                  animation.update(value,status:phase);
                  await tester.pumpWidget(host(Center(child:ScaleTransition(key:const ValueKey('matrix'),scale:animation,
                    filterQuality:quality,child:const SizedBox(width:100,height:100)))));
                  final t=tester.widget<Transform>(transform(find.byKey(const ValueKey('matrix'))));
                  expect(t.filterQuality,phase.isAnimating?quality:null);expect(t.transform.storage[0],value);
                  expect(tester.takeException(),isNull);
                }
                await tester.pumpWidget(const SizedBox());expect(animation.listeners,isEmpty);
              });
            }
            for(final value in [-1.0,0.0,.5,1.0,2.0]){
              testWidgets('native transformed pointer hits $value',(tester)async{
                var taps=0;
                await tester.pumpWidget(host(Center(child:ScaleTransition(scale:AlwaysStoppedAnimation<double>(value),
                  child:GestureDetector(behavior:HitTestBehavior.opaque,onTap:()=>taps++,
                    child:const SizedBox(key:ValueKey('tap'),width:100,height:100))))));
                await tester.pumpAndSettle();await tester.tapAt(tester.getCenter(find.byKey(const ValueKey('tap'))));
                expect(taps,value==0?0:1);expect(tester.takeException(),isNull);
              });
            }
            testWidgets('ancestor bounds constrain enlarged painting',(tester)async{
              var taps=0;
              await tester.pumpWidget(host(Center(child:SizedBox(width:100,height:100,
                child:ScaleTransition(scale:const AlwaysStoppedAnimation<double>(3),
                  child:GestureDetector(behavior:HitTestBehavior.opaque,onTap:()=>taps++,
                    child:const SizedBox(key:ValueKey('outside'),width:100,height:100)))))));
              await tester.tapAt(tester.getCenter(find.byKey(const ValueKey('outside')))+const Offset(100,0));
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
                name: scale_transition_contract
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
            assertTrue(output.contains("+77: All tests passed!"), output);
            System.out.println("ScaleTransition pinned SDK: 77 generated/native runtime cases passed.");
        }
    }
}
