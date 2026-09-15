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
class CustomSingleChildLayoutRealSdkTest {
    @TempDir(cleanup=CleanupMode.ON_SUCCESS) Path project;
    private Path sdk,flutter,dart,lib;
    private static final StableId NESTED_ID=StableId.random();
    private static final SlotName CHILDREN=new SlotName("children");
    private final List<String> cases=new ArrayList<>();

    @Test void completeConstructorTypedSourcesScaffoldingAndNativeRelayout() throws Exception {
        initialize();
        Files.writeString(lib.resolve("delegates.dart"),delegateSource());
        var baseline=open(group(StableId.random(),List.of()),"probe.dart","""
                // User member is preserved.
                int _userValue() => 73;
                SingleChildLayoutDelegate get _layout => refs.layout;
                SingleChildLayoutDelegate _factory() => refs.layout;
                static SingleChildLayoutDelegate get layout => refs.layout;
                static SingleChildLayoutDelegate create() => refs.layout;
                SingleChildLayoutDelegate? get _nullable => null;
                dynamic get _dynamic => refs.layout;
                Object get _wrong => Object();
                String get _unrelated => 'not a delegate';
                """);
        baseline=reopen(baseline,"import 'package:custom_single_layout_contract/delegates.dart' as refs;\n"+source(baseline));
        var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault()
                .find(CustomSingleChildLayoutWidgetPropertySchema.TYPE).orElseThrow(),NESTED_ID);
        var current=apply(baseline,new AddWidget(place(baseline.current().document().root().id(),0),prototype));
        assertAnalysis(baseline,current,"probe.dart",true);save("starter_empty",current);
        assertTrue(source(current).contains("class _FlutterDesignerSingleChildLayoutDelegate"));
        assertArrayEquals(baseline.current().dartCandidateBytes(),current.undo().session().current().dartCandidateBytes());
        assertArrayEquals(current.current().dartCandidateBytes(),current.undo().session().redo().session().current().dartCandidateBytes());
        var before=reopen(current,source(current));
        current=apply(before,new AddWidget(new WidgetPlacement(NESTED_ID,new SlotName("child"),0),adapter("Child",60)));
        assertAnalysis(before,current,"probe.dart",true);save("starter_child",current);
        for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)) {
            var reference=new PropertyValue.DartObjectReferenceValue(
                    imported?Optional.of("package:custom_single_layout_contract/delegates.dart"):Optional.empty(),
                    member?(imported?"Values":"Storage"):(imported?(factory?"create":"layout"):(factory?"_factory":"_layout")),
                    member?Optional.of(factory?"create":"layout"):Optional.empty(),
                    factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                    factory?Optional.of(false):Optional.empty());
            before=reopen(current,source(current));
            current=apply(before,new SetProperty(NESTED_ID,new PropertyName("delegate"),reference));
            assertAnalysis(before,current,"probe.dart",true);
            save("source_"+(imported?1:0)+(member?1:0)+(factory?1:0),current);
        }
        for(String name:List.of("_nullable","_dynamic","_wrong","_unrelated")) {
            before=reopen(current,source(current));
            var bad=apply(before,new SetProperty(NESTED_ID,new PropertyName("delegate"),
                    new PropertyValue.DartObjectReferenceValue(Optional.empty(),name,Optional.empty(),
                            PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty())));
            assertAnalysis(before,bad,"probe.dart",false);
        }
        for(boolean named:List.of(false,true)) {
            before=reopen(current,source(current));
            current=apply(before,new SetProperty(NESTED_ID,new PropertyName("delegate"),
                    new PropertyValue.DartObjectReferenceValue(Optional.of("package:custom_single_layout_contract/delegates.dart"),
                            "ConstLayout",named?Optional.of("named"):Optional.empty(),
                            PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION,Optional.of(true))));
            assertAnalysis(before,current,"probe.dart",true);save(named?"const_named":"const_default",current);
        }
        before=reopen(current,source(current));
        current=apply(before,new SetProperty(NESTED_ID,new PropertyName("delegate"),CustomSingleChildLayoutWidgetPropertySchema.INITIAL_DELEGATE));
        assertAnalysis(before,current,"probe.dart",true);
        var edited=reopen(current,source(current).replace("const Size(128, 96)","const Size(160, 110)"));
        assertTrue(source(edited).contains("const Size(160, 110)"));
        var duplicate=apply(edited,new AddWidget(place(edited.current().document().root().id(),1),
                WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(prototype.type()).orElseThrow(),StableId.random())));
        assertEquals(1,source(duplicate).split("class _FlutterDesignerSingleChildLayoutDelegate",-1).length-1);
        assertTrue(source(duplicate).contains("const Size(160, 110)"));
        assertAnalysis(edited,duplicate,"probe.dart",true);
        save("edited",edited);
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("layout_test.dart"),nativeTests());
        run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");
        String log=Files.readString(project.resolve("flutter-test.log"));
        int expected=cases.size()*2+2;
        assertTrue(log.contains("+"+expected+": All tests passed!"),log);
        System.out.println("CustomSingleChildLayout: "+expected+" generated/native SDK cases passed.");
    }
    private void save(String name,DesignerCommandSession session)throws Exception {saveCase(name,session);cases.add(name);}
    private String delegateSource() {return """
            import 'package:flutter/widgets.dart';
            class Signal extends ChangeNotifier {
              int adds=0,removes=0;
              double shift=0;
              @override
              void addListener(VoidCallback listener){adds++;super.addListener(listener);}
              @override
              void removeListener(VoidCallback listener){removes++;super.removeListener(listener);}
              void move(){shift+=5;notifyListeners();}
            }
            final signal=Signal();
            class ProjectLayout extends SingleChildLayoutDelegate {
              ProjectLayout([this.extra=0]):super(relayout:signal);
              final double extra;
              @override
              Size getSize(BoxConstraints constraints)=>constraints.constrain(Size(200+extra,140));
              @override
              BoxConstraints getConstraintsForChild(BoxConstraints constraints)=>const BoxConstraints.tightFor(width:48,height:24);
              @override
              Offset getPositionForChild(Size size,Size childSize)=>Offset(11+signal.shift+extra,17);
              @override
              bool shouldRelayout(covariant ProjectLayout oldDelegate)=>extra!=oldDelegate.extra;
            }
            class ConstLayout extends SingleChildLayoutDelegate {
              const ConstLayout();
              const ConstLayout.named();
              @override
              Size getSize(BoxConstraints constraints)=>constraints.constrain(const Size(200,140));
              @override
              BoxConstraints getConstraintsForChild(BoxConstraints constraints)=>const BoxConstraints.tightFor(width:48,height:24);
              @override
              Offset getPositionForChild(Size size,Size childSize)=>const Offset(11,17);
              @override
              bool shouldRelayout(covariant ConstLayout oldDelegate)=>false;
            }
            SingleChildLayoutDelegate get layout=>ProjectLayout();
            SingleChildLayoutDelegate create()=>layout;
            class Values {
              static SingleChildLayoutDelegate get layout=>ProjectLayout();
              static SingleChildLayoutDelegate create()=>layout;
            }
            """;}
    private String nativeTests() {
        var out=new StringBuilder("""
                import 'package:flutter/material.dart';
                import 'package:flutter/rendering.dart';
                import 'package:flutter_test/flutter_test.dart';
                import 'package:custom_single_layout_contract/delegates.dart' as refs;
                """);
        for(String name:cases)out.append("import '../lib/"+name+".dart' as "+name+";\n");
        out.append("void main(){\n");
        for(String name:cases)for(boolean rtl:List.of(false,true)){
            boolean starter=name.startsWith("starter")||name.equals("edited"),empty=name.equals("starter_empty");
            int width=name.equals("edited")?160:starter?128:200,height=name.equals("edited")?110:starter?96:140;
            out.append("testWidgets('"+name+" rtl="+rtl+"',(tester) async {\n");
            out.append("final adds=refs.signal.adds,removes=refs.signal.removes;\n");
            out.append("await tester.pumpWidget(MaterialApp(home:Directionality(textDirection:TextDirection."+(rtl?"rtl":"ltr")+",child:const "+name+".Sample())));\n");
            out.append("final finder=find.descendant(of:find.byType("+name+".Sample),matching:find.byType(CustomSingleChildLayout));\n");
            out.append("final w=tester.widget<CustomSingleChildLayout>(finder),r=tester.renderObject<RenderCustomSingleChildLayoutBox>(finder);\n");
            out.append("expect(r.size,const Size("+width+","+height+"));expect(w.child==null,"+empty+");\n");
            if(!empty){
                out.append("expect(r.child!.size,const Size("+(starter?"60,60":"48,24")+"));\n");
                if(starter)out.append("expect((r.child!.parentData! as BoxParentData).offset,const Offset("+((width-60)/2)+","+((height-60)/2)+"));\n");
                else if(name.startsWith("const"))out.append("expect((r.child!.parentData! as BoxParentData).offset,const Offset(11,17));\n");
                else out.append("""
                        expect((r.child!.parentData! as BoxParentData).offset,Offset(11+refs.signal.shift,17));
                        final previous=(r.child!.parentData! as BoxParentData).offset;
                        refs.signal.move();await tester.pump();
                        expect((r.child!.parentData! as BoxParentData).offset,previous+const Offset(5,0));
                        """);
            }
            out.append("expect(tester.takeException(),isNull);await tester.pumpWidget(const SizedBox());expect(refs.signal.adds-adds,refs.signal.removes-removes);});\n");
        }
        out.append("""
                testWidgets('delegate replacement relayouts and detaches old notifications',(tester) async{
                  Future<void> pump(double extra)=>tester.pumpWidget(Directionality(textDirection:TextDirection.ltr,
                    child:Center(child:CustomSingleChildLayout(delegate:refs.ProjectLayout(extra),child:const SizedBox()))));
                  await pump(0);final r=tester.renderObject<RenderCustomSingleChildLayoutBox>(find.byType(CustomSingleChildLayout));
                  await pump(20);expect(tester.renderObject(find.byType(CustomSingleChildLayout)),same(r));
                  expect(r.size,const Size(220,140));
                  expect((r.child!.parentData! as BoxParentData).offset,Offset(31+refs.signal.shift,17));
                  expect(tester.takeException(),isNull);
                });
                testWidgets('starter handles tight zero and both unbounded axes without a child',(tester) async{
                  await tester.pumpWidget(const MaterialApp(home:starter_empty.Sample()));
                  final delegate=tester.widget<CustomSingleChildLayout>(find.byType(CustomSingleChildLayout)).delegate;
                  for(final constraints in [const BoxConstraints(),const BoxConstraints.tightFor(width:0,height:0),
                    const BoxConstraints.tightFor(width:300,height:200),const BoxConstraints(maxWidth:70,maxHeight:40)]){
                    await tester.pumpWidget(Directionality(textDirection:TextDirection.ltr,child:Center(child:UnconstrainedBox(
                      child:ConstrainedBox(constraints:constraints,child:CustomSingleChildLayout(delegate:delegate))))));
                    final r=tester.renderObject<RenderCustomSingleChildLayoutBox>(find.byType(CustomSingleChildLayout));
                    expect(r.size.isFinite,isTrue);expect(constraints.isSatisfiedBy(r.size),isTrue);
                    expect(tester.takeException(),isNull);
                  }
                });
                }
                """);
        return out.toString();
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
    private void initialize() throws Exception {
        sdk = Path.of(System.getProperty("flutter.events.sdk")).toRealPath();
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        flutter = sdk.resolve(windows ? "bin/flutter.bat" : "bin/flutter");
        dart = sdk.resolve(windows ? "bin/cache/dart-sdk/bin/dart.exe" : "bin/cache/dart-sdk/bin/dart");
        Files.writeString(project.resolve("pubspec.yaml"), """
                name: custom_single_layout_contract
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
