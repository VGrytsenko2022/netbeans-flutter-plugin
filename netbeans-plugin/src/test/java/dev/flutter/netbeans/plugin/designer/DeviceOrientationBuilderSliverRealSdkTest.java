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

@EnabledIfSystemProperty(named="flutter.events.sdk", matches=".+")
class DeviceOrientationBuilderSliverRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;
    private static final StableId BUILDER_ID=StableId.random(), GROUP_ID=StableId.random();
    private static final SlotName CHILDREN=new SlotName("slivers");
    @Test void strictTypedReferencesSourceHistoryAndResponsiveNativeLayout() throws Exception {
        initialize();
        Files.writeString(lib.resolve("builders.dart"), """
                import 'package:flutter/widgets.dart';
                Widget externalBuilder(BuildContext context, Orientation orientation) =>
                    const SliverToBoxAdapter(child:SizedBox(width:80,height:40));
                class DeviceOrientationBuilders {
                  static OrientationWidgetBuilder get responsive => externalBuilder;
                  static OrientationWidgetBuilder create() => responsive;
                }
                """);
        var sibling=new WidgetNode(StableId.random(),new WidgetTypeId("flutter.widgets.SliverToBoxAdapter"),
                Map.of(),Map.of());
        var group=new WidgetNode(GROUP_ID,new WidgetTypeId("flutter.widgets.CustomScrollView"),Map.of(),
                Map.of(CHILDREN,new WidgetSlot.ListSlot(List.of(sibling))));
        var baseline=open(group,"probe.dart","""
                // User member is preserved.
                int _userValue() => 73;
                OrientationWidgetBuilder get _responsive => (context, orientation) {
                  final portrait=orientation==Orientation.portrait;
                  final text=portrait?'portrait':'landscape';
                  final theme=Theme.of(context).brightness==Brightness.dark?'dark':'light';
                  final direction=Directionality.of(context)==TextDirection.rtl?'rtl':'ltr';
                  return SliverToBoxAdapter(child:SizedBox(width:portrait?80:120,height:portrait?40:80,child:Text('$text $theme $direction')));
                };
                OrientationWidgetBuilder _factory() => _responsive;
                Widget _direct(BuildContext context, Object orientation) => const SliverToBoxAdapter(child:SizedBox(width:80,height:40));
                WidgetBuilder get _wrongSignature => (context) => const SizedBox.shrink();
                OrientationWidgetBuilder? get _nullable => null;
                dynamic _dynamicResult(BuildContext context, Object orientation) => const SizedBox.shrink();
                Widget? _nullableResult(BuildContext context, Orientation orientation) => null;
                dynamic get _dynamicReference => _responsive;
                OrientationWidgetBuilder get _wrongRenderType => (context, orientation) => const SizedBox.shrink();
                """);
        var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault()
                .find(DeviceOrientationBuilderWidgetPropertySchema.SLIVER_TYPE).orElseThrow(),BUILDER_ID);
        var current=apply(baseline,new AddWidget(new WidgetPlacement(GROUP_ID,CHILDREN,1),prototype));
        assertAnalysis(baseline,current,"probe.dart",true);saveCase("empty",current);
        assertArrayEquals(baseline.current().dartCandidateBytes(),current.undo().session().current().dartCandidateBytes());
        assertArrayEquals(current.current().dartCandidateBytes(),current.undo().session().redo().session().current().dartCandidateBytes());
        for(DesignerCommand invalid:List.of(new ResetProperty(BUILDER_ID,new PropertyName("builder")),
                new SetProperty(BUILDER_ID,new PropertyName("builder"),new PropertyValue.NullValue()))) {
            var rejected=current.apply(invalid);assertFalse(rejected.changed());
            assertEquals(current.current().fdSnapshot(),rejected.session().current().fdSnapshot());
            assertArrayEquals(current.current().dartCandidateBytes(),rejected.session().current().dartCandidateBytes());
        }
        for(String name:List.of("_responsive","_factory","_direct")) {
            var before=reopen(current,source(current));
            current=apply(before,new SetProperty(BUILDER_ID,new PropertyName("builder"),ref(name,name.equals("_factory"),null,false)));
            assertAnalysis(before,current,"probe.dart",true);saveCase(name.substring(1),current);
            assertArrayEquals(before.current().dartCandidateBytes(),current.undo().session().current().dartCandidateBytes());
            assertArrayEquals(current.current().dartCandidateBytes(),current.undo().session().redo().session().current().dartCandidateBytes());
        }
        for(boolean factory:List.of(false,true)) {
            var before=reopen(current,source(current));
            current=apply(before,new SetProperty(BUILDER_ID,new PropertyName("builder"),
                    ref("DeviceOrientationBuilders",factory,factory?"create":"responsive",true)));
            assertAnalysis(before,current,"probe.dart",true);saveCase(factory?"external_factory":"external_getter",current);
        }
        for(String bad:List.of("_wrongSignature","_nullable","_dynamicResult","_dynamicReference","_nullableResult")) {
            var before=reopen(current,source(current));
            var wrong=apply(before,new SetProperty(BUILDER_ID,new PropertyName("builder"),ref(bad,false,null,false)));
            assertAnalysis(before,wrong,"probe.dart",false);
        }
        var wrongRender=apply(reopen(current,source(current)),new SetProperty(BUILDER_ID,new PropertyName("builder"),ref("_wrongRenderType",false,null,false)));
        assertAnalysis(current,wrongRender,"probe.dart",true);saveCase("wrong_render",wrongRender);
        var moved=apply(reopen(current,source(current)),new MoveWidget(BUILDER_ID,new WidgetPlacement(GROUP_ID,CHILDREN,0)));
        assertAnalysis(current,moved,"probe.dart",true);
        assertArrayEquals(current.current().dartCandidateBytes(),moved.undo().session().current().dartCandidateBytes());saveCase("moved",moved);
        var reset=apply(reopen(moved,source(moved)),new SetProperty(BUILDER_ID,new PropertyName("builder"),new PropertyValue.StringValue("empty")));
        assertAnalysis(moved,reset,"probe.dart",true);saveCase("reset",reset);
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("layout_test.dart"),"""
                import 'package:flutter/material.dart';
                import 'package:flutter/rendering.dart';
                import 'package:flutter_test/flutter_test.dart';
                import '../lib/empty.dart' as empty;
                import '../lib/responsive.dart' as responsive;
                import '../lib/factory.dart' as factory_case;
                import '../lib/direct.dart' as direct;
                import '../lib/external_getter.dart' as external_getter;
                import '../lib/external_factory.dart' as external_factory;
                import '../lib/reset.dart' as reset;
                import '../lib/moved.dart' as moved;
                import '../lib/wrong_render.dart' as wrong_render;
                void main() {
                  final cases=<String,Widget>{
                    'empty':const empty.Sample(), 'reset':const reset.Sample(),
                    'responsive':const responsive.Sample(),'factory':const factory_case.Sample(),
                    'direct':const direct.Sample(),'moved':const moved.Sample(),
                    'external_getter':const external_getter.Sample(),'external_factory':const external_factory.Sample()};
                  for(final entry in cases.entries) {
                    for(final rtl in [false,true]) {
                      for(final dark in [false,true]) {
                        testWidgets('${entry.key} rtl=$rtl dark=$dark',(tester) async {
                          await tester.pumpWidget(MaterialApp(home:entry.value));
                          final native=tester.widget<DeviceOrientationBuilder>(find.byType(DeviceOrientationBuilder,skipOffstage:false));
                          await tester.pumpWidget(const SizedBox.shrink());
                          Element? retained;
                          for(final screen in [const Size(100,200),const Size(150,150),const Size(240,150)]) {
                            final portrait=screen.width<=screen.height;
                            await tester.pumpWidget(MaterialApp(theme:ThemeData(brightness:dark?Brightness.dark:Brightness.light),
                              home:MediaQuery(data:MediaQueryData(size:screen),
                                child:Directionality(textDirection:rtl?TextDirection.rtl:TextDirection.ltr,
                                  child:Center(child:SizedBox(width:240,height:180,
                                    child:CustomScrollView(slivers:[native])))))));
                            final element=tester.element(find.byType(DeviceOrientationBuilder,skipOffstage:false));
                            if(retained!=null){expect(identical(retained,element),isTrue);} retained=element;
                            final isEmpty=entry.key=='empty'||entry.key=='reset';
                            final isResponsive=entry.key=='responsive'||entry.key=='factory';
                            final render=tester.renderObject<RenderSliver>(find.byType(DeviceOrientationBuilder,skipOffstage:false));
                            expect(render.geometry!.scrollExtent,isEmpty?0:isResponsive?(portrait?40:80):40);
                            if(isResponsive){expect(find.text('${portrait?'portrait':'landscape'} ${dark?'dark':'light'} ${rtl?'rtl':'ltr'}'),findsOneWidget);}
                            expect(tester.takeException(),isNull);
                          }
                          if(entry.key=='responsive'||entry.key=='factory') {
                            await tester.pumpWidget(MaterialApp(theme:ThemeData(brightness:dark?Brightness.light:Brightness.dark),
                              home:MediaQuery(data:const MediaQueryData(size:Size(240,150)),
                                child:Directionality(textDirection:rtl?TextDirection.ltr:TextDirection.rtl,
                                  child:Center(child:SizedBox(width:240,height:180,
                                    child:CustomScrollView(slivers:[native])))))));
                            await tester.pumpAndSettle();
                            expect(identical(retained,tester.element(find.byType(DeviceOrientationBuilder,skipOffstage:false))),isTrue);
                            expect(find.text('landscape ${dark?'light':'dark'} ${rtl?'ltr':'rtl'}'),findsOneWidget);
                            expect(tester.takeException(),isNull);
                          }
                        });
                      }
                    }
                  }
                  testWidgets('requires MediaQuery rather than silently falling back to parent dimensions',(tester) async {
                    await tester.pumpWidget(DeviceOrientationBuilder(builder:(_,o)=>const SizedBox.shrink()),wrapWithView:false);
                    expect(tester.takeException(),isNotNull);
                    await tester.pumpWidget(const SizedBox.shrink());
                    while(tester.takeException()!=null) {}
                  });
                  testWidgets('projection result must match its parent rendering protocol',(tester) async {
                    await tester.pumpWidget(const MaterialApp(home:wrong_render.Sample()));
                    expect(tester.takeException(),isNotNull);
                    await tester.pumpWidget(const SizedBox.shrink());
                    while(tester.takeException()!=null) {}
                  });
                  for(final horizontal in [false,true])for(final reverse in [false,true]) {
                    testWidgets('sliver scroll axis=$horizontal reverse=$reverse',(tester) async {
                      await tester.pumpWidget(const MaterialApp(home:responsive.Sample()));
                      final native=tester.widget<DeviceOrientationBuilder>(find.byType(DeviceOrientationBuilder,skipOffstage:false));
                      for(final portrait in [true,false]) {
                        await tester.pumpWidget(MaterialApp(home:MediaQuery(
                          data:MediaQueryData(size:portrait?const Size(100,200):const Size(200,100)),
                          child:Center(child:SizedBox(width:240,height:180,child:CustomScrollView(
                            scrollDirection:horizontal?Axis.horizontal:Axis.vertical,reverse:reverse,slivers:[native]))))));
                        final render=tester.renderObject<RenderSliver>(find.byType(DeviceOrientationBuilder,skipOffstage:false));
                        expect(render.geometry!.scrollExtent,horizontal?(portrait?80:120):(portrait?40:80));
                        expect(tester.takeException(),isNull);
                      }
                    });
                  }
                }
                """);
        run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");
    }
    private static PropertyValue.DartObjectReferenceValue ref(String name,boolean factory,String member,boolean imported) {
        return new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:device_orientation_builder_contract/builders.dart"):Optional.empty(),
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
                name: device_orientation_builder_contract
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
