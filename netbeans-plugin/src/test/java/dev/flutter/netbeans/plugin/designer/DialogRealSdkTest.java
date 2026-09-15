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
class DialogRealSdkTest {
    @TempDir(cleanup=CleanupMode.ON_SUCCESS) Path project;
    private Path sdk,flutter,dart,lib;
    private static final SlotName CHILDREN=new SlotName("children");
    private final List<String> cases=new ArrayList<>();



    @Test void bothConstructorsExactTypedProofsAndGeneratedFlutterRendering()throws Exception {
        initialize();
        String members="""
                // User member is preserved.
                int _userValue() => 73;
                Key? get keyValue => const ValueKey('dialog');
                Color? get colorValue => const Color(0xffabcdef);
                double? get elevationValue => 3.5;
                Duration get durationValue => const Duration(milliseconds:123);
                Curve get curveValue => Curves.easeIn;
                EdgeInsets? get insetsValue => const EdgeInsets.all(12);
                EdgeInsets? makeInsets() => insetsValue;
                static EdgeInsets? get staticInsets => const EdgeInsets.all(12);
                static EdgeInsets? makeStaticInsets() => staticInsets;
                ShapeBorder? get shapeValue => const StadiumBorder();
                AlignmentGeometry? get alignmentValue => AlignmentDirectional.bottomStart;
                BoxConstraints? get constraintsValue => const BoxConstraints(minWidth:100,maxWidth:400);
                Object get wrong => Object();
                dynamic get unsafe => const EdgeInsets.all(12);
                EdgeInsetsDirectional get directional => const EdgeInsetsDirectional.all(12);
                EdgeInsetsGeometry? get geometry => const EdgeInsets.all(12);
                Duration? get nullableDuration => null;
                Curve? get nullableCurve => null;
                """;
        Files.writeString(lib.resolve("sources.dart"),"""
                import 'package:flutter/material.dart';
                EdgeInsets? get insets => const EdgeInsets.all(9);
                EdgeInsets? makeInsets() => insets;
                class Insets {
                  static EdgeInsets? get insets => const EdgeInsets.all(9);
                  static EdgeInsets? makeInsets() => insets;
                }
                """);
        var root=group(StableId.random(),List.of());var baseline=open(root,"probe.dart",members);
        for(boolean full:List.of(false,true)){
            var d=BuiltInWidgetCatalog.getDefault().find(full?DialogWidgetPropertySchema.FULLSCREEN_TYPE:DialogWidgetPropertySchema.TYPE).orElseThrow();
            var box=new WidgetNode(StableId.random(),new WidgetTypeId("flutter.widgets.SizedBox"),
                    Map.of(new PropertyName("width"),new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(160)),new PropertyName("height"),new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(80))),Map.of());
            var widget=new WidgetNode(StableId.random(),d.typeId(),Map.of(),Map.of(new SlotName("child"),WidgetSlot.SingleSlot.of(box)));
            var before=reopen(baseline,source(baseline));var current=apply(before,new AddWidget(place(root.id(),0),widget));
            assertAnalysis(before,current,"probe.dart",true);save(full?"fullscreen":"ordinary",current);
            var seed=reopen(current,source(current));
            var sources=new LinkedHashMap<String,String>();sources.put("key","keyValue");sources.put("backgroundColor","colorValue");
            sources.put("insetAnimationDurationUs","durationValue");sources.put("insetAnimationCurve","curveValue");
            if(!full){sources.put("shadowColor","colorValue");sources.put("surfaceTintColor","colorValue");sources.put("elevation","elevationValue");
                sources.put("insetPadding","insetsValue");sources.put("alignment","alignmentValue");sources.put("constraints","constraintsValue");sources.put("shape","shapeValue");}
            before=reopen(seed,source(seed));current=before;
            for(var entry:sources.entrySet())current=apply(current,new SetProperty(widget.id(),new PropertyName(entry.getKey()),ref(entry.getValue())));
            assertAnalysis(before,current,"probe.dart",true);save("typed_"+full,current);
            for(String field:sources.keySet())for(String invalid:List.of("wrong","unsafe")){
                before=reopen(seed,source(seed));current=apply(before,new SetProperty(widget.id(),new PropertyName(field),ref(invalid)));
                assertAnalysis(before,current,"probe.dart",false);
            }
            for(var entry:Map.of("insetAnimationDurationUs","nullableDuration","insetAnimationCurve","nullableCurve").entrySet()){
                before=reopen(seed,source(seed));current=apply(before,new SetProperty(widget.id(),new PropertyName(entry.getKey()),ref(entry.getValue())));
                assertAnalysis(before,current,"probe.dart",false);
            }
            before=reopen(seed,source(seed));current=before;
            current=apply(current,new SetProperty(widget.id(),new PropertyName("key"),new PropertyValue.StringValue("literal-key")));
            current=apply(current,new SetProperty(widget.id(),new PropertyName("insetAnimationDurationUs"),new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(120000))));
            current=apply(current,new SetProperty(widget.id(),new PropertyName("insetAnimationCurve"),new PropertyValue.StringValue("easeInOutCubicEmphasized")));
            current=apply(current,new SetProperty(widget.id(),new PropertyName("semanticsRole"),new PropertyValue.EnumValue("SemanticsRole","alertDialog")));
            for(String field:sources.keySet())if(!Set.of("key","insetAnimationDurationUs","insetAnimationCurve").contains(field))
                current=apply(current,new SetProperty(widget.id(),new PropertyName(field),new PropertyValue.NullValue()));
            assertAnalysis(before,current,"probe.dart",true);save("nulls_and_literals_"+full,current);
            if(!full){
                for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
                    String name=member?(imported?"Insets":"Storage"):(factory?"makeInsets":imported?"insets":"insetsValue");
                    var ref=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:dialog_contract/sources.dart"):Optional.empty(),name,
                        member?Optional.of(imported?(factory?"makeInsets":"insets"):(factory?"makeStaticInsets":"staticInsets")):Optional.empty(),
                        factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                        factory?Optional.of(false):Optional.empty());
                    before=reopen(seed,source(seed));current=apply(before,new SetProperty(widget.id(),new PropertyName("insetPadding"),ref));
                    assertAnalysis(before,current,"probe.dart",true);save("insets_"+cases.size(),current);
                }
                for(String wrong:List.of("directional","geometry")){
                    before=reopen(seed,source(seed));current=apply(before,new SetProperty(widget.id(),new PropertyName("insetPadding"),ref(wrong)));
                    assertAnalysis(before,current,"probe.dart",false);
                }
                for(String kind:CardWidgetPropertySchema.shapeKinds()){
                    before=reopen(seed,source(seed));current=apply(before,new SetProperty(widget.id(),new PropertyName("shapeKind"),new PropertyValue.StringValue(kind)));
                    for(String field:CardWidgetPropertySchema.builtInShapePropertyNames())
                        if(CardWidgetPropertySchema.isShapeDetailProperty(field)&&CardWidgetPropertySchema.shapePropertyAppliesToKind(field,kind))
                            current=apply(current,new SetProperty(widget.id(),new PropertyName(field),dev.flutter.netbeans.plugin.designer.properties.CardPropertyContractTest.value(field)));
                    current=apply(current,new SetProperty(widget.id(),new PropertyName("insetPadding"),new PropertyValue.EdgeInsetsValue(java.math.BigDecimal.ONE,java.math.BigDecimal.TWO,java.math.BigDecimal.ONE,java.math.BigDecimal.TWO)));
                    assertAnalysis(before,current,"probe.dart",true);save("shape_"+kind,current);
                }
            }
        }
        // All native semantic enum symbols are analyzed together, without mounting invalid semantic hierarchies.
        var before=reopen(baseline,source(baseline));var current=before;
        int index=0;
        for(String role:DialogWidgetPropertySchema.ROLES){
            var widget=new WidgetNode(StableId.random(),DialogWidgetPropertySchema.TYPE,
                Map.of(new PropertyName("semanticsRole"),new PropertyValue.EnumValue("SemanticsRole",role)),Map.of());
            current=apply(current,new AddWidget(place(root.id(),index++),widget));
        }
        assertAnalysis(before,current,"probe.dart",true);
        Files.writeString(project.resolve("native_test.dart"),nativeTests());
        run(List.of(flutter.toString(),"test","native_test.dart","--reporter","expanded"),"native.log");
        System.out.println("Dialog: "+(cases.size()*2)+" generated SDK render cases passed; strict nullable EdgeInsets and all semantic roles verified.");
    }
    private static PropertyValue.DartObjectReferenceValue ref(String name){return new PropertyValue.DartObjectReferenceValue(Optional.empty(),name,Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());}
    private void save(String name,DesignerCommandSession session)throws Exception{saveCase(name,session);cases.add(name);}
    private String nativeTests(){
        var out=new StringBuilder("import 'package:flutter/material.dart';\nimport 'package:flutter_test/flutter_test.dart';\n");
        for(String name:cases)out.append("import 'package:dialog_contract/").append(name).append(".dart' as ").append(name).append(";\n");
        out.append("void main(){\n");
        for(String name:cases)for(boolean rtl:List.of(false,true))out.append("""
              testWidgets('NAME RTL', (tester) async {
                await tester.pumpWidget(MaterialApp(home:Directionality(textDirection:DIR,child:Scaffold(body:NAME.Sample()))));
                await tester.pumpAndSettle();
                expect(find.byType(Dialog),findsOneWidget);
                final widget=tester.widget<Dialog>(find.byType(Dialog));
                expect(widget.child,isNotNull);
                expect(tester.takeException(),isNull);
                await tester.pumpWidget(const SizedBox());await tester.pump();
              });
                """.replace("NAME",name).replace("RTL",Boolean.toString(rtl)).replace("DIR",rtl?"TextDirection.rtl":"TextDirection.ltr"));
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
