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
class CustomMultiChildLayoutRealSdkTest {
    @TempDir(cleanup=CleanupMode.ON_SUCCESS) Path project;
    private Path sdk,flutter,dart,lib;
    private static final StableId NESTED_ID=StableId.random();
    private static final SlotName CHILDREN=new SlotName("children");
    private final List<String> cases=new ArrayList<>();


    private static final StableId ID1=StableId.random(),ID2=StableId.random();
    @Test void completeTypedSourcesStableIdsAndNativeDelegateLifecycle() throws Exception {
        initialize();Files.writeString(lib.resolve("delegates.dart"),delegateSource());
        var baseline=open(group(StableId.random(),List.of()),"probe.dart","""
            // User member is preserved.
            int _userValue() => 73;
            MultiChildLayoutDelegate get _layout => refs.layout;
            MultiChildLayoutDelegate _factory() => refs.layout;
            static MultiChildLayoutDelegate get layout => refs.layout;
            static MultiChildLayoutDelegate create() => refs.layout;
            Object get _id => refs.identity;
            Object _idFactory() => refs.createId();
            static Object get identity => refs.identity;
            static Object createId() => refs.createId();
            MultiChildLayoutDelegate? get _nullable => null;
            dynamic get _dynamic => refs.layout;
            Object get _wrong => Object();
            Object? get _nullableId => null;
            dynamic get _dynamicId => Object();
            """);
        baseline=reopen(baseline,"import 'package:custom_multi_layout_contract/delegates.dart' as refs;\n"+source(baseline));
        var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(CustomMultiChildLayoutWidgetPropertySchema.TYPE).orElseThrow(),NESTED_ID);
        var current=apply(baseline,new AddWidget(place(baseline.current().document().root().id(),0),prototype));
        assertAnalysis(baseline,current,"probe.dart",true);save("starter_empty",current);
        assertArrayEquals(baseline.current().dartCandidateBytes(),current.undo().session().current().dartCandidateBytes());
        assertArrayEquals(current.current().dartCandidateBytes(),current.undo().session().redo().session().current().dartCandidateBytes());
        for(var id:List.of(ID1,ID2)){
            var before=reopen(current,source(current));
            var child=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(LayoutIdWidgetPropertySchema.TYPE).orElseThrow(),id);
            current=apply(before,new AddWidget(place(NESTED_ID,id.equals(ID1)?0:1),child));
            assertAnalysis(before,current,"probe.dart",true);
        }
        save("starter_two",current);
        for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
            var before=reopen(current,source(current));
            var ref=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:custom_multi_layout_contract/delegates.dart"):Optional.empty(),
                member?(imported?"Values":"Storage"):(imported?(factory?"createId":"identity"):(factory?"_idFactory":"_id")),
                member?Optional.of(factory?"createId":"identity"):Optional.empty(),
                factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                factory?Optional.of(false):Optional.empty());
            current=apply(before,new SetProperty(ID1,new PropertyName("id"),ref));
            assertAnalysis(before,current,"probe.dart",true);
            save("id_"+(imported?1:0)+(member?1:0)+(factory?1:0),current);
        }
        for(boolean named:List.of(false,true)){
            var before=reopen(current,source(current));
            var ref=new PropertyValue.DartObjectReferenceValue(Optional.of("package:custom_multi_layout_contract/delegates.dart"),
                "ConstId",named?Optional.of("named"):Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION,Optional.of(true));
            current=apply(before,new SetProperty(ID1,new PropertyName("id"),ref));
            assertAnalysis(before,current,"probe.dart",true);save(named?"const_id_named":"const_id",current);
        }
        {
            var before=reopen(current,source(current));
            var ref=new PropertyValue.DartObjectReferenceValue(Optional.of("package:custom_multi_layout_contract/delegates.dart"),
                "Slot",Optional.of("first"),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
            current=apply(before,new SetProperty(ID1,new PropertyName("id"),ref));
            assertAnalysis(before,current,"probe.dart",true);save("enum_id",current);
        }
        for(String name:List.of("_nullableId","_dynamicId")){
            var before=reopen(current,source(current));
            var bad=apply(before,new SetProperty(ID1,new PropertyName("id"),reference(name)));
            assertAnalysis(before,bad,"probe.dart",false);
        }
        for(var value:List.<PropertyValue>of(new PropertyValue.StringValue(""),new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(3)),
                new PropertyValue.DoubleValue(new java.math.BigDecimal("2.5")),new PropertyValue.BooleanValue(false))){
            var before=reopen(current,source(current));
            current=apply(before,new SetProperty(ID1,new PropertyName("id"),value));
            assertAnalysis(before,current,"probe.dart",true);save("literal_"+cases.size(),current);
        }
        for(var id:List.of(ID1,ID2)){
            var before=reopen(current,source(current));
            current=apply(before,new SetProperty(id,new PropertyName("id"),new PropertyValue.StringValue(id.equals(ID1)?"first":"second")));
            assertAnalysis(before,current,"probe.dart",true);
        }
        for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
            var before=reopen(current,source(current));
            var ref=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:custom_multi_layout_contract/delegates.dart"):Optional.empty(),
                member?(imported?"Values":"Storage"):(imported?(factory?"create":"layout"):(factory?"_factory":"_layout")),
                member?Optional.of(factory?"create":"layout"):Optional.empty(),
                factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                factory?Optional.of(false):Optional.empty());
            current=apply(before,new SetProperty(NESTED_ID,new PropertyName("delegate"),ref));
            assertAnalysis(before,current,"probe.dart",true);save("delegate_"+(imported?1:0)+(member?1:0)+(factory?1:0),current);
        }
        for(String name:List.of("_nullable","_dynamic","_wrong")){
            var before=reopen(current,source(current));
            var bad=apply(before,new SetProperty(NESTED_ID,new PropertyName("delegate"),reference(name)));
            assertAnalysis(before,bad,"probe.dart",false);
        }
        var before=reopen(current,source(current));
        current=apply(before,new SetProperty(NESTED_ID,new PropertyName("delegate"),CustomMultiChildLayoutWidgetPropertySchema.INITIAL_DELEGATE));
        assertAnalysis(before,current,"probe.dart",true);
        var edited=reopen(current,source(current).replace("const Size(256, 192)","const Size(280, 210)"));
        save("edited",edited);
        var again=apply(edited,new AddWidget(place(edited.current().document().root().id(),1),
                WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(prototype.type()).orElseThrow(),StableId.random())));
        assertEquals(1,source(again).split("class _FlutterDesignerMultiChildLayoutDelegate",-1).length-1);
        assertTrue(source(again).contains("const Size(280, 210)"));assertAnalysis(edited,again,"probe.dart",true);
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("layout_test.dart"),nativeTests());
        run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");
        int expected=cases.size()*2+2;
        assertTrue(Files.readString(project.resolve("flutter-test.log")).contains("+"+expected+": All tests passed!"));
        System.out.println("CustomMultiChildLayout/LayoutId: "+expected+" generated/native SDK cases passed.");
    }
    private static PropertyValue.DartObjectReferenceValue reference(String name){
        return new PropertyValue.DartObjectReferenceValue(Optional.empty(),name,Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
    }
    private void save(String name,DesignerCommandSession s)throws Exception{saveCase(name,s);cases.add(name);}
    private String delegateSource(){return """
        import 'package:flutter/widgets.dart';
        enum Slot { first }
        class ConstId {const ConstId();const ConstId.named();}
        int creations=0;
        Object get identity=>const Symbol('project-id');
        Object createId(){creations++;return Object();}
        class Signal extends ChangeNotifier {
          int adds=0,removes=0;double shift=0;
          @override void addListener(VoidCallback l){adds++;super.addListener(l);}
          @override void removeListener(VoidCallback l){removes++;super.removeListener(l);}
          void move(){shift+=5;notifyListeners();}
        }
        final signal=Signal();
        class ProjectLayout extends MultiChildLayoutDelegate {
          ProjectLayout([this.extra=0]):super(relayout:signal);
          final double extra;
          @override Size getSize(BoxConstraints c)=>c.constrain(Size(200+extra,140));
          @override void performLayout(Size size){
            final second=layoutChild('second',const BoxConstraints.tightFor(width:70,height:20));
            positionChild('second',Offset(5,80+signal.shift));
            if(hasChild('first')){
              layoutChild('first',BoxConstraints.tightFor(width:second.width-10,height:30));
              positionChild('first',Offset(11+extra,17+signal.shift));
            }
          }
          @override bool shouldRelayout(covariant ProjectLayout oldDelegate)=>oldDelegate.extra!=extra;
        }
        MultiChildLayoutDelegate get layout=>ProjectLayout();
        MultiChildLayoutDelegate create()=>layout;
        class Values{
          static Object get identity=>const Symbol('static-id');
          static Object createId(){creations++;return Object();}
          static MultiChildLayoutDelegate get layout=>ProjectLayout();
          static MultiChildLayoutDelegate create()=>layout;
        }
        """;}
    private String nativeTests(){
        var out=new StringBuilder("""
            import 'package:flutter/material.dart';
            import 'package:flutter/rendering.dart';
            import 'package:flutter_test/flutter_test.dart';
            import 'package:custom_multi_layout_contract/delegates.dart' as refs;
            """);
        for(var name:cases)out.append("import '../lib/"+name+".dart' as "+name+";\n");
        out.append("void main(){\n");
        for(var name:cases)for(boolean rtl:List.of(false,true)){
            boolean custom=name.startsWith("delegate_"),empty=name.equals("starter_empty");
            int width=name.equals("edited")?280:custom?200:256,height=name.equals("edited")?210:custom?140:192;
            out.append("testWidgets('"+name+" rtl="+rtl+"',(tester)async{\n");
            out.append("final adds=refs.signal.adds,removes=refs.signal.removes,creations=refs.creations;\n");
            out.append("await tester.pumpWidget(MaterialApp(home:Directionality(textDirection:TextDirection."+(rtl?"rtl":"ltr")+",child:const "+name+".Sample())));\n");
            out.append("final f=find.descendant(of:find.byType("+name+".Sample),matching:find.byType(CustomMultiChildLayout));\n");
            out.append("final w=tester.widget<CustomMultiChildLayout>(f),r=tester.renderObject<RenderCustomMultiChildLayoutBox>(f);\n");
            out.append("expect(r.size,const Size("+width+","+height+"));expect(r.childCount,"+(empty?0:2)+");\n");
            if(name.startsWith("id_")&&name.endsWith("1"))out.append("expect(refs.creations-creations,1);\n");
            if(!empty){
                out.append("final first=r.firstChild!,pd= r.firstChild!.parentData! as MultiChildLayoutParentData;final second=pd.nextSibling!;\n");
                out.append("expect(pd.id,(w.children.first as LayoutId).id);expect((w.children.first.key! as ValueKey<Object>).value,pd.id);expect(first.size,const Size("+(custom?"60,30":"48,48")+"));\n");
                out.append("expect(second.size,const Size("+(custom?"70,20":"48,48")+"));\n");
                if(custom)out.append("""
                    expect(pd.offset,Offset(11,17+refs.signal.shift));
                    final old=pd.offset;refs.signal.move();await tester.pump();
                    expect(pd.offset,old+const Offset(0,5));
                    """);
                else out.append("expect(pd.offset,Offset.zero);expect((second.parentData! as MultiChildLayoutParentData).offset,const Offset(0,"+(height/2)+"));\n");
            }
            out.append("expect(tester.takeException(),isNull);await tester.pumpWidget(const SizedBox());expect(refs.signal.adds-adds,refs.signal.removes-removes);});\n");
        }
        out.append("""
            testWidgets('replacement relayouts without recreating native parent data',(tester)async{
              Future<void> pump(double extra)=>tester.pumpWidget(Directionality(textDirection:TextDirection.ltr,child:Center(
                child:CustomMultiChildLayout(delegate:refs.ProjectLayout(extra),children:[
                  LayoutId(id:'first',child:const SizedBox()),LayoutId(id:'second',child:const SizedBox())]))));
              await pump(0);final r=tester.renderObject<RenderCustomMultiChildLayoutBox>(find.byType(CustomMultiChildLayout));
              await pump(20);expect(tester.renderObject(find.byType(CustomMultiChildLayout)),same(r));expect(r.size,const Size(220,140));
              expect((r.firstChild!.parentData! as MultiChildLayoutParentData).offset,Offset(31,17+refs.signal.shift));
              expect(tester.takeException(),isNull);
            });
            testWidgets('starter empty tight zero and unbounded axes',(tester)async{
              await tester.pumpWidget(const MaterialApp(home:starter_empty.Sample()));
              final delegate=tester.widget<CustomMultiChildLayout>(find.byType(CustomMultiChildLayout)).delegate;
              for(final c in [const BoxConstraints(),const BoxConstraints.tightFor(width:0,height:0),const BoxConstraints(maxWidth:70,maxHeight:40)]){
                await tester.pumpWidget(Directionality(textDirection:TextDirection.ltr,child:Center(child:UnconstrainedBox(
                  child:ConstrainedBox(constraints:c,child:CustomMultiChildLayout(delegate:delegate))))));
                final r=tester.renderObject<RenderCustomMultiChildLayoutBox>(find.byType(CustomMultiChildLayout));
                expect(c.isSatisfiedBy(r.size),isTrue);expect(r.size.isFinite,isTrue);expect(tester.takeException(),isNull);
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
                name: custom_multi_layout_contract
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
