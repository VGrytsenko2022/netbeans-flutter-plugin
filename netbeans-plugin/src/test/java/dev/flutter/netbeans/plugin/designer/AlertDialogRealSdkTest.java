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
class AlertDialogRealSdkTest {
    @TempDir(cleanup=CleanupMode.ON_SUCCESS) Path project;
    private Path sdk,flutter,dart,lib;
    private static final SlotName CHILDREN=new SlotName("children");
    private final List<String> cases=new ArrayList<>();



    @Test void bothConstructorsSourcesAllShapesStylesAndNativePlatforms()throws Exception{
        initialize();
        String members="""
            // User member is preserved.
            int _userValue() => 73;
            Key? get keyValue => const ValueKey('alert');
            Color? get colorValue => const Color(0xffabcdef);
            double? get elevationValue => 3.5;
            String? get labelValue => 'Alert';
            Duration get durationValue => const Duration(milliseconds:120);
            Curve get curveValue => Curves.easeIn;
            EdgeInsets? get nullableInsets => const EdgeInsets.all(12);
            EdgeInsets get insetsValue => const EdgeInsets.all(12);
            EdgeInsetsGeometry? get geometryValue => const EdgeInsetsDirectional.all(8);
            TextStyle? get styleValue => const TextStyle(fontSize:16);
            ShapeBorder? get shapeValue => const StadiumBorder();
            AlignmentGeometry? get alignmentValue => AlignmentDirectional.bottomStart;
            BoxConstraints? get constraintsValue => const BoxConstraints(minWidth:100,maxWidth:400);
            ScrollController? get controllerValue => null;
            Object get wrong => Object();
            dynamic get unsafe => null;
            Duration? get nullableDuration => null;
            Curve? get nullableCurve => null;
            """;
        var root=group(StableId.random(),List.of());var baseline=open(root,"probe.dart",members);
        for(boolean adaptive:List.of(false,true)){
            var d=BuiltInWidgetCatalog.getDefault().find(adaptive?AlertDialogWidgetPropertySchema.ADAPTIVE_TYPE:AlertDialogWidgetPropertySchema.TYPE).orElseThrow();
            var child=new WidgetNode(StableId.random(),new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"),new PropertyValue.StringValue("Message")),Map.of());
            var widget=new WidgetNode(StableId.random(),d.typeId(),Map.of(),Map.of(new SlotName("content"),WidgetSlot.SingleSlot.of(child)));
            var before=reopen(baseline,source(baseline));var current=apply(before,new AddWidget(place(root.id(),0),widget));
            assertAnalysis(before,current,"probe.dart",true);save("default_"+adaptive,current);
            var seed=reopen(current,source(current));
            var sources=new LinkedHashMap<String,String>();
            sources.put("key","keyValue");sources.put("backgroundColor","colorValue");sources.put("iconColor","colorValue");
            sources.put("shadowColor","colorValue");sources.put("surfaceTintColor","colorValue");sources.put("elevation","elevationValue");
            sources.put("actionsOverflowButtonSpacing","elevationValue");sources.put("semanticLabel","labelValue");
            for(String n:List.of("iconPadding","titlePadding","contentPadding","actionsPadding","buttonPadding"))sources.put(n,"geometryValue");
            for(String n:AlertDialogWidgetPropertySchema.styleFamilies())sources.put(n,"styleValue");
            sources.put("insetPadding",adaptive?"insetsValue":"nullableInsets");sources.put("shape","shapeValue");
            sources.put("alignment","alignmentValue");sources.put("constraints","constraintsValue");
            if(adaptive){sources.put("scrollController","controllerValue");sources.put("actionScrollController","controllerValue");
                sources.put("insetAnimationDurationUs","durationValue");sources.put("insetAnimationCurve","curveValue");}
            before=reopen(seed,source(seed));current=before;
            for(var e:sources.entrySet())current=apply(current,new SetProperty(widget.id(),new PropertyName(e.getKey()),ref(e.getValue())));
            assertAnalysis(before,current,"probe.dart",true);save("sources_"+adaptive,current);
            for(String n:sources.keySet())for(String invalid:List.of("wrong","unsafe")){
                before=reopen(seed,source(seed));current=apply(before,new SetProperty(widget.id(),new PropertyName(n),ref(invalid)));
                assertAnalysis(before,current,"probe.dart",false);
            }
            if(adaptive)for(var e:Map.of("insetPadding","nullableInsets","insetAnimationDurationUs","nullableDuration","insetAnimationCurve","nullableCurve").entrySet()){
                before=reopen(seed,source(seed));current=apply(before,new SetProperty(widget.id(),new PropertyName(e.getKey()),ref(e.getValue())));
                assertAnalysis(before,current,"probe.dart",false);
            }
            before=reopen(seed,source(seed));current=before;
            for(String n:sources.keySet())if(!adaptive||!Set.of("insetPadding","insetAnimationDurationUs","insetAnimationCurve").contains(n))
                current=apply(current,new SetProperty(widget.id(),new PropertyName(n),new PropertyValue.NullValue()));
            if(adaptive){
                current=apply(current,new SetProperty(widget.id(),new PropertyName("insetAnimationDurationUs"),new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(120000))));
                current=apply(current,new SetProperty(widget.id(),new PropertyName("insetAnimationCurve"),new PropertyValue.StringValue("easeInOutCubicEmphasized")));
            }
            assertAnalysis(before,current,"probe.dart",true);save("nullable_"+adaptive,current);
            for(String shape:CardWidgetPropertySchema.shapeKinds()){
                before=reopen(seed,source(seed));current=apply(before,new SetProperty(widget.id(),new PropertyName("shapeKind"),new PropertyValue.StringValue(shape)));
                for(String n:CardWidgetPropertySchema.builtInShapePropertyNames())if(CardWidgetPropertySchema.isShapeDetailProperty(n)&&CardWidgetPropertySchema.shapePropertyAppliesToKind(n,shape))
                    current=apply(current,new SetProperty(widget.id(),new PropertyName(n),dev.flutter.netbeans.plugin.designer.properties.CardPropertyContractTest.value(n)));
                for(String family:AlertDialogWidgetPropertySchema.styleFamilies()){
                    current=apply(current,new SetProperty(widget.id(),new PropertyName(family+"FontSize"),new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(18))));
                    current=apply(current,new SetProperty(widget.id(),new PropertyName(family+"FontFamily"),new PropertyValue.StringValue("Roboto")));
                    current=apply(current,new SetProperty(widget.id(),new PropertyName(family+"LocaleLanguageCode"),new PropertyValue.StringValue("en")));
                    current=apply(current,new SetProperty(widget.id(),new PropertyName(family+"DecorationUnderline"),new PropertyValue.BooleanValue(true)));
                }
                current=apply(current,new SetProperty(widget.id(),new PropertyName("actionsOverflowAlignment"),new PropertyValue.EnumValue("OverflowBarAlignment","center")));
                assertAnalysis(before,current,"probe.dart",true);save("shape_"+adaptive+"_"+shape,current);
            }
        }
        Files.writeString(project.resolve("native_test.dart"),nativeTests());
        run(List.of(flutter.toString(),"test","native_test.dart","--reporter","expanded"),"native.log");
        System.out.println("AlertDialog: "+(cases.size()*12)+" generated native cases passed on six platforms and both directions; 87 unsafe source cases rejected.");
    }
    private static PropertyValue.DartObjectReferenceValue ref(String name){return new PropertyValue.DartObjectReferenceValue(Optional.empty(),name,Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());}
    private void save(String name,DesignerCommandSession session)throws Exception{saveCase(name,session);cases.add(name);}
    private String nativeTests(){
        var out=new StringBuilder("import 'package:flutter/material.dart';\\nimport 'package:flutter/cupertino.dart';\\nimport 'package:flutter_test/flutter_test.dart';\\n".replace("\\n","\n"));
        for(String name:cases)out.append("import 'package:dialog_contract/").append(name).append(".dart' as ").append(name).append(";\n");
        out.append("void main(){\n");
        for(String name:cases)for(String platform:List.of("android","fuchsia","iOS","linux","macOS","windows"))for(boolean rtl:List.of(false,true)){
            boolean cupertino=name.contains("true")&&(platform.equals("iOS")||platform.equals("macOS"));
            out.append("""
                testWidgets('NAME PLATFORM RTL',(tester)async{
                  await tester.pumpWidget(MaterialApp(theme:ThemeData(platform:TargetPlatform.PLATFORM),
                    home:Directionality(textDirection:DIR,child:Scaffold(body:NAME.Sample()))));
                  await tester.pumpAndSettle();
                  expect(find.byType(CupertinoAlertDialog),CUPERTINO);
                  expect(find.byType(Dialog),MATERIAL);
                  expect(tester.takeException(),isNull);
                  await tester.pumpWidget(const SizedBox());await tester.pump();
                });
                """.replace("NAME",name).replace("PLATFORM",platform).replace("RTL",Boolean.toString(rtl))
                    .replace("DIR",rtl?"TextDirection.rtl":"TextDirection.ltr")
                    .replace("CUPERTINO",cupertino?"findsOneWidget":"findsNothing")
                    .replace("MATERIAL",cupertino?"findsNothing":"findsOneWidget"));
        }
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
