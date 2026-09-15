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
import org.junit.jupiter.api.io.CleanupMode;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="flutter.events.sdk",matches=".+")
class SimpleDialogRealSdkTest {
    @TempDir(cleanup=CleanupMode.ON_SUCCESS) Path project;
    private Path sdk,flutter,dart,lib;
    private static final SlotName CHILDREN=new SlotName("children");
    private final List<String> cases=new ArrayList<>();



    @Test void allArgumentsShapesTypedSourcesAndRouteResults() throws Exception {
        initialize();
        String members="""
            // User member is preserved.
            int _userValue() => 73;
            Key? get keyValue => const ValueKey('simple');
            Color? get colorValue => const Color(0xffabcdef);
            double? get elevationValue => 3.5;
            String? get labelValue => 'Choice';
            EdgeInsets? get nullableInsets => const EdgeInsets.all(12);
            EdgeInsetsGeometry get geometryValue => const EdgeInsetsDirectional.all(8);
            EdgeInsetsGeometry? get nullableGeometry => null;
            TextStyle? get styleValue => const TextStyle(fontSize:16);
            ShapeBorder? get shapeValue => const StadiumBorder();
            AlignmentGeometry? get alignmentValue => AlignmentDirectional.centerStart;
            BoxConstraints? get constraintsValue => const BoxConstraints(minWidth:100,maxWidth:400);
            Object get wrong => Object();
            dynamic get unsafe => null;
            void choose() { Navigator.pop(context, 'chosen'); }
            VoidCallback? get nullableCallback => null;
            VoidCallback makeCallback() => choose;
            void wrongCallback(int value) {}
            """;
        var catalog=BuiltInWidgetCatalog.getDefault();
        var dialog=WidgetNodePrototypeFactory.create(catalog.find(SimpleDialogWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());
        var option=WidgetNodePrototypeFactory.create(catalog.find(SimpleDialogWidgetPropertySchema.OPTION_TYPE).orElseThrow(),StableId.random());
        var text=new WidgetNode(StableId.random(),new WidgetTypeId("flutter.widgets.Text"),Map.of(new PropertyName("data"),new PropertyValue.StringValue("Choose")),Map.of());
        var seed=open(dialog,"probe.dart",members);
        seed=apply(seed,new AddWidget(new WidgetPlacement(dialog.id(),CHILDREN,0),option));
        seed=apply(seed,new AddWidget(new WidgetPlacement(option.id(),new SlotName("child"),0),text));
        seed=reopen(seed,source(seed));
        save("defaults",seed);
        var sources=new LinkedHashMap<String,String>();
        sources.put("key","keyValue");for(String n:List.of("backgroundColor","shadowColor","surfaceTintColor"))sources.put(n,"colorValue");
        sources.put("elevation","elevationValue");sources.put("semanticLabel","labelValue");
        sources.put("titlePadding","geometryValue");sources.put("contentPadding","geometryValue");
        sources.put("insetPadding","nullableInsets");sources.put("shape","shapeValue");
        sources.put("alignment","alignmentValue");sources.put("constraints","constraintsValue");
        for(String n:AlertDialogWidgetPropertySchema.styleFamilies())sources.put(n,"styleValue");
        var before=reopen(seed,source(seed));var current=before;
        for(var e:sources.entrySet())current=apply(current,new SetProperty(dialog.id(),new PropertyName(e.getKey()),ref(e.getValue())));
        current=apply(current,new SetProperty(option.id(),new PropertyName("padding"),ref("nullableInsets")));
        assertAnalysis(before,current,"probe.dart",true);save("sources",current);
        int rejections=0;
        for(String n:sources.keySet())for(String invalid:List.of("wrong","unsafe")) {
            before=reopen(seed,source(seed));current=apply(before,new SetProperty(dialog.id(),new PropertyName(n),ref(invalid)));
            assertAnalysis(before,current,"probe.dart",false);rejections++;
        }
        for(String n:List.of("titlePadding","contentPadding")) {
            before=reopen(seed,source(seed));current=apply(before,new SetProperty(dialog.id(),new PropertyName(n),ref("nullableGeometry")));
            assertAnalysis(before,current,"probe.dart",false);rejections++;
        }
        for(String n:List.of("key","padding","onPressed"))for(String invalid:List.of("wrong","unsafe","geometryValue")) {
            before=reopen(seed,source(seed));current=apply(before,new SetProperty(option.id(),new PropertyName(n),ref(invalid)));
            assertAnalysis(before,current,"probe.dart",false);rejections++;
        }
        before=reopen(seed,source(seed));current=before;
        for(String n:sources.keySet())if(!Set.of("titlePadding","contentPadding").contains(n))
            current=apply(current,new SetProperty(dialog.id(),new PropertyName(n),new PropertyValue.NullValue()));
        for(String n:List.of("key","padding","onPressed"))current=apply(current,new SetProperty(option.id(),new PropertyName(n),new PropertyValue.NullValue()));
        assertAnalysis(before,current,"probe.dart",true);save("nullable",current);
        for(String shape:CardWidgetPropertySchema.shapeKinds()) {
            before=reopen(seed,source(seed));current=apply(before,new SetProperty(dialog.id(),new PropertyName("shapeKind"),new PropertyValue.StringValue(shape)));
            for(String n:CardWidgetPropertySchema.builtInShapePropertyNames())if(CardWidgetPropertySchema.isShapeDetailProperty(n)&&CardWidgetPropertySchema.shapePropertyAppliesToKind(n,shape))
                current=apply(current,new SetProperty(dialog.id(),new PropertyName(n),dev.flutter.netbeans.plugin.designer.properties.CardPropertyContractTest.value(n)));
            for(String family:AlertDialogWidgetPropertySchema.styleFamilies())
                current=apply(current,new SetProperty(dialog.id(),new PropertyName(family+"FontSize"),new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(18))));
            assertAnalysis(before,current,"probe.dart",true);save("shape_"+shape,current);
        }
        for(String handler:List.of("choose","nullableCallback","makeCallback","wrongCallback")) {
            before=reopen(seed,source(seed));
            var reference=handler.equals("makeCallback")?new PropertyValue.DartObjectReferenceValue(Optional.empty(),handler,Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION,Optional.of(false)):ref(handler);
            current=apply(before,new SetProperty(option.id(),new PropertyName("onPressed"),reference));
            assertAnalysis(before,current,"probe.dart",!handler.equals("wrongCallback"));
            if(!handler.equals("wrongCallback"))save(handler.equals("choose")?"selected_option":"callback_"+handler,current);else rejections++;
        }
        Files.writeString(project.resolve("native_test.dart"),nativeTests());
        run(List.of(flutter.toString(),"test","native_test.dart","--reporter","expanded"),"native.log");
        System.out.println("SimpleDialog: "+(cases.size()*12+1)+" generated native cases; "+rejections+" unsafe sources rejected; real dialog result verified.");
    }
    private static PropertyValue.DartObjectReferenceValue ref(String name){return new PropertyValue.DartObjectReferenceValue(Optional.empty(),name,Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());}
    private void save(String name,DesignerCommandSession session)throws Exception{saveCase(name,session);cases.add(name);}
    private String nativeTests() {
        var out=new StringBuilder("import 'package:flutter/material.dart';\nimport 'package:flutter_test/flutter_test.dart';\n");
        for(String name:cases)out.append("import 'package:dialog_contract/").append(name).append(".dart' as ").append(name).append(";\n");
        out.append("void main(){\n");
        for(String name:cases)for(String platform:List.of("android","fuchsia","iOS","linux","macOS","windows"))for(boolean rtl:List.of(false,true)) {
            out.append("""
                testWidgets('NAME PLATFORM RTL',(tester)async{
                  await tester.pumpWidget(MaterialApp(theme:ThemeData(platform:TargetPlatform.PLATFORM),
                    home:Directionality(textDirection:DIR,child:Scaffold(body:NAME.Sample()))));
                  await tester.pumpAndSettle();
                  expect(find.byType(SimpleDialog), findsOneWidget);
                  expect(find.byType(SimpleDialogOption), findsOneWidget);
                  expect(tester.takeException(),isNull);
                  await tester.pumpWidget(const SizedBox()); await tester.pump();
                });
                """.replace("NAME",name).replace("PLATFORM",platform).replace("RTL",Boolean.toString(rtl))
                .replace("DIR",rtl?"TextDirection.rtl":"TextDirection.ltr"));
        }
        out.append("""
            testWidgets('generated option returns a real dialog route result',(tester)async{
              String? selected;
              await tester.pumpWidget(MaterialApp(home:Builder(builder:(context)=>Scaffold(body:
                TextButton(onPressed:()async{selected=await showDialog<String>(context:context,
                  builder:(_)=>const selected_option.Sample());},child:const Text('Open'))))));
              await tester.tap(find.text('Open')); await tester.pumpAndSettle();
              await tester.tap(find.text('Choose')); await tester.pumpAndSettle();
              expect(selected,'chosen'); expect(find.byType(SimpleDialog),findsNothing);
              expect(tester.takeException(),isNull);
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
    private static WidgetPlacement place(StableId id, int index) { return new WidgetPlacement(id, CHILDREN, index); }
    private static WidgetNode group(StableId id, List<WidgetNode> children) {
        return new WidgetNode(id, new WidgetTypeId("flutter.widgets.Column"), Map.of(), Map.of(CHILDREN, new WidgetSlot.ListSlot(children)));
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
