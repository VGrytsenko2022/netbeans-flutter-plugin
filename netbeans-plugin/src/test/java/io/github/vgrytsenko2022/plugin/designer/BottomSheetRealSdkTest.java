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
import org.junit.jupiter.api.io.CleanupMode;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="flutter.events.sdk",matches=".+")
class BottomSheetRealSdkTest {
    @TempDir(cleanup=CleanupMode.ON_SUCCESS) Path project;
    private Path sdk,flutter,dart,lib;
    private final List<String> cases=new ArrayList<>();
    @Test void allArgumentsSourcesShapesDragEventsAndControllerLifecycle() throws Exception {
        initialize();
        String members="""
            // User member is preserved.
            int _userValue() => 73;
            Key? get keyValue => const ValueKey('sheet');
            Color? get colorValue => const Color(0xffabcdef);
            double? get elevationValue => 3.5;
            ShapeBorder? get shapeValue => const StadiumBorder();
            BoxConstraints? get constraintsValue => const BoxConstraints(minWidth:100,maxWidth:400);
            Size? get sizeValue => const Size(40,6);
            late final AnimationController controller = BottomSheet.createAnimationController(_SheetTicker())..value=1;
            final calls=<String>[];
            void closing() { calls.add('closing'); }
            void start(DragStartDetails details) { calls.add('start'); }
            void end(DragEndDetails details,{required bool isClosing}) { calls.add('end:$isClosing'); }
            Widget contents(BuildContext context) => const SizedBox(width:200,height:150,child:Text('Custom'));
            WidgetBuilder makeBuilder() => contents;
            VoidCallback makeCallback() => closing;
            Widget? nullableBuilder(BuildContext context) => null;
            void badEnd(DragEndDetails details,bool isClosing) {}
            Object get wrong => Object();
            dynamic get unsafe => null;
            @override void dispose() { controller.dispose(); super.dispose(); }
            """;
        var catalog=BuiltInWidgetCatalog.getDefault();
        var sheet=WidgetNodePrototypeFactory.create(catalog.find(BottomSheetWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());
        var seed=open(sheet,"probe.dart",members);
        seed=reopen(seed,"import 'package:flutter/scheduler.dart';\n" + source(seed)
                + "\nclass _SheetTicker implements TickerProvider { @override Ticker createTicker(TickerCallback callback) => Ticker(callback); }\n");
        var before=reopen(seed,source(seed));
        var text=new WidgetNode(StableId.random(),new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"),new PropertyValue.StringValue("Content")),Map.of());
        var local=apply(before,new AddWidget(new WidgetPlacement(sheet.id(),new SlotName("child"),0),text));
        assertAnalysis(before,local,"probe.dart",true);save("child",local);save("empty",seed);
        var fields=new LinkedHashMap<String,String>();
        fields.put("key","keyValue");for(String p:List.of("backgroundColor","shadowColor","dragHandleColor"))fields.put(p,"colorValue");
        fields.put("elevation","elevationValue");fields.put("shape","shapeValue");fields.put("constraints","constraintsValue");
        fields.put("dragHandleSize","sizeValue");fields.put("animationController","controller");
        fields.put("onClosing","closing");fields.put("onDragStart","start");fields.put("onDragEnd","end");fields.put("builder","contents");
        var current=before;
        for(var e:fields.entrySet())current=apply(current,new SetProperty(sheet.id(),new PropertyName(e.getKey()),ref(e.getValue())));
        current=apply(current,new SetProperty(sheet.id(),new PropertyName("enableDrag"),new PropertyValue.BooleanValue(true)));
        current=apply(current,new SetProperty(sheet.id(),new PropertyName("showDragHandle"),new PropertyValue.BooleanValue(true)));
        current=apply(current,new SetProperty(sheet.id(),new PropertyName("clipBehavior"),new PropertyValue.EnumValue("Clip","antiAlias")));
        assertAnalysis(before,current,"probe.dart",true);save("sources",current);
        int rejected=0;
        for(String p:fields.keySet())for(String bad:List.of("wrong","unsafe")){
            before=reopen(seed,source(seed));current=apply(before,new SetProperty(sheet.id(),new PropertyName(p),ref(bad)));
            assertAnalysis(before,current,"probe.dart",false);rejected++;
        }
        for(var entry:Map.of("builder","nullableBuilder","onDragEnd","badEnd").entrySet()){
            before=reopen(seed,source(seed));current=apply(before,new SetProperty(sheet.id(),new PropertyName(entry.getKey()),ref(entry.getValue())));
            assertAnalysis(before,current,"probe.dart",false);rejected++;
        }
        for(String p:List.of("builder","onClosing")) {
            before=reopen(seed,source(seed));current=apply(before,new SetProperty(sheet.id(),new PropertyName(p),
                new PropertyValue.DartObjectReferenceValue(Optional.empty(),p.equals("builder")?"makeBuilder":"makeCallback",
                    Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION,Optional.of(false))));
            assertAnalysis(before,current,"probe.dart",true);save("factory_"+p,current);
        }
        before=reopen(local,source(local));current=before;
        for(String p:List.of("key","backgroundColor","shadowColor","dragHandleColor","dragHandleSize","elevation","shape",
                "constraints","animationController","onDragStart","onDragEnd","clipBehavior"))
            current=apply(current,new SetProperty(sheet.id(),new PropertyName(p),new PropertyValue.NullValue()));
        assertAnalysis(before,current,"probe.dart",true);save("nullable",current);
        for(String shape:CardWidgetPropertySchema.shapeKinds()){
            before=reopen(local,source(local));current=apply(before,new SetProperty(sheet.id(),new PropertyName("shapeKind"),new PropertyValue.StringValue(shape)));
            for(String p:CardWidgetPropertySchema.builtInShapePropertyNames())if(CardWidgetPropertySchema.isShapeDetailProperty(p)&&CardWidgetPropertySchema.shapePropertyAppliesToKind(p,shape))
                current=apply(current,new SetProperty(sheet.id(),new PropertyName(p),io.github.vgrytsenko2022.plugin.designer.properties.CardPropertyContractTest.value(p)));
            assertAnalysis(before,current,"probe.dart",true);save("shape_"+shape,current);
        }
        before=reopen(local,source(local));current=before;
        for(String event:List.of("onClosing","onDragStart","onDragEnd"))
            current=apply(current,new CreateEventHandler(sheet.id(),new PropertyName(event),"_handle"+event));
        assertAnalysis(before,current,"probe.dart",true);save("generated_events",current);
        Files.writeString(project.resolve("native_test.dart"),nativeTests());
        run(List.of(flutter.toString(),"test","native_test.dart","--reporter","expanded"),"native.log");
        System.out.println("BottomSheet: "+(cases.size()*12+1)+" generated native cases; "+rejected+" unsafe sources rejected; real drag callback order and controller disposal verified.");
    }
    private static PropertyValue.DartObjectReferenceValue ref(String name) {return new PropertyValue.DartObjectReferenceValue(Optional.empty(),name,Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());}
    private void save(String name,DesignerCommandSession s)throws Exception{saveCase(name,s);cases.add(name);}
    private String nativeTests(){
        var out=new StringBuilder("import 'package:flutter/material.dart';\nimport 'package:flutter_test/flutter_test.dart';\n");
        for(String name:cases)out.append("import 'package:dialog_contract/").append(name).append(".dart' as ").append(name).append(";\n");
        out.append("void main(){\n");
        for(String name:cases)for(String platform:List.of("android","fuchsia","iOS","linux","macOS","windows"))for(boolean rtl:List.of(false,true))
            out.append("""
                testWidgets('NAME PLATFORM RTL',(tester)async {
                  await tester.pumpWidget(MaterialApp(theme:ThemeData(platform:TargetPlatform.PLATFORM),
                    home:Directionality(textDirection:DIR,child:Scaffold(body:NAME.Sample()))));
                  await tester.pumpAndSettle();expect(find.byType(BottomSheet),findsOneWidget);
                  expect(tester.takeException(),isNull);
                  await tester.pumpWidget(const SizedBox());await tester.pump();expect(tester.takeException(),isNull);
                });
                """.replace("NAME",name).replace("PLATFORM",platform).replace("RTL",Boolean.toString(rtl)).replace("DIR",rtl?"TextDirection.rtl":"TextDirection.ltr"));
        out.append("""
            testWidgets('real pointer drag invokes named end then closing and caller disposes',(tester)async {
              await tester.pumpWidget(const MaterialApp(home:Scaffold(body:sources.Sample())));
              await tester.pumpAndSettle();
              final state=tester.state<sources.Storage>(find.byType(sources.Sample));
              final sheet=tester.widget<BottomSheet>(find.byType(BottomSheet));
              expect(sheet.animationController,same(state.controller));
              await tester.fling(find.text('Custom'),const Offset(0,160),1000);await tester.pumpAndSettle();
              expect(state.calls.first,'start');expect(state.calls,contains('end:true'));
              expect(state.calls.indexOf('closing'),greaterThan(state.calls.indexOf('end:true')));
              expect(tester.takeException(),isNull);
              await tester.pumpWidget(const SizedBox());await tester.pump();expect(tester.takeException(),isNull);
            });
            """);
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
    private static WidgetPlacement place(StableId id, int index) { return new WidgetPlacement(id, new SlotName("children"), index); }
    private static WidgetNode group(StableId id, List<WidgetNode> children) {
        return new WidgetNode(id, new WidgetTypeId("flutter.widgets.Column"), Map.of(), Map.of(new SlotName("children"), new WidgetSlot.ListSlot(children)));
    }

    private void initialize() throws Exception {
        sdk = Path.of(System.getProperty("flutter.events.sdk")).toRealPath();
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        flutter = sdk.resolve(windows ? "bin/flutter.bat" : "bin/flutter");
        dart = sdk.resolve(windows ? "bin/cache/dart-sdk/bin/dart.exe" : "bin/cache/dart-sdk/bin/dart");
        Files.writeString(project.resolve("pubspec.yaml"), """
                name: dialog_contract
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
        if(candidate.current().preparedPair().isEmpty()) {
            assertTrue(pass,"Only unchanged Dart can omit a candidate analysis pair");
            assertArrayEquals(baseline.current().dartCandidateBytes(),candidate.current().dartCandidateBytes());
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
    }
}
