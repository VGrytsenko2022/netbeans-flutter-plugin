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
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
@EnabledIfSystemProperty(named="flutter.events.sdk",matches=".+")
class SizeTransitionRealSdkTest {
    @TempDir Path project;
    private Path sdk,flutter,dart,lib;
    private static final StableId GROUP_ID=StableId.random(),NESTED_ID=StableId.random();
    private static final SlotName CHILDREN=new SlotName("children");
    private final Map<String,String> cases=new LinkedHashMap<>();
    private int runtimeCaseCount;
    @Test void fullConstructorReferencesSaveHistoryAndLiveSizeLifecycle() throws Exception {
        initialize();
        Files.writeString(lib.resolve("references.dart"), """
            import 'package:flutter/widgets.dart';
            Animation<double> get animation => SizeValues.animation;
            Animation<double> animationFactory() => SizeValues.animation;
            AlignmentGeometry? get alignment => SizeValues.alignment;
            AlignmentGeometry? alignmentFactory() => SizeValues.alignment;
            double? get legacy => SizeValues.legacy;
            double? legacyFactory() => SizeValues.legacy;
            double? get cross => SizeValues.cross;
            double? crossFactory() => SizeValues.cross;
            class SizeValues {
              static Animation<double> animation = const AlwaysStoppedAnimation<double>(.5);
              static Animation<double> animationFactory() => animation;
              static AlignmentGeometry? alignment = const AlignmentDirectional(1,-1);
              static AlignmentGeometry? alignmentFactory() => alignment;
              static double? legacy = -1;
              static double? legacyFactory() => legacy;
              static double? cross = .5;
              static double? crossFactory() => cross;
            }
            """);
        var body=adapter("Sized body",100);
        var baseline=open(group(GROUP_ID,List.of(body)),"probe.dart","""
            // User member is preserved.
            int _userValue() => 73;
            static Animation<double> currentAnimation = const AlwaysStoppedAnimation<double>(.5);
            static Animation<double> animationFactory() => currentAnimation;
            Animation<double> get _animation => currentAnimation;
            Animation<double> _animationFactory() => currentAnimation;
            static AlignmentGeometry? currentAlignment = const AlignmentDirectional(1,-1);
            static AlignmentGeometry? alignmentFactory() => currentAlignment;
            AlignmentGeometry? get _alignment => currentAlignment;
            AlignmentGeometry? _alignmentFactory() => currentAlignment;
            static double? currentLegacy = -1;
            static double? legacyFactory() => currentLegacy;
            double? get _legacy => currentLegacy;
            double? _legacyFactory() => currentLegacy;
            static double? currentCross = .5;
            static double? crossFactory() => currentCross;
            double? get _cross => currentCross;
            double? _crossFactory() => currentCross;
            double? get _nullNumber => null;
            AlignmentGeometry? get _nullAlignment => null;
            double get _plain => .5;
            Animation<double>? get _nullableAnimation => currentAnimation;
            Animation<double?> get _nullableValue => const AlwaysStoppedAnimation<double?>(null);
            Animation<int> get _wrongValue => const AlwaysStoppedAnimation<int>(1);
            Animation<dynamic> get _genericDynamic => const AlwaysStoppedAnimation<dynamic>('bad');
            dynamic get _dynamic => currentAnimation;
            ValueNotifier<double> get _notAnimation => ValueNotifier<double>(1);
            Object? get _object => null;
            num? get _num => .5;
            int? get _int => 1;
            dynamic get _dynamicNumber => .5;
            dynamic get _dynamicAlignment => Alignment.center;
            """);
        var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(SizeTransitionWidgetPropertySchema.TYPE).orElseThrow(),NESTED_ID);
        var current=apply(baseline,new WrapWidget(body.id(),prototype,new SlotName("child"),0));
        assertAnalysis(baseline,current,"probe.dart",true);
        assertArrayEquals(baseline.current().dartCandidateBytes(),current.undo().session().current().dartCandidateBytes());
        assertArrayEquals(current.current().dartCandidateBytes(),current.undo().session().redo().session().current().dartCandidateBytes());
        recordCase("identity",current,"1,Axis.vertical,null,null,null,false");
        current=reopen(current,source(current));
        for(var e:Map.of("negative","-1","zero","0","half","0.5","two","2").entrySet()){
            var changed=apply(current,new SetProperty(NESTED_ID,new PropertyName("sizeFactor"),number(e.getValue())));
            assertAnalysis(current,changed,"probe.dart",true);recordCase(e.getKey(),changed,e.getValue()+",Axis.vertical,null,null,null,false");
        }
        var horizontal=apply(current,new SetProperty(NESTED_ID,new PropertyName("axis"),new PropertyValue.EnumValue("Axis","horizontal")));
        assertAnalysis(current,horizontal,"probe.dart",true);recordCase("horizontal",horizontal,"1,Axis.horizontal,null,null,null,false");
        horizontal=reopen(horizontal,source(horizontal));
        var legacy=apply(horizontal,new SetProperty(NESTED_ID,new PropertyName("axisAlignment"),number("-2")));
        assertAnalysis(horizontal,legacy,"probe.dart",true);recordCase("legacy",legacy,"1,Axis.horizontal,null,-2,null,false");
        legacy=reopen(legacy,source(legacy));
        for(boolean directional:List.of(false,true)){
            var value=new PropertyValue.AlignmentGeometryValue(directional?PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL:PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL,new java.math.BigDecimal("2"),new java.math.BigDecimal("-1"));
            var changed=apply(current,new SetProperty(NESTED_ID,new PropertyName("alignment"),value));
            assertAnalysis(current,changed,"probe.dart",true);recordCase(directional?"directional":"physical",changed,"1,Axis.vertical,const "+(directional?"AlignmentDirectional":"Alignment")+"(2,-1),null,null,false");
        }
        // Switch strategies in one transaction; single-field conflicting changes are rejected.
        assertFalse(legacy.apply(new SetProperty(NESTED_ID,new PropertyName("alignment"),new PropertyValue.AlignmentGeometryValue(PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL,java.math.BigDecimal.ZERO,java.math.BigDecimal.ZERO))).changed());
        var modern=apply(legacy,new PatchProperties(NESTED_ID,List.of(new PatchProperties.ResetPatch(new PropertyName("axisAlignment")),
            new PatchProperties.SetPatch(new PropertyName("alignment"),new PropertyValue.AlignmentGeometryValue(PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL,java.math.BigDecimal.ZERO,java.math.BigDecimal.ZERO)))));
        assertAnalysis(legacy,modern,"probe.dart",true);recordCase("switched",modern,"1,Axis.horizontal,Alignment.center,null,null,false");
        for(String cross:List.of("0","0.5","2","null")){
            var changed=apply(current,new SetProperty(NESTED_ID,new PropertyName("fixedCrossAxisSizeFactor"),cross.equals("null")?new PropertyValue.NullValue():number(cross)));
            assertAnalysis(current,changed,"probe.dart",true);recordCase("cross_"+cross.replace(".","_"),changed,"1,Axis.vertical,null,null,"+cross+",false");
        }
        var explicitNull=apply(current,new PatchProperties(NESTED_ID,List.of(new PatchProperties.SetPatch(new PropertyName("alignment"),new PropertyValue.NullValue()),new PatchProperties.SetPatch(new PropertyName("axisAlignment"),new PropertyValue.NullValue()))));
        assertAnalysis(current,explicitNull,"probe.dart",true);recordCase("null_alignments",explicitNull,"1,Axis.vertical,null,null,null,false");
        var empty=apply(current,new RemoveWidget(body.id()));assertAnalysis(current,empty,"probe.dart",true);recordCase("empty",empty,"1,Axis.vertical,null,null,null,true");
        var moved=apply(current,new MoveWidget(body.id(),new WidgetPlacement(GROUP_ID,CHILDREN,1)));assertAnalysis(current,moved,"probe.dart",true);recordCase("moved",moved,"1,Axis.vertical,null,null,null,true");
        assertFalse(current.apply(new ResetProperty(NESTED_ID,new PropertyName("sizeFactor"))).changed());
        for(String kind:List.of("sizeFactor","alignment","axisAlignment","fixedCrossAxisSizeFactor"))
        for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
            var changed=apply(current,new SetProperty(NESTED_ID,new PropertyName(kind),reference(kind,imported,member,factory)));
            assertAnalysis(current,changed,"probe.dart",true);
            recordCase(referenceCase(kind,imported,member,factory),changed,(kind.equals("sizeFactor")?".5":"1")+",Axis.vertical,"+(kind.equals("alignment")?"const AlignmentDirectional(1,-1)":"null")+","+(kind.equals("axisAlignment")?"-1":"null")+","+(kind.equals("fixedCrossAxisSizeFactor")?".5":"null")+",false");
        }
        for(String kind:List.of("alignment","axisAlignment","fixedCrossAxisSizeFactor")){
            var changed=apply(current,new SetProperty(NESTED_ID,new PropertyName(kind),ref(kind.equals("alignment")?"_nullAlignment":"_nullNumber",false)));
            assertAnalysis(current,changed,"probe.dart",true);recordCase("nullable_"+kind,changed,"1,Axis.vertical,null,null,null,false");
        }
        var live=apply(current,new PatchProperties(NESTED_ID,List.of(
            new PatchProperties.SetPatch(new PropertyName("sizeFactor"),reference("sizeFactor",true,true,false)),
            new PatchProperties.SetPatch(new PropertyName("alignment"),reference("alignment",true,true,false)),
            new PatchProperties.SetPatch(new PropertyName("fixedCrossAxisSizeFactor"),reference("fixedCrossAxisSizeFactor",true,true,false)))));
        assertAnalysis(current,live,"probe.dart",true);recordCase("live",live,".5,Axis.vertical,const AlignmentDirectional(1,-1),null,.5,false");
        live=reopen(live,source(live));
        var reset=apply(live,new PatchProperties(NESTED_ID,List.of(new PatchProperties.ResetPatch(new PropertyName("alignment")),new PatchProperties.ResetPatch(new PropertyName("fixedCrossAxisSizeFactor")))));
        assertAnalysis(live,reset,"probe.dart",true);recordCase("reset",reset,".5,Axis.vertical,null,null,null,false");
        for(String bad:List.of("_plain","_nullableAnimation","_nullableValue","_wrongValue","_genericDynamic","_dynamic","_notAnimation"))
            assertAnalysis(current,apply(current,new SetProperty(NESTED_ID,new PropertyName("sizeFactor"),ref(bad,false))),"probe.dart",false);
        for(String bad:List.of("_object","_plain","_dynamicAlignment"))
            assertAnalysis(current,apply(current,new SetProperty(NESTED_ID,new PropertyName("alignment"),ref(bad,false))),"probe.dart",false);
        for(String kind:List.of("axisAlignment","fixedCrossAxisSizeFactor"))for(String bad:List.of("_num","_int","_dynamicNumber","_object"))
            assertAnalysis(current,apply(current,new SetProperty(NESTED_ID,new PropertyName(kind),ref(bad,false))),"probe.dart",false);
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("size_test.dart"),runtime());
        run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");
    }
    private void recordCase(String name,DesignerCommandSession session,String expected) throws Exception {saveCase(name,session);cases.put(name,expected);}
    private static PropertyValue.DoubleValue number(String n){return new PropertyValue.DoubleValue(new java.math.BigDecimal(n));}
    private static String referenceCase(String kind,boolean imported,boolean member,boolean factory){return kind+"_"+(imported?"imported":"current")+"_"+(member?"member":"root")+"_"+(factory?"factory":"getter");}
    private static PropertyValue.DartObjectReferenceValue reference(String kind,boolean imported,boolean member,boolean factory){
        String field=switch(kind){case "sizeFactor"->"animation";case "axisAlignment"->"legacy";case "fixedCrossAxisSizeFactor"->"cross";default->"alignment";};
        String root=member?(imported?"SizeValues":"Storage"):(imported?"":"_")+field+(factory?"Factory":"");
        String accessor=factory?field+"Factory":imported?field:"current"+Character.toUpperCase(field.charAt(0))+field.substring(1);
        return new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:size_transition_contract/references.dart"):Optional.empty(),root,
            member?Optional.of(accessor):Optional.empty(),factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
            factory?Optional.of(false):Optional.empty());
    }
    private String runtime(){
        runtimeCaseCount=cases.size()*2+19;
        var imports=new StringBuilder("""
            import 'dart:math' as math;
            import 'package:flutter/material.dart';
            import 'package:flutter/rendering.dart';
            import 'package:flutter_test/flutter_test.dart';
            import 'package:size_transition_contract/references.dart' as refs;
            """);
        var map=new StringBuilder("final cases=<String,(Widget,double,Axis,AlignmentGeometry?,double?,double?,bool)>{");
        cases.forEach((name,expected)->{
            imports.append("import '../lib/").append(name).append(".dart' as case_").append(name).append(";\n");
            map.append("'").append(name).append("':(const case_").append(name).append(".Sample(),").append(expected).append("),");
        });
        return imports.toString()+"""
            Widget host(Widget child,{bool rtl=false})=>MaterialApp(home:Directionality(textDirection:rtl?TextDirection.rtl:TextDirection.ltr,child:child));
            Finder native(Widget owner)=>find.descendant(of:find.byType(owner.runtimeType),matching:find.byType(SizeTransition));
            Finder align(Finder finder)=>find.descendant(of:finder,matching:find.byType(Align)).first;
            class ProbeAnimation extends Animation<double>{
              ProbeAnimation(this._value);double _value;final listeners=<VoidCallback>{};
              @override double get value=>_value;
              void update(double v){_value=v;for(final f in List<VoidCallback>.of(listeners)){f();}}
              @override AnimationStatus get status=>AnimationStatus.forward;
              @override void addListener(VoidCallback f)=>listeners.add(f);
              @override void removeListener(VoidCallback f)=>listeners.remove(f);
              @override void addStatusListener(AnimationStatusListener listener){}
              @override void removeStatusListener(AnimationStatusListener listener){}
            }
            """+"void main(){"+map+"};"+"""
            for(final entry in cases.entries)for(final rtl in [false,true]){
              testWidgets('generated ${entry.key} rtl=$rtl',(tester)async{
                final e=entry.value;await tester.pumpWidget(host(e.$1,rtl:rtl));await tester.pumpAndSettle();
                final finder=native(e.$1),n=tester.widget<SizeTransition>(finder),a=tester.widget<Align>(align(finder));
                expect(n.sizeFactor.value,e.$2);expect(n.axis,e.$3);expect(n.alignment,e.$4);
                // ignore: deprecated_member_use
                expect(n.axisAlignment,e.$5);expect(n.fixedCrossAxisSizeFactor,e.$6);expect(n.child==null,e.$7);
                final effective=e.$4??(e.$3==Axis.horizontal?AlignmentDirectional(e.$5??0,-1):AlignmentDirectional(-1,e.$5??0));
                expect(a.alignment,effective);
                final wf=e.$3==Axis.horizontal?math.max(e.$2,0.0):e.$6,hf=e.$3==Axis.vertical?math.max(e.$2,0.0):e.$6;
                expect(a.widthFactor,wf);expect(a.heightFactor,hf);
                final parent=tester.renderObject<RenderBox>(finder),constraints=parent.constraints,extent=e.$7?0.0:100.0;
                final width=wf==null?(constraints.hasBoundedWidth?constraints.maxWidth:extent):extent*wf;
                final height=hf==null?(constraints.hasBoundedHeight?constraints.maxHeight:extent):extent*hf;
                expect(parent.size,constraints.constrain(Size(width,height)));
                if(!e.$7){
                  final child=tester.renderObject<RenderBox>(find.text('Sized body')),resolved=effective.resolve(rtl?TextDirection.rtl:TextDirection.ltr);
                  final offset=child.localToGlobal(Offset.zero,ancestor:parent);
                  expect(child.size,const Size(100,100));
                  expect(offset.dx,closeTo((parent.size.width-100)*(resolved.x+1)/2,1e-8));
                  expect(offset.dy,closeTo((parent.size.height-100)*(resolved.y+1)/2,1e-8));
                }
                expect(tester.widget<ClipRect>(find.descendant(of:finder,matching:find.byType(ClipRect)).first).clipBehavior,Clip.hardEdge);
                expect(tester.takeException(),isNull);
              });
            }
            testWidgets('live source replacement and nullable alignment retain State and listeners',(tester)async{
              final first=ProbeAnimation(.5),second=ProbeAnimation(.25);
              refs.SizeValues.animation=first;refs.SizeValues.alignment=Alignment.center;refs.SizeValues.cross=.5;
              final sample=case_live.Sample();await tester.pumpWidget(host(sample));await tester.pumpAndSettle();
              final finder=native(sample),state=tester.state(native(sample)),child=tester.element(find.text('Sized body'));
              expect(first.listeners,hasLength(1));first.update(-1);await tester.pump();
              expect(tester.getSize(finder).height,0);expect(identical(child,tester.element(find.text('Sized body'))),true);
              first.update(2);await tester.pump();expect(tester.getSize(finder).height,200);
              refs.SizeValues.animation=second;refs.SizeValues.alignment=null;refs.SizeValues.cross=null;
              await tester.pumpWidget(host(case_live.Sample()));
              expect(first.listeners,isEmpty);expect(second.listeners,hasLength(1));
              expect(identical(state,tester.state(finder)),true);expect(identical(child,tester.element(find.text('Sized body'))),true);
              expect(tester.widget<SizeTransition>(finder).alignment,isNull);expect(tester.widget<SizeTransition>(finder).fixedCrossAxisSizeFactor,isNull);
              first.update(99);await tester.pump();expect(tester.getSize(finder).height,25);
              second.update(.75);await tester.pump();expect(tester.getSize(finder).height,75);
              await tester.pumpWidget(const SizedBox());expect(second.listeners,isEmpty);expect(tester.takeException(),isNull);
            });
            for(final axis in Axis.values){
              testWidgets('real source controller layout frames and tight constraints $axis',(tester)async{
                final controller=AnimationController(vsync:tester,duration:const Duration(milliseconds:100));
                refs.SizeValues.animation=Tween<double>(begin:-1,end:2).animate(controller);
                final sample=case_sizeFactor_imported_member_getter.Sample();
                await tester.pumpWidget(host(sample));await tester.pumpAndSettle();final generated=tester.widget<SizeTransition>(native(sample));
                Widget controlled(bool tight)=>host(Center(child:SizedBox(width:200,height:160,child:Align(
                  widthFactor:tight?1:null,child:ConstrainedBox(constraints:tight?const BoxConstraints.tightFor(width:200,height:160):const BoxConstraints(),
                    child:SizeTransition(key:const ValueKey('controlled'),axis:axis,sizeFactor:generated.sizeFactor,
                      fixedCrossAxisSizeFactor:.5,alignment:Alignment.center,child:const SizedBox(key:ValueKey('body'),width:100,height:100)))))));
                await tester.pumpWidget(controlled(false));final finder=find.byKey(const ValueKey('controlled')),state=tester.state(find.byKey(const ValueKey('controlled'))),child=tester.element(find.byKey(const ValueKey('body')));
                controller.forward();await tester.pump();
                for(final fraction in [.2,.4,.6,.8,1.0]){
                  await tester.pump(const Duration(milliseconds:20));
                  expect(tester.widget<SizeTransition>(finder).sizeFactor.value,closeTo(-1+3*fraction,1e-8));
                  final factor=math.max(-1+3*fraction,0.0);
                  expect(tester.getSize(finder).width,closeTo(axis==Axis.horizontal?math.min(100*factor,200):50,1e-8));
                  expect(tester.getSize(finder).height,closeTo(axis==Axis.vertical?math.min(100*factor,160):50,1e-8));
                }
                await tester.pumpAndSettle();controller.reverse();await tester.pump();await tester.pumpAndSettle();
                expect(identical(state,tester.state(finder)),true);expect(identical(child,tester.element(find.byKey(const ValueKey('body')))),true);
                await tester.pumpWidget(controlled(true));expect(tester.getSize(finder),const Size(200,160));
                expect(identical(state,tester.state(finder)),true);
                await tester.pumpWidget(const SizedBox());controller.dispose();expect(tester.takeException(),isNull);
              });
              for(final unbounded in [false,true]){
                testWidgets('empty child and null cross-axis in bounded and unbounded constraints $axis $unbounded',(tester)async{
                  final value=SizeTransition(key:const ValueKey('empty'),axis:axis,sizeFactor:const AlwaysStoppedAnimation<double>(.5));
                  await tester.pumpWidget(host(Center(child:unbounded?UnconstrainedBox(child:value):SizedBox(width:200,height:160,child:Align(child:value)))));
                  expect(tester.getSize(find.byKey(const ValueKey('empty'))),unbounded?Size.zero:axis==Axis.vertical?const Size(200,0):const Size(0,160));
                  expect(tester.takeException(),isNull);
                });
              }
              for(final rtl in [false,true])for(final factor in [-1.0,0.0,.5]){
                testWidgets('native clipped pointer coordinates $axis $rtl $factor',(tester)async{
                  var taps=0;
                  await tester.pumpWidget(host(Center(child:SizeTransition(key:const ValueKey('clip'),axis:axis,sizeFactor:AlwaysStoppedAnimation<double>(factor),
                    fixedCrossAxisSizeFactor:1,child:GestureDetector(behavior:HitTestBehavior.opaque,onTap:()=>taps++,
                      child:const SizedBox(key:ValueKey('tap'),width:100,height:100)))),rtl:rtl));
                  final parent=tester.renderObject<RenderBox>(find.byKey(const ValueKey('clip'))),body=tester.renderObject<RenderBox>(find.byKey(const ValueKey('tap')));
                  if(factor>0){await tester.tapAt(parent.localToGlobal(Offset(parent.size.width/2,parent.size.height/2)));expect(taps,1);}
                  taps=0;await tester.tapAt(body.localToGlobal(const Offset(2,2)));expect(taps,0);expect(tester.takeException(),isNull);
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
                name: size_transition_contract
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
            assertTrue(output.contains("+"+runtimeCaseCount+": All tests passed!"), output);
            System.out.println("SizeTransition pinned SDK: "+runtimeCaseCount+" generated/native runtime cases passed.");
        }
    }
}
