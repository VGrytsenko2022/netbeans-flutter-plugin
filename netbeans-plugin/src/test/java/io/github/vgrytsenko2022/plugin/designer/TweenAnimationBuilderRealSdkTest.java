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

@EnabledIfSystemProperty(named="flutter.events.sdk", matches=".+")
class TweenAnimationBuilderRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;
    private static final StableId BUILDER_ID=StableId.random(), GROUP_ID=StableId.random();
    @Test void boxGeneratedFormsProofHistoryAndOwnedTweenLifecycle() throws Exception { verify(false); }
    @Test void sliverGeneratedFormsProofHistoryAndOwnedTweenLifecycle() throws Exception { verify(true); }
    private void verify(boolean sliver) throws Exception {
        initialize();
        String fallback=sliver?"const SliverToBoxAdapter()":"const SizedBox.shrink()";
        Files.writeString(lib.resolve("builders.dart"), """
                import 'package:flutter/widgets.dart';
                class Sources {
                  static Tween<double> get source => Tween<double>(begin: 0, end: 10);
                  static Tween<double> create() => source;
                }
                Widget externalBuilder(BuildContext context,double value,Widget? child) => child ?? %s;
                class Builders {
                  static ValueWidgetBuilder<double> get responsive => externalBuilder;
                  static ValueWidgetBuilder<double> create() => responsive;
                }
                """.formatted(fallback));
        String members="""
                // User member is preserved.
                int _userValue() => 73;
                Tween<double> get _source => Tween<double>(begin:0,end:10);
                Tween<double> _sourceFactory() => _source;
                ValueWidgetBuilder<double> get _responsive => (context,value,child) => child ?? %s;
                ValueWidgetBuilder<double> _factory() => _responsive;
                Widget _direct(BuildContext context,double value,Object? child) => %s;
                WidgetBuilder get _wrongSignature => (context)=>const SizedBox.shrink();
                Widget _nonNullableChild(BuildContext context,double value,Widget child) => child;
                ValueWidgetBuilder<double>? get _nullable => null;
                dynamic _dynamicResult(BuildContext context,double value,Widget? child) => %s;
                Widget? _nullableResult(BuildContext context,double value,Widget? child) => null;
                dynamic get _dynamicReference => _responsive;
                Tween<double>? get _nullableSource => null;
                dynamic get _dynamicSource => _source;
                int get _wrongSource => 7;
                Tween<String> get _stringSource => ConstantTween<String>('wrong');
                Tween<double?> get _nullableValueSource => Tween<double?>(begin:0,end:1);
                Tween<dynamic> get _rawValueSource => ConstantTween<dynamic>(0.0);
                Widget _wrongValueArgument(BuildContext context,String value,Widget? child) => const SizedBox.shrink();
                Duration get _duration => const Duration(milliseconds:40);
                Curve get _curve => Curves.easeInOut;
                int completed = 0;
                void _done() { completed++; }
                """.formatted(fallback,fallback,fallback);
        var slot=new SlotName(sliver?"slivers":"children");
        var sibling=new WidgetNode(StableId.random(),new WidgetTypeId(sliver?"flutter.widgets.SliverToBoxAdapter":"flutter.widgets.SizedBox"),Map.of(),Map.of());
        var group=new WidgetNode(GROUP_ID,new WidgetTypeId(sliver?"flutter.widgets.CustomScrollView":"flutter.widgets.Column"),Map.of(),
                Map.of(slot,new WidgetSlot.ListSlot(List.of(sibling))));
        var baseline=open(group,"probe.dart",members);
        var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(sliver?
                TweenAnimationBuilderWidgetPropertySchema.SLIVER_TYPE:TweenAnimationBuilderWidgetPropertySchema.TYPE).orElseThrow(),BUILDER_ID);
        var current=apply(baseline,new AddWidget(new WidgetPlacement(GROUP_ID,slot,1),prototype));
        assertAnalysis(baseline,current,"probe.dart",true);saveCase("empty",current);
        assertArrayEquals(baseline.current().dartCandidateBytes(),current.undo().session().current().dartCandidateBytes());
        assertArrayEquals(current.current().dartCandidateBytes(),current.undo().session().redo().session().current().dartCandidateBytes());
        WidgetNode child=new WidgetNode(StableId.random(),new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"),new PropertyValue.StringValue("prebuilt child")),Map.of());
        if(sliver)child=new WidgetNode(StableId.random(),new WidgetTypeId("flutter.widgets.SliverToBoxAdapter"),Map.of(),Map.of(new SlotName("child"),WidgetSlot.SingleSlot.of(child)));
        var before=reopen(current,source(current));
        current=apply(before,new AddWidget(new WidgetPlacement(BUILDER_ID,new SlotName("child"),0),child));
        assertAnalysis(before,current,"probe.dart",true);saveCase("child",current);
        for(String property:List.of("tween","builder","durationUs")) {
            assertFalse(current.apply(new ResetProperty(BUILDER_ID,new PropertyName(property))).changed());
            assertFalse(current.apply(new SetProperty(BUILDER_ID,new PropertyName(property),new PropertyValue.NullValue())).changed());
        }
        for(String property:List.of("tween","builder")) for(String name:property.equals("tween")?List.of("_source","_sourceFactory"):List.of("_responsive","_factory","_direct")) {
            before=reopen(current,source(current));
            current=apply(before,new SetProperty(BUILDER_ID,new PropertyName(property),ref(name,name.endsWith("Factory")||name.equals("_factory"),null,false)));
            assertAnalysis(before,current,"probe.dart",true);saveCase(name.substring(1),current);
        }
        for(String bad:List.of("_wrongSignature","_nonNullableChild","_nullable","_dynamicResult","_nullableResult","_dynamicReference","_wrongValueArgument")) {
            before=reopen(current,source(current));
            assertAnalysis(before,apply(before,new SetProperty(BUILDER_ID,new PropertyName("builder"),ref(bad,false,null,false))),"probe.dart",false);
        }
        for(String bad:List.of("_nullableSource","_dynamicSource","_wrongSource","_sourceFactory","_stringSource","_nullableValueSource","_rawValueSource")) {
            before=reopen(current,source(current));
            assertAnalysis(before,apply(before,new SetProperty(BUILDER_ID,new PropertyName("tween"),ref(bad,false,null,false))),"probe.dart",false);
        }
        for(boolean factory:List.of(false,true)) {
            before=reopen(current,source(current));
            current=apply(before,new SetProperty(BUILDER_ID,new PropertyName("builder"),ref("Builders",factory,factory?"create":"responsive",true)));
            current=apply(current,new SetProperty(BUILDER_ID,new PropertyName("tween"),ref("Sources",factory,factory?"create":"source",true)));
            assertAnalysis(before,current,"probe.dart",true);saveCase(factory?"external_factory":"external_getter",current);
        }
        before=reopen(current,source(current));
        current=apply(before,new PatchProperties(BUILDER_ID,List.of(
                new PatchProperties.SetPatch(new PropertyName("durationUs"),ref("_duration",false,null,false)),
                new PatchProperties.SetPatch(new PropertyName("curve"),ref("_curve",false,null,false)),
                new PatchProperties.SetPatch(new PropertyName("onEnd"),ref("_done",false,null,false)))));
        assertAnalysis(before,current,"probe.dart",true);saveCase("configured",current);
        before=reopen(current,source(current));
        current=apply(before,new MoveWidget(BUILDER_ID,new WidgetPlacement(GROUP_ID,slot,0)));
        assertAnalysis(before,current,"probe.dart",true);saveCase("moved",current);
        assertArrayEquals(before.current().dartCandidateBytes(),current.undo().session().current().dartCandidateBytes());
        before=reopen(current,source(current));
        current=apply(before,new PatchProperties(BUILDER_ID,List.of(
                new PatchProperties.SetPatch(new PropertyName("builder"),new PropertyValue.StringValue("child")),
                new PatchProperties.SetPatch(new PropertyName("tween"),new PropertyValue.StringValue("default")),
                new PatchProperties.SetPatch(new PropertyName("durationUs"),new PropertyValue.IntegerValue(java.math.BigInteger.ZERO)),
                new PatchProperties.ResetPatch(new PropertyName("curve")),new PatchProperties.ResetPatch(new PropertyName("onEnd")))));
        assertAnalysis(before,current,"probe.dart",true);saveCase("reset",current);
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("tween_test.dart"), nativeTests(sliver));
        run(List.of(flutter.toString(),"test","--no-pub","test/tween_test.dart"),"native.log");
    }
    @Test void selectedGenericTypesAliasesAndNullableValuesUseExactProof() throws Exception {
        initialize();
        Files.writeString(lib.resolve("builders.dart"),"""
                import 'package:flutter/widgets.dart';
                class Payload { const Payload(this.value);final double value; }
                class PayloadTween extends Tween<Payload> {
                  PayloadTween():super(begin:const Payload(0),end:const Payload(1));
                  @override Payload lerp(double t)=>Payload(t);
                }
                enum Mode { first, second }
                typedef Rows=List<String>;
                typedef ColorValue=Color;
                typedef RectValue=Rect;
                typedef DynamicAlias=dynamic;
                typedef NullableAlias=String?;
                Tween<Payload> payloadTween()=>PayloadTween();
                Tween<Mode> modeTween()=>ConstantTween<Mode>(Mode.second);
                Tween<Rows> rowsTween()=>ConstantTween<Rows>(['end']);
                Tween<Color?> colorTween()=>ColorTween(begin:const Color(0xff000000),end:const Color(0xffffffff));
                Tween<Rect?> rectTween()=>RectTween(begin:Rect.zero,end:const Rect.fromLTWH(10,20,30,40));
                Tween<String?> nullableTween()=>ConstantTween<String?>('end');
                """);
        var files=new ArrayList<String>();int count=0;
        for(boolean sliver:List.of(false,true)) for(boolean nullable:List.of(false,true))
            for(String type:List.of("String","int","double","num","bool","Object")) {
                String selected=type+(nullable?"?":"");
                var initial=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(sliver?
                        TweenAnimationBuilderWidgetPropertySchema.SLIVER_TYPE:TweenAnimationBuilderWidgetPropertySchema.TYPE).orElseThrow(),BUILDER_ID);
                var baseline=open(initial,"generic.dart","""
                        // User member is preserved.
                        int _userValue() => 73;
                        ValueWidgetBuilder<%s> get _bound => (context,value,child) => child ?? %s;
                        """.formatted(selected,sliver?"const SliverToBoxAdapter()":"const SizedBox.shrink()"));
                var edited=apply(baseline,new PatchProperties(BUILDER_ID,List.of(
                        new PatchProperties.SetPatch(new PropertyName("valueType"),new PropertyValue.StringValue(type)),
                        new PatchProperties.SetPatch(new PropertyName("nullableValueType"),new PropertyValue.BooleanValue(nullable)),
                        new PatchProperties.SetPatch(new PropertyName("builder"),ref("_bound",false,null,false)))));
                assertAnalysis(baseline,edited,"generic.dart",true);
                String file="generic_"+count++;saveCase(file,edited);files.add(file);
                assertArrayEquals(edited.current().dartCandidateBytes(),edited.undo().session().redo().session().current().dartCandidateBytes());
            }
        for(boolean sliver:List.of(false,true)) for(String type:List.of("Payload","Mode","Rows","ColorValue","RectValue","NullableAlias")) {
            boolean nullable=Set.of("ColorValue","RectValue","NullableAlias").contains(type);
            String factory=switch(type){case "Payload"->"payloadTween";case "Mode"->"modeTween";case "Rows"->"rowsTween";case "ColorValue"->"colorTween";case "RectValue"->"rectTween";default->"nullableTween";};
            var initial=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(sliver?
                    TweenAnimationBuilderWidgetPropertySchema.SLIVER_TYPE:TweenAnimationBuilderWidgetPropertySchema.TYPE).orElseThrow(),BUILDER_ID);
            var baseline=open(initial,"generic.dart","// User member is preserved.\nint _userValue() => 73;");
            var edits=new ArrayList<PatchProperties.Patch>();
            edits.add(new PatchProperties.SetPatch(new PropertyName("valueType"),ref(type,false,null,true)));
            edits.add(new PatchProperties.SetPatch(new PropertyName("nullableValueType"),new PropertyValue.BooleanValue(nullable)));
            edits.add(new PatchProperties.SetPatch(new PropertyName("tween"),ref(factory,true,null,true)));
            var edited=apply(baseline,new PatchProperties(BUILDER_ID,edits));
            assertAnalysis(baseline,edited,"generic.dart",true);
            String file="generic_"+count++;saveCase(file,edited);files.add(file);
            var invalidType=apply(reopen(edited,source(edited)),new SetProperty(BUILDER_ID,new PropertyName("valueType"),ref("DynamicAlias",false,null,true)));
            assertAnalysis(edited,invalidType,"generic.dart",false);
            if(type.equals("NullableAlias")){
                var badFlag=apply(reopen(edited,source(edited)),new SetProperty(BUILDER_ID,new PropertyName("nullableValueType"),new PropertyValue.BooleanValue(false)));
                assertAnalysis(edited,badFlag,"generic.dart",false);
            }
        }
        var dartTests=new StringBuilder("import 'package:flutter/material.dart';\nimport 'package:flutter_test/flutter_test.dart';\n");
        for(int i=0;i<files.size();i++)dartTests.append("import '../lib/").append(files.get(i)).append(".dart' as c").append(i).append(";\n");
        dartTests.append("void main(){\n");
        for(int i=0;i<files.size();i++) {
            boolean sliver=i<24?i>=12:(i-24)>=6;
            dartTests.append("testWidgets('generic ").append(i).append("',(tester) async { await tester.pumpWidget(MaterialApp(home:")
                    .append("const c").append(i).append(".Sample()")
                    .append(")); await tester.pumpAndSettle(); expect(find.byWidgetPredicate((w)=>w is TweenAnimationBuilder<Object?>,skipOffstage:false),findsOneWidget); expect(tester.takeException(),isNull); });\n");
        }
        dartTests.append("}\n");
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("generic_test.dart"),dartTests.toString());
        run(List.of(flutter.toString(),"test","--no-pub","test/generic_test.dart"),"generic-native.log");
    }
    private static String nativeTests(boolean sliver) {
        return """
                import 'package:flutter/material.dart';
                import 'package:flutter_test/flutter_test.dart';
                import '../lib/empty.dart' as empty;
                import '../lib/child.dart' as child_case;
                import '../lib/source.dart' as source_case;
                import '../lib/sourceFactory.dart' as source_factory;
                import '../lib/responsive.dart' as responsive;
                import '../lib/factory.dart' as factory_case;
                import '../lib/direct.dart' as direct;
                import '../lib/external_getter.dart' as external_getter;
                import '../lib/external_factory.dart' as external_factory;
                import '../lib/configured.dart' as configured;
                import '../lib/moved.dart' as moved;
                import '../lib/reset.dart' as reset;
                const sliver=%s;
                Widget host(Widget child)=>MaterialApp(home:sliver?CustomScrollView(slivers:[child]):Center(child:child));
                Widget fallback()=>sliver?const SliverToBoxAdapter():const SizedBox.shrink();
                class CountChild extends StatelessWidget {
                  const CountChild(this.built); final VoidCallback built;
                  @override Widget build(BuildContext context) { built();return const SizedBox(width:30,height:20); }
                }
                void main() {
                  final cases=<String,Widget>{
                    'empty':const empty.Sample(),'child':const child_case.Sample(),'source':const source_case.Sample(),
                    'sourceFactory':const source_factory.Sample(),'responsive':const responsive.Sample(),'factory':const factory_case.Sample(),
                    'direct':const direct.Sample(),'external_getter':const external_getter.Sample(),
                    'external_factory':const external_factory.Sample(),'configured':const configured.Sample(),
                    'moved':const moved.Sample(),'reset':const reset.Sample()};
                  for(final entry in cases.entries) for(final rtl in [false,true]) for(final dark in [false,true]) {
                    testWidgets(entry.key+' rtl=$rtl dark=$dark',(tester) async {
                      await tester.pumpWidget(MaterialApp(theme:ThemeData(brightness:dark?Brightness.dark:Brightness.light),
                        home:Directionality(textDirection:rtl?TextDirection.rtl:TextDirection.ltr,child:entry.value)));
                      final native=find.byType(TweenAnimationBuilder<double>,skipOffstage:false);
                      expect(native,findsOneWidget);
                      final widget=tester.widget<TweenAnimationBuilder<double>>(native);
                      expect(widget.tween.end,isNotNull);
                      final element=tester.element(native);
                      await tester.pumpAndSettle();
                      expect(identical(element,tester.element(native)),isTrue);
                      expect(tester.takeException(),isNull);
                      if(entry.key!='empty'&&entry.key!='direct')expect(find.text('prebuilt child'),findsOneWidget);
                      if(entry.key=='configured'||entry.key=='moved') {
                        expect(widget.duration,const Duration(milliseconds:40));expect(widget.curve,Curves.easeInOut);expect(widget.onEnd,isNotNull);
                      }
                    });
                  }
                  testWidgets('initial animation retargets from current value and retains prebuilt Child',(tester) async {
                    var childBuilds=0,ends=0;double value=-1;
                    final content=CountChild(()=>childBuilds++);
                    final child=sliver?SliverToBoxAdapter(child:content):content;
                    Widget build(BuildContext context,double current,Widget? passed) {value=current;expect(identical(passed,child),isTrue);return passed!;}
                    Widget make(Tween<double> tween)=>TweenAnimationBuilder<double>(key:const ValueKey('owned'),
                      tween:tween,duration:const Duration(seconds:1),builder:build,child:child,onEnd:()=>ends++);
                    final first=Tween<double>(begin:0,end:10);
                    await tester.pumpWidget(host(make(first)));await tester.pump(const Duration(milliseconds:400));
                    expect(value,closeTo(4,0.01));expect(ends,0);expect(childBuilds,1);
                    final element=tester.element(find.byKey(const ValueKey('owned'),skipOffstage:false));
                    await tester.pumpWidget(host(make(Tween<double>(begin:999,end:20))));
                    expect(value,closeTo(4,0.01));
                    await tester.pump(const Duration(milliseconds:500));expect(value,closeTo(12,0.01));
                    await tester.pump(const Duration(milliseconds:500));expect(value,20);await tester.pump(const Duration(milliseconds:16));expect(ends,1);
                    expect(childBuilds,1);expect(identical(element,tester.element(find.byKey(const ValueKey('owned'),skipOffstage:false))),isTrue);
                    await tester.pumpWidget(host(make(Tween<double>(begin:-999,end:20))));await tester.pumpAndSettle();
                    expect(ends,1);expect(value,20);
                    await tester.pumpWidget(const SizedBox.shrink());await tester.pump(const Duration(seconds:2));expect(tester.takeException(),isNull);
                  });
                  testWidgets('null begin is replaced by end and does not start an initial animation',(tester) async {
                    var ends=0;double value=-1;
                    final owned=Tween<double>(end:7);
                    await tester.pumpWidget(host(TweenAnimationBuilder<double>(tween:owned,duration:const Duration(seconds:1),
                      onEnd:()=>ends++,builder:(c,v,child){value=v;return fallback();})));
                    await tester.pumpAndSettle();expect(value,7);expect(owned.begin,7);expect(ends,0);
                  });
                  testWidgets('zero duration completes and null Child is passed unchanged',(tester) async {
                    var ends=0;double value=-1;
                    await tester.pumpWidget(host(TweenAnimationBuilder<double>(tween:Tween<double>(begin:0,end:5),
                      duration:Duration.zero,onEnd:()=>ends++,builder:(c,v,child){expect(child,isNull);value=v;return fallback();})));
                    await tester.pumpAndSettle();expect(value,5);expect(ends,1);
                  });
                  testWidgets('curve and duration changes apply without replacing the native element',(tester) async {
                    double value=-1;
                    Widget make(Curve curve,Duration duration)=>TweenAnimationBuilder<double>(key:const ValueKey('curve'),
                      tween:Tween<double>(begin:0,end:10),duration:duration,curve:curve,builder:(c,v,child){value=v;return fallback();});
                    await tester.pumpWidget(host(make(Curves.easeIn,const Duration(seconds:1))));
                    final element=tester.element(find.byKey(const ValueKey('curve'),skipOffstage:false));
                    await tester.pump(const Duration(milliseconds:500));expect(value,lessThan(5));
                    await tester.pumpWidget(host(make(Curves.linear,const Duration(seconds:2))));
                    expect(identical(element,tester.element(find.byKey(const ValueKey('curve'),skipOffstage:false))),isTrue);
                    await tester.pumpAndSettle();expect(value,10);expect(tester.takeException(),isNull);
                  });
                  testWidgets('end must be non-null even for nullable T',(tester) async {
                    await tester.pumpWidget(host(TweenAnimationBuilder<double?>(tween:Tween<double?>(),
                      duration:const Duration(seconds:1),builder:(c,v,child)=>fallback())));
                    expect(tester.takeException(),isAssertionError);
                    await tester.pumpWidget(const SizedBox.shrink());
                  });
                }
                """.formatted(sliver);
    }
    private static PropertyValue.DartObjectReferenceValue ref(String name,boolean factory,String member,boolean imported) {
        return new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:tween_animation_builder_contract/builders.dart"):Optional.empty(),
                name,Optional.ofNullable(member),factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                factory?Optional.of(false):Optional.empty());
    }
    private void saveCase(String name,DesignerCommandSession session) throws Exception {
        var reopened=reopen(session,source(session));
        assertEquals(session.current().fdSnapshot(),reopened.current().fdSnapshot());
        assertArrayEquals(session.current().dartCandidateBytes(),reopened.current().dartCandidateBytes());
        assertTrue(source(reopened).contains("// User member is preserved."));
        assertTrue(source(reopened).contains("int _userValue() => 73;"));
        Files.write(lib.resolve(name+".dart"),reopened.current().dartCandidateBytes());
    }
    private void initialize() throws Exception {
        sdk = Path.of(System.getProperty("flutter.events.sdk")).toRealPath();
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        flutter = sdk.resolve(windows ? "bin/flutter.bat" : "bin/flutter");
        dart = sdk.resolve(windows ? "bin/cache/dart-sdk/bin/dart.exe" : "bin/cache/dart-sdk/bin/dart");
        Files.writeString(project.resolve("pubspec.yaml"), """
                name: tween_animation_builder_contract
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
        if (root.type().equals(TweenAnimationBuilderWidgetPropertySchema.SLIVER_TYPE))
            root=new WidgetNode(GROUP_ID,new WidgetTypeId("flutter.widgets.CustomScrollView"),Map.of(),
                    Map.of(new SlotName("slivers"),new WidgetSlot.ListSlot(List.of(root))));
        var session = StateBindingRealSdkTest.openRoot(root, file, members);
        return reopen(session, "import 'package:flutter/foundation.dart';\n" + source(session));
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
