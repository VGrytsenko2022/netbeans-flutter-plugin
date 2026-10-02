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

@EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
class FadeTransitionRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;
    private static final StableId GROUP_ID = StableId.random(), NESTED_ID = StableId.random();
    private static final SlotName CHILDREN = new SlotName("children");

    @Test void generationSaveReopenStrictTypesAndLiveAnimationForBothRenderProtocols() throws Exception {
        initialize();
        Files.writeString(lib.resolve("references.dart"), """
            import 'package:flutter/widgets.dart';
            Animation<double> get animation => FadeValues.current;
            Animation<double> animationFactory() => FadeValues.current;
            class FadeValues {
              static Animation<double> current = const AlwaysStoppedAnimation<double>(0.25);
              static Animation<double> create() => current;
            }
            """);
        for(boolean sliver:List.of(false,true)) {
            var type=sliver?FadeTransitionWidgetPropertySchema.SLIVER_TYPE:FadeTransitionWidgetPropertySchema.TYPE;
            var slot=new SlotName(sliver?"sliver":"child");
            var body=adapter("Fade body",100);
            if(sliver)body=new WidgetNode(StableId.random(),new WidgetTypeId("flutter.widgets.SliverToBoxAdapter"),Map.of(),Map.of(new SlotName("child"),WidgetSlot.SingleSlot.of(body)));
            var owner=new WidgetNode(GROUP_ID,new WidgetTypeId(sliver?"flutter.widgets.CustomScrollView":"flutter.widgets.Column"),Map.of(),
                Map.of(new SlotName(sliver?"slivers":"children"),new WidgetSlot.ListSlot(List.of(body))));
            var baseline=open(owner,"probe.dart","""
                // User member is preserved.
                int _userValue() => 73;
                static Animation<double> currentAnimation = const AlwaysStoppedAnimation<double>(0.25);
                static Animation<double> animationFactory() => currentAnimation;
                Animation<double> get _animation => currentAnimation;
                Animation<double> _animationFactory() => currentAnimation;
                double get _plain => 0.25;
                Animation<double>? get _nullableAnimation => currentAnimation;
                Animation<double?> get _nullableValue => const AlwaysStoppedAnimation<double?>(null);
                Animation<int> get _wrongValue => const AlwaysStoppedAnimation<int>(1);
                Animation<dynamic> get _genericDynamic => const AlwaysStoppedAnimation<dynamic>('bad');
                dynamic get _dynamic => currentAnimation;
                ValueNotifier<double> get _notAnimation => ValueNotifier<double>(0.25);
                """);
            var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(type).orElseThrow(),NESTED_ID);
            var current=apply(baseline,new WrapWidget(body.id(),prototype,slot,0));
            assertAnalysis(baseline,current,"probe.dart",true);
            assertArrayEquals(baseline.current().dartCandidateBytes(),current.undo().session().current().dartCandidateBytes());
            assertArrayEquals(current.current().dartCandidateBytes(),current.undo().session().redo().session().current().dartCandidateBytes());
            current=reopen(current,source(current));
            saveCase(caseName(sliver,"opaque"),current);
            for(String value:List.of("0","0.5")) {
                var changed=apply(current,new SetProperty(NESTED_ID,new PropertyName("opacity"),new PropertyValue.DoubleValue(new java.math.BigDecimal(value))));
                assertAnalysis(current,changed,"probe.dart",true);saveCase(caseName(sliver,value.equals("0")?"zero":"fractional"),changed);
                if(value.equals("0")){
                    changed=reopen(changed,source(changed));
                    var semantics=apply(changed,new SetProperty(NESTED_ID,new PropertyName("alwaysIncludeSemantics"),new PropertyValue.BooleanValue(true)));
                    assertAnalysis(changed,semantics,"probe.dart",true);saveCase(caseName(sliver,"semantics"),semantics);
                }
            }
            var empty=apply(current,new RemoveWidget(body.id()));
            assertAnalysis(current,empty,"probe.dart",true);saveCase(caseName(sliver,"empty"),empty);
            var moved=apply(current,new MoveWidget(body.id(),new WidgetPlacement(GROUP_ID,new SlotName(sliver?"slivers":"children"),1)));
            assertAnalysis(current,moved,"probe.dart",true);
            assertFalse(current.apply(new ResetProperty(NESTED_ID,new PropertyName("opacity"))).changed());
            for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
                var changed=apply(current,new SetProperty(NESTED_ID,new PropertyName("opacity"),animationReference(imported,member,factory)));
                assertAnalysis(current,changed,"probe.dart",true);saveCase(caseName(sliver,referenceCase(imported,member,factory)),changed);
            }
            for(String bad:List.of("_plain","_nullableAnimation","_nullableValue","_wrongValue","_genericDynamic","_dynamic","_notAnimation"))
                assertAnalysis(current,apply(current,new SetProperty(NESTED_ID,new PropertyName("opacity"),ref(bad,false))),"probe.dart",false);
        }
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("fade_test.dart"),runtime());
        run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");
    }
    private static String caseName(boolean sliver,String name) {return (sliver?"sliver_":"box_")+name;}
    private static String referenceCase(boolean imported,boolean member,boolean factory) {
        return "ref_"+(imported?"imported":"current")+"_"+(member?"member":"root")+"_"+(factory?"factory":"getter");
    }
    private static PropertyValue.DartObjectReferenceValue animationReference(boolean imported,boolean member,boolean factory) {
        String root=member?(imported?"FadeValues":"Storage"):(imported?(factory?"animationFactory":"animation"):(factory?"_animationFactory":"_animation"));
        String accessor=imported?(factory?"create":"current"):(factory?"animationFactory":"currentAnimation");
        return new PropertyValue.DartObjectReferenceValue(
            imported?Optional.of("package:fade_transition_contract/references.dart"):Optional.empty(),root,member?Optional.of(accessor):Optional.empty(),
            factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
            factory?Optional.of(false):Optional.empty());
    }
    private static String runtime() {
        var imports=new StringBuilder("""
            import 'package:flutter/material.dart';
            import 'package:flutter/rendering.dart';
            import 'package:flutter_test/flutter_test.dart';
            import 'package:fade_transition_contract/references.dart' as refs;
            """);
        var cases=new StringBuilder("final cases=<String,Widget>{");
        for(boolean sliver:List.of(false,true)){
            var names=new ArrayList<>(List.of("opaque","zero","fractional","semantics","empty"));
            for(boolean i:List.of(false,true))for(boolean m:List.of(false,true))for(boolean f:List.of(false,true))names.add(referenceCase(i,m,f));
            for(String suffix:names){
                String name=caseName(sliver,suffix);
                imports.append("import '../lib/").append(name).append(".dart' as case_").append(name).append(";\n");
                cases.append("'").append(name).append("':const case_").append(name).append(".Sample(),");
            }
        }
        return imports.toString()+"""
            Widget host(Widget child,{bool rtl=false})=>MaterialApp(home:Directionality(
              textDirection:rtl?TextDirection.rtl:TextDirection.ltr,child:child));
            class ProbeAnimation extends Animation<double>{
              ProbeAnimation(this._value);
              double _value;
              final listeners=<VoidCallback>{};
              @override double get value=>_value;
              void update(double value){_value=value;for(final f in List<VoidCallback>.of(listeners)){f();}}
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
                final sliver=e.key.startsWith('sliver_'),empty=e.key.endsWith('_empty');
                final finder=find.descendant(of:find.byType(e.value.runtimeType,skipOffstage:false),matching:find.byType(sliver?SliverFadeTransition:FadeTransition,skipOffstage:false),skipOffstage:false);
                final n=tester.widget(finder);
                final animation=n is FadeTransition?n.opacity:(n as SliverFadeTransition).opacity;
                final semantics=n is FadeTransition?n.alwaysIncludeSemantics:(n as SliverFadeTransition).alwaysIncludeSemantics;
                final expected=e.key.contains('_ref_') ? 0.25 : e.key.endsWith('_fractional') ? 0.5 :
                  e.key.endsWith('_zero')||e.key.endsWith('_semantics')?0.0:1.0;
                expect(animation.value,expected);expect(semantics,e.key.endsWith('_semantics'));
                final render=tester.renderObject(finder);
                if(render is RenderAnimatedOpacity)expect(render.size.height,empty?0:100);
                if(render is RenderSliverAnimatedOpacity)expect(render.geometry!.scrollExtent,empty?0:100);
                final children=<RenderObject>[];render.visitChildrenForSemantics(children.add);
                expect(children.isNotEmpty,(!empty||sliver)&&(expected>0||semantics));
                expect(tester.takeException(),isNull);
              });
            }
            for(final sliver in [false,true]){
              Widget sample()=>sliver?case_sliver_ref_imported_member_getter.Sample():case_box_ref_imported_member_getter.Sample();
              Finder transition()=>find.descendant(of:find.byType(sample().runtimeType,skipOffstage:false),matching:find.byType(sliver?SliverFadeTransition:FadeTransition,skipOffstage:false),skipOffstage:false);
              testWidgets('generated live replacement listeners sliver=$sliver',(tester)async{
                final first=ProbeAnimation(.2),second=ProbeAnimation(.7);refs.FadeValues.current=first;
                await tester.pumpWidget(host(sample()));await tester.pumpAndSettle();
                final finder=transition(),render=tester.renderObject(finder);
                final child=tester.element(find.text('Fade body'));expect(first.listeners,hasLength(1));
                first.update(.4);await tester.pump();
                expect((render as dynamic).opacity.value,.4);
                refs.FadeValues.current=second;await tester.pumpWidget(host(sample()));
                expect(first.listeners,isEmpty);expect(second.listeners,hasLength(1));expect(identical(render,tester.renderObject(finder)),true);
                first.update(.99);await tester.pump();expect((render as dynamic).opacity.value,.7);
                second.update(0);await tester.pump();expect((render as dynamic).opacity.value,0);
                expect(identical(child,tester.element(find.text('Fade body'))),true);
                await tester.pumpWidget(const SizedBox());expect(second.listeners,isEmpty);expect(tester.takeException(),isNull);
              });
              testWidgets('generated AnimationController frames sliver=$sliver',(tester)async{
                final controller=AnimationController(vsync:tester,duration:const Duration(milliseconds:100));
                refs.FadeValues.current=controller;
                await tester.pumpWidget(host(sample()));
                final finder=transition(),render=tester.renderObject(finder);
                controller.forward();await tester.pump();
                for(final fraction in [.25,.5,.75,1.0]){
                  await tester.pump(const Duration(milliseconds:25));expect((render as dynamic).opacity.value,closeTo(fraction,1e-8));
                }
                expect(identical(render,tester.renderObject(finder)),true);
                await tester.pumpWidget(const SizedBox());controller.dispose();expect(tester.takeException(),isNull);
              });
              testWidgets('zero opacity preserves pointer hits sliver=$sliver',(tester)async{
                var taps=0;
                final box=GestureDetector(behavior:HitTestBehavior.opaque,onTap:()=>taps++,child:const SizedBox(width:100,height:100));
                final child=sliver?CustomScrollView(slivers:[SliverFadeTransition(opacity:const AlwaysStoppedAnimation<double>(0),
                    sliver:SliverToBoxAdapter(child:box))]):Center(child:FadeTransition(opacity:const AlwaysStoppedAnimation<double>(0),child:box));
                await tester.pumpWidget(host(child));await tester.tap(find.byWidget(box));expect(taps,1);expect(tester.takeException(),isNull);
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
                name: fade_transition_contract
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
    }
}
