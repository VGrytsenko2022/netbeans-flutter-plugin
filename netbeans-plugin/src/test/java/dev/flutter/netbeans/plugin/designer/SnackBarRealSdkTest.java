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
class SnackBarRealSdkTest {
    @TempDir(cleanup=CleanupMode.ON_SUCCESS) Path project;
    private Path sdk,flutter,dart,lib;
    private final List<String> cases=new ArrayList<>();
    @Test void allParametersProofsNativePresentationAndDismissal() throws Exception {
        initialize();
        String members="""
            // User member is preserved.
            int _userValue() => 73;
            int presses=0,visible=0;
            Key? get keyValue => const ValueKey('snack');
            Color? get colorValue => const Color(0xff2468ac);
            double? get elevationValue => 3;
            double? get widthValue => 320;
            double? get thresholdValue => 0.25;
            EdgeInsetsGeometry? get insetsValue => const EdgeInsetsDirectional.fromSTEB(8,4,12,4);
            ShapeBorder? get shapeValue => const StadiumBorder();
            Animation<double>? get animationValue => const AlwaysStoppedAnimation<double>(1);
            Duration get durationValue => const Duration(milliseconds:250);
            String get labelValue => 'Undo';
            String labelFactory() => labelValue;
            VoidCallback callbackFactory() => pressed;
            void pressed(){presses++;}
            void shown(){visible++;}
            void wrongCallback(int v){}
            Object get wrong => Object();
            dynamic get unsafe => null;
            """;
        var catalog=BuiltInWidgetCatalog.getDefault();
        var bar=WidgetNodePrototypeFactory.create(catalog.find(SnackBarWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());
        var action=WidgetNodePrototypeFactory.create(catalog.find(SnackBarWidgetPropertySchema.ACTION).orElseThrow(),StableId.random());
        var seed=open(bar,"probe.dart",members);
        var base=apply(seed,new AddWidget(new WidgetPlacement(bar.id(),new SlotName("action"),0),action));
        save("defaults",base);
        var fields=new LinkedHashMap<String,String>();
        fields.put("key","keyValue");fields.put("backgroundColor","colorValue");fields.put("elevation","elevationValue");
        fields.put("padding","insetsValue");fields.put("width","widthValue");fields.put("shape","shapeValue");
        fields.put("actionOverflowThreshold","thresholdValue");fields.put("closeIconColor","colorValue");
        fields.put("durationUs","durationValue");fields.put("animation","animationValue");fields.put("onVisible","shown");
        var before=reopen(base,source(base));var current=apply(before,new SetProperty(bar.id(),new PropertyName("behavior"),new PropertyValue.EnumValue("SnackBarBehavior","floating")));
        for(var e:fields.entrySet()) current=apply(current,new SetProperty(bar.id(),new PropertyName(e.getKey()),ref(e.getValue())));
        for(String p:List.of("key","textColor","disabledTextColor","backgroundColor","disabledBackgroundColor","label","onPressed"))
            current=apply(current,new SetProperty(action.id(),new PropertyName(p),ref(p.equals("key")?"keyValue":p.equals("label")?"labelValue":p.equals("onPressed")?"pressed":"colorValue")));
        current=apply(current,new SetProperty(bar.id(),new PropertyName("persist"),new PropertyValue.BooleanValue(false)));
        current=apply(current,new SetProperty(bar.id(),new PropertyName("showCloseIcon"),new PropertyValue.BooleanValue(true)));
        assertAnalysis(before,current,"probe.dart",true);save("sources",current);
        before=reopen(current,source(current));
        current=apply(before,new PatchProperties(bar.id(),List.of(new PatchProperties.ResetPatch(new PropertyName("width")),
                new PatchProperties.SetPatch(new PropertyName("margin"),ref("insetsValue")))));
        assertAnalysis(before,current,"probe.dart",true);save("margin",current);
        int rejected=0;
        for(boolean button:List.of(false,true)) {
            var id=button?action.id():bar.id();
            var schema=SnackBarWidgetPropertySchema.properties(button);
            for(var p:schema)if(p.constraints().stream().anyMatch(c->c instanceof PropertyValueConstraint.DartObjectReferenceValues))
                for(String bad:List.of("wrong","unsafe")){
                    before=reopen(base,source(base));current=before;
                    if(Set.of("width","margin").contains(p.name().value()))current=apply(current,new SetProperty(bar.id(),new PropertyName("behavior"),new PropertyValue.EnumValue("SnackBarBehavior","floating")));
                    current=apply(current,new SetProperty(id,p.name(),ref(bad)));assertAnalysis(before,current,"probe.dart",false);rejected++;
                }
        }
        for(String name:List.of("onVisible","onPressed")) {
            before=reopen(base,source(base));current=apply(before,new SetProperty(name.equals("onVisible")?bar.id():action.id(),new PropertyName(name),ref("wrongCallback")));
            assertAnalysis(before,current,"probe.dart",false);rejected++;
        }
        for(String p:List.of("label","onPressed")) {
            before=reopen(base,source(base));current=apply(before,new SetProperty(action.id(),new PropertyName(p),new PropertyValue.DartObjectReferenceValue(
                Optional.empty(),p.equals("label")?"labelFactory":"callbackFactory",Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION,Optional.of(false))));
            assertAnalysis(before,current,"probe.dart",true);save("factory_"+p,current);
        }
        for(String shape:CardWidgetPropertySchema.shapeKinds()){
            before=reopen(base,source(base));current=apply(before,new SetProperty(bar.id(),new PropertyName("shapeKind"),new PropertyValue.StringValue(shape)));
            for(String p:CardWidgetPropertySchema.builtInShapePropertyNames())
                if(CardWidgetPropertySchema.isShapeDetailProperty(p)&&CardWidgetPropertySchema.shapePropertyAppliesToKind(p,shape))
                    current=apply(current,new SetProperty(bar.id(),new PropertyName(p),dev.flutter.netbeans.plugin.designer.properties.CardPropertyContractTest.value(p)));
            assertAnalysis(before,current,"probe.dart",true);save("shape_"+shape,current);
        }
        before=reopen(base,source(base));current=apply(before,new CreateEventHandler(bar.id(),new PropertyName("onVisible"),"_visible"));
        current=apply(current,new CreateEventHandler(action.id(),new PropertyName("onPressed"),"_pressed"));
        assertAnalysis(before,current,"probe.dart",true);save("events",current);
        before=reopen(base,source(base));current=before;
        for(var p:SnackBarWidgetPropertySchema.properties(false))if(p.acceptedKinds().contains(PropertyValueKind.NULL)&&!p.name().value().equals("animation"))
            current=apply(current,new SetProperty(bar.id(),p.name(),new PropertyValue.NullValue()));
        for(var p:SnackBarWidgetPropertySchema.properties(true))if(p.acceptedKinds().contains(PropertyValueKind.NULL))
            current=apply(current,new SetProperty(action.id(),p.name(),new PropertyValue.NullValue()));
        assertAnalysis(before,current,"probe.dart",true);save("nullable",current);
        Files.writeString(project.resolve("native_test.dart"),nativeTests());
        run(List.of(flutter.toString(),"test","native_test.dart","--reporter","expanded"),"native.log");
        System.out.println("SnackBar: "+(cases.size()*24+5)+" generated/native cases; "+rejected+" unsafe source cases rejected.");
    }
    private static PropertyValue.DartObjectReferenceValue ref(String name){return new PropertyValue.DartObjectReferenceValue(Optional.empty(),name,Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());}
    private void save(String name,DesignerCommandSession s)throws Exception{saveCase(name,s);cases.add(name);}
    private String nativeTests(){
        var out=new StringBuilder("import 'package:flutter/material.dart';\nimport 'package:flutter_test/flutter_test.dart';\n");
        for(String name:cases)out.append("import 'package:dialog_contract/").append(name).append(".dart' as ").append(name).append(";\n");
        out.append("void main(){\n");
        for(String name:cases)for(String platform:List.of("android","fuchsia","iOS","linux","macOS","windows"))
            for(boolean rtl:List.of(false,true))for(boolean m3:List.of(false,true))
            out.append("""
                testWidgets('NAME PLATFORM RTL M3',(tester)async {
                  await tester.pumpWidget(MaterialApp(theme:ThemeData(useMaterial3:M3,platform:TargetPlatform.PLATFORM),
                    home:Directionality(textDirection:DIR,child:Scaffold(body:NAME.Sample()))));
                  await tester.pumpAndSettle();expect(find.byType(SnackBar),findsOneWidget);
                  expect(tester.takeException(),isNull);await tester.pumpWidget(const SizedBox());await tester.pump();expect(tester.takeException(),isNull);
                });
                """.replace("NAME",name).replace("PLATFORM",platform).replace("RTL",Boolean.toString(rtl)).replace("DIR",rtl?"TextDirection.rtl":"TextDirection.ltr").replace("M3",Boolean.toString(m3)));
        out.append("""
            testWidgets('messenger replaces animation and action calls user once then closes',(tester)async{
              await tester.pumpWidget(const MaterialApp(home:Scaffold(body:sources.Sample())));
              final state=tester.state<sources.Storage>(find.byType(sources.Sample));
              final seed=tester.widget<SnackBar>(find.byType(SnackBar));
              final messenger=GlobalKey<ScaffoldMessengerState>();
              await tester.pumpWidget(MaterialApp(scaffoldMessengerKey:messenger,home:const Scaffold(body:SizedBox())));
              final shown=messenger.currentState!.showSnackBar(seed);
              SnackBarClosedReason? reason;shown.closed.then((v)=>reason=v);
              await tester.pumpAndSettle();
              expect(tester.widget<SnackBar>(find.byType(SnackBar)).animation,isNot(same(seed.animation)));
              expect(state.visible,1);
              await tester.tap(find.text('Undo'));await tester.pumpAndSettle();
              expect(state.presses,1);expect(reason,SnackBarClosedReason.action);expect(find.byType(SnackBar),findsNothing);expect(tester.takeException(),isNull);
            });
            for(final persist in [false,true]) {
              testWidgets('persist $persist and native timeout',(tester)async{
                final messenger=GlobalKey<ScaffoldMessengerState>();
                await tester.pumpWidget(MaterialApp(scaffoldMessengerKey:messenger,home:const Scaffold()));
                final shown=messenger.currentState!.showSnackBar(SnackBar(content:const Text('Timed'),persist:persist,duration:const Duration(milliseconds:100)));
                SnackBarClosedReason? reason;shown.closed.then((v)=>reason=v);
                await tester.pumpAndSettle();await tester.pump(const Duration(seconds:1));await tester.pumpAndSettle();
                expect(find.byType(SnackBar),persist?findsOneWidget:findsNothing);
                expect(reason,persist?isNull:SnackBarClosedReason.timeout);expect(tester.takeException(),isNull);
              });
            }
            testWidgets('native close icon reason',(tester)async{
              final messenger=GlobalKey<ScaffoldMessengerState>();
              await tester.pumpWidget(MaterialApp(scaffoldMessengerKey:messenger,home:const Scaffold()));
              final shown=messenger.currentState!.showSnackBar(const SnackBar(content:Text('Close'),showCloseIcon:true,persist:true));
              SnackBarClosedReason? reason;shown.closed.then((v)=>reason=v);await tester.pumpAndSettle();
              await tester.tap(find.byIcon(Icons.close));await tester.pumpAndSettle();expect(reason,SnackBarClosedReason.dismiss);expect(tester.takeException(),isNull);
            });
            test('WidgetStateColor native dependency remains enforced',(){
              expect(()=>SnackBarAction(label:'x',onPressed:(){},backgroundColor:WidgetStateColor.resolveWith((_)=>Colors.red),disabledBackgroundColor:Colors.blue),throwsAssertionError);
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
