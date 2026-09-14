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

@EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
class SliverSafeAreaRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;
    private static final StableId GROUP_ID = StableId.random(), NESTED_ID = StableId.random();
    private static final SlotName SLIVERS = new SlotName("slivers");

    @Test void allSidesMinimumRequiredChildAndHistoryPassAnalyzerAndNativeSdk() throws Exception {
        initialize();
        var imports=new StringBuilder();var cases=new StringBuilder();
        var child=adapter("Safe body",40);
        var baseline=open(root(group(GROUP_ID,List.of(child,adapter("Sibling",60)))),
            "probe.dart","// User member is preserved.\nint _userValue() => 73;");
        var type=SliverSafeAreaWidgetPropertySchema.TYPE;
        var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(type).orElseThrow(),NESTED_ID);
        var current=apply(baseline,new WrapWidget(child.id(),prototype,new SlotName("sliver"),0));
        assertAnalysis(baseline,current,"probe.dart",true);
        assertExact(baseline,current.undo().session());
        assertExact(current,current.undo().session().redo().session());
        recordCase("defaults",current,15,false,imports,cases);
        current=reopen(current,source(current));
        var minimum=new PropertyValue.EdgeInsetsValue(new java.math.BigDecimal("-3.5"),new java.math.BigDecimal("19.25"),
            new java.math.BigDecimal("21"),new java.math.BigDecimal("-2"));
        for(int bits=0;bits<16;bits++) {
            var patches=new ArrayList<PatchProperties.Patch>();int index=0;
            for(String name:List.of("left","top","right","bottom"))
                patches.add(new PatchProperties.SetPatch(new PropertyName(name),new PropertyValue.BooleanValue((bits&(1<<index++))!=0)));
            patches.add(new PatchProperties.SetPatch(new PropertyName("minimum"),minimum));
            var changed=apply(current,new PatchProperties(NESTED_ID,patches));
            assertAnalysis(current,changed,"probe.dart",true);
            assertExact(current,changed.undo().session());
            assertExact(changed,changed.undo().session().redo().session());
            recordCase("flags"+bits,changed,bits,true,imports,cases);
            current=reopen(changed,source(changed));
        }
        var resets=new ArrayList<PatchProperties.Patch>();
        for(var p:BuiltInWidgetCatalog.getDefault().find(type).orElseThrow().properties()) resets.add(new PatchProperties.ResetPatch(p.name()));
        var reset=apply(current,new PatchProperties(NESTED_ID,resets));
        assertAnalysis(current,reset,"probe.dart",true);assertExact(current,reset.undo().session());
        recordCase("reset",reset,15,false,imports,cases);
        reset=reopen(reset,source(reset));
        assertFalse(reset.apply(new RemoveWidget(child.id())).changed());
        assertFalse(reset.apply(new MoveWidget(child.id(),place(reset.current().document().root().id(),1))).changed());
        var moved=apply(reset,new MoveWidget(NESTED_ID,place(reset.current().document().root().id(),1)));
        assertAnalysis(reset,moved,"probe.dart",true);assertExact(reset,moved.undo().session());
        recordCase("moved",moved,15,false,imports,cases);
        String dartTest="""
                import 'dart:math' as math;
                import 'package:flutter/material.dart';
                import 'package:flutter/rendering.dart';
                import 'package:flutter_test/flutter_test.dart';
                %s
                void main() {
                  final cases=<String,(Widget,int,bool)>{%s};
                  for(final entry in cases.entries) {
                    for(final horizontal in [false,true]) {
                      for(final reverse in [false,true]) {
                        for(final rtl in [false,true]) {
                          testWidgets('${entry.key} h=$horizontal r=$reverse rtl=$rtl',(tester) async {
                            final bits=entry.value.$2;
                            bool flag(int i)=>bits&(1<<i)!=0;
                            await tester.pumpWidget(MaterialApp(home:entry.value.$1));
                            await tester.pumpAndSettle();
                            final sliver=tester.widget<SliverSafeArea>(find.byType(SliverSafeArea));
                            expect([sliver.left,sliver.top,sliver.right,sliver.bottom],[for(var i=0;i<4;i++)flag(i)]);
                            final min=entry.value.$3 ? const EdgeInsets.fromLTRB(-3.5,19.25,21,-2) : EdgeInsets.zero;
                            expect(sliver.minimum,min);
                            final expected=EdgeInsets.fromLTRB(math.max(flag(0)?7:0,min.left),math.max(flag(1)?11:0,min.top),
                              math.max(flag(2)?13:0,min.right),math.max(flag(3)?17:0,min.bottom));
                            await tester.pumpWidget(MaterialApp(home:MediaQuery(
                              data:const MediaQueryData(padding:EdgeInsets.fromLTRB(7,11,13,17)),
                              child:Directionality(textDirection:rtl?TextDirection.rtl:TextDirection.ltr,
                                child:Center(child:SizedBox(width:300,height:160,
                                  child:CustomScrollView(primary:false,scrollDirection:horizontal?Axis.horizontal:Axis.vertical,
                                    reverse:reverse,slivers:[sliver])))))));
                            await tester.pumpAndSettle();
                            final render=tester.renderObject<RenderSliverPadding>(find.byType(SliverSafeArea));
                            expect(render.padding,expected);
                            expect(render.geometry!.scrollExtent,40+(horizontal?expected.horizontal:expected.vertical));
                            final context=tester.element(find.text('Safe body'));
                            expect(MediaQuery.paddingOf(context),EdgeInsets.fromLTRB(flag(0)?0:7,flag(1)?0:11,flag(2)?0:13,flag(3)?0:17));
                            expect(tester.takeException(),isNull);
                          });
                        }
                      }
                    }
                  }
                }
                """.formatted(imports,cases);
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("safe_area_test.dart"),dartTest);
        run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");
    }
    private static void assertExact(DesignerCommandSession expected,DesignerCommandSession actual) {
        assertEquals(expected.current().fdSnapshot(),actual.current().fdSnapshot());
        assertArrayEquals(expected.current().dartCandidateBytes(),actual.current().dartCandidateBytes());
    }
    private void recordCase(String name,DesignerCommandSession session,int bits,boolean minimum,
            StringBuilder imports,StringBuilder cases) throws Exception {
        saveCase(name,session);
        imports.append("import '../lib/").append(name).append(".dart' as ").append(name).append(";\n");
        cases.append("'").append(name).append("': (const ").append(name).append(".Sample(), ").append(bits).append(", ").append(minimum).append("),\n");
    }
    private void saveCase(String name, DesignerCommandSession session) throws Exception {
        var reopened = reopen(session, source(session));
        assertEquals(session.current().fdSnapshot(), reopened.current().fdSnapshot());
        assertArrayEquals(session.current().dartCandidateBytes(), reopened.current().dartCandidateBytes());
        assertTrue(source(reopened).contains("// User member is preserved."));
        assertTrue(source(reopened).contains("int _userValue() => 73;"));
        Files.write(lib.resolve(name + ".dart"), reopened.current().dartCandidateBytes());
    }
    private static WidgetPlacement place(StableId id, int index) { return new WidgetPlacement(id, SLIVERS, index); }
    private static WidgetNode group(StableId id, List<WidgetNode> children) {
        return new WidgetNode(id, SliverCrossAxisGroupWidgetPropertySchema.TYPE, Map.of(), Map.of(SLIVERS, new WidgetSlot.ListSlot(children)));
    }
    private static WidgetNode adapter(String label, int extent) {
        var text = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue(label)), Map.of());
        var size = new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(extent));
        var box = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.SizedBox"),
                Map.of(new PropertyName("width"), size, new PropertyName("height"), size),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text)));
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.SliverToBoxAdapter"), Map.of(),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(box)));
    }
    private static WidgetNode root(WidgetNode group) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.CustomScrollView"),
                Map.of(new PropertyName("primary"), new PropertyValue.BooleanValue(false)),
                Map.of(SLIVERS, new WidgetSlot.ListSlot(List.of(group))));
    }
    private void initialize() throws Exception {
        sdk = Path.of(System.getProperty("flutter.events.sdk")).toRealPath();
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        flutter = sdk.resolve(windows ? "bin/flutter.bat" : "bin/flutter");
        dart = sdk.resolve(windows ? "bin/cache/dart-sdk/bin/dart.exe" : "bin/cache/dart-sdk/bin/dart");
        Files.writeString(project.resolve("pubspec.yaml"), """
                name: sliver_safe_area_contract
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
            if (!pass) assertTrue(result.symbolEvidence().stream().anyMatch(evidence -> evidence.staticTypeEvidence()
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
