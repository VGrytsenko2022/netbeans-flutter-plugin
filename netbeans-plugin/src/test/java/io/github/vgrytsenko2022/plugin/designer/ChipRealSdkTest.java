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
class ChipRealSdkTest {
    @TempDir(cleanup=CleanupMode.ON_SUCCESS) Path project;
    private Path sdk,flutter,dart,lib;
    private final List<String> cases=new ArrayList<>();
    @Test void allArgumentsLocalFamiliesStrictSourcesAndNativeLifecycle()throws Exception {
        initialize();
        var expressions=new LinkedHashMap<String,String>();
        expressions.put("Key?","const ValueKey('chip')");
        expressions.put("TextStyle?","const TextStyle(fontSize:18)");
        expressions.put("EdgeInsetsGeometry?","const EdgeInsetsDirectional.fromSTEB(2,3,4,5)");
        expressions.put("VoidCallback?","deleted");
        expressions.put("Color?","const Color(0xffabcdef)");
        expressions.put("String?","'Remove item'");
        expressions.put("BorderSide?","const BorderSide(width:2)");
        expressions.put("OutlinedBorder?","const StadiumBorder()");
        expressions.put("FocusNode?","null");
        expressions.put("WidgetStateProperty<Color?>?","const WidgetStatePropertyAll<Color?>(Color(0xffabcdef))");
        expressions.put("VisualDensity?","VisualDensity.compact");
        expressions.put("double?","2.0");
        expressions.put("IconThemeData?","const IconThemeData(size:18)");
        expressions.put("BoxConstraints?","const BoxConstraints.tightFor(width:24,height:24)");
        expressions.put("ChipAnimationStyle?","ChipAnimationStyle(enableAnimation:AnimationStyle.noAnimation)");
        expressions.put("MouseCursor?","SystemMouseCursors.click");
        expressions.put("MouseCursor","SystemMouseCursors.basic");
        expressions.put("AnimationStyle?","AnimationStyle.noAnimation");
        expressions.put("Duration?","const Duration(milliseconds:123)");
        expressions.put("Curve?","Curves.easeIn");
        var members=new StringBuilder("""
            // User member is preserved.
            int _userValue() => 73;
            int deletions=0;
            void deleted(){deletions++;}
            Object get wrong => Object();
            dynamic get unsafe => null;
            ShapeBorder get generalShape => const StadiumBorder();
            void wrongCallback(int v){}
            """);
        var getters=new LinkedHashMap<String,String>();int index=0;
        for(var e:expressions.entrySet()){
            String getter="value"+index++;getters.put(e.getKey(),getter);
            members.append(e.getKey()).append(" get ").append(getter).append(" => ").append(e.getValue()).append(";\n");
        }
        var catalog=BuiltInWidgetCatalog.getDefault();
        var chip=WidgetNodePrototypeFactory.create(catalog.find(ChipWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());
        var seed=open(chip,"probe.dart",members.toString());save("defaults",seed);
        // Three disjoint source families cover every admitted reference field without whole/local conflicts.
        var byType=new LinkedHashMap<String,String>();int referenceFields=0;
        for(int group=0;group<3;group++){
            var before=reopen(seed,source(seed));var current=before;
            for(var p:ChipWidgetPropertySchema.properties())for(var c:p.constraints())if(c instanceof PropertyValueConstraint.DartObjectReferenceValues r){
                String name=p.name().value();
                int fieldGroup=ChipWidgetPropertySchema.DIRECT.contains(name)?0:
                    name.endsWith("Us")||name.endsWith("Curve")?2:1;
                if(fieldGroup!=group)continue;
                current=apply(current,new SetProperty(chip.id(),p.name(),ref(getters.get(r.expectedDartType()))));
                byType.putIfAbsent(r.expectedDartType(),name);referenceFields++;
            }
            assertAnalysis(before,current,"probe.dart",true);save("sources"+group,current);
        }
        assertEquals(50,referenceFields);
        for(String type:List.of("VisualDensity?","ChipAnimationStyle?")){
            var before=reopen(seed,source(seed));
            var current=apply(before,new SetProperty(chip.id(),new PropertyName(byType.get(type)),ref(getters.get(type))));
            assertAnalysis(before,current,"probe.dart",true);
        }
        int rejected=0;
        for(var e:byType.entrySet())for(String bad:List.of("wrong","unsafe")){
            var before=reopen(seed,source(seed));var current=before;
            if(e.getValue().startsWith("mouseCursor")&&!e.getValue().equals("mouseCursor")&&!e.getValue().equals("mouseCursorDefault"))
                current=apply(current,new SetProperty(chip.id(),new PropertyName("mouseCursorDefault"),s("basic")));
            current=apply(current,new SetProperty(chip.id(),new PropertyName(e.getValue()),ref(bad)));
            assertAnalysis(before,current,"probe.dart",false);rejected++;
        }
        for(var e:Map.of("shape","generalShape","onDeleted","wrongCallback").entrySet()){
            var before=reopen(seed,source(seed));var current=apply(before,new SetProperty(chip.id(),new PropertyName(e.getKey()),ref(e.getValue())));
            assertAnalysis(before,current,"probe.dart",false);rejected++;
        }
        // Nullable native arguments must remain explicit null, without materializing a local style.
        var before=reopen(seed,source(seed));var current=before;
        for(var p:ChipWidgetPropertySchema.properties())if(ChipWidgetPropertySchema.DIRECT.contains(p.name().value())&&p.acceptedKinds().contains(PropertyValueKind.NULL))
            current=apply(current,new SetProperty(chip.id(),p.name(),new PropertyValue.NullValue()));
        assertAnalysis(before,current,"probe.dart",true);save("nullable",current);
        for(String shape:CardWidgetPropertySchema.shapeKinds()){
            before=reopen(seed,source(seed));current=apply(before,new SetProperty(chip.id(),new PropertyName("shapeKind"),s(shape)));
            assertAnalysis(before,current,"probe.dart",true);save("shape_"+shape,current);
        }
        before=reopen(seed,source(seed));current=before;
        var local=new LinkedHashMap<String,PropertyValue>();
        local.put("key",s("local"));local.put("labelPadding",new PropertyValue.EdgeInsetsValue(java.math.BigDecimal.ONE,java.math.BigDecimal.ONE,java.math.BigDecimal.ONE,java.math.BigDecimal.ONE));
        local.put("padding",local.get("labelPadding"));local.put("onDeleted",ref("deleted"));
        local.put("deleteButtonTooltipMessage",s("Remove"));local.put("elevation",n(2));
        local.put("clipBehavior",new PropertyValue.EnumValue("Clip","antiAlias"));
        local.put("materialTapTargetSize",new PropertyValue.EnumValue("MaterialTapTargetSize","shrinkWrap"));
        local.put("autofocus",new PropertyValue.BooleanValue(false));
        for(String name:List.of("deleteIconColor","backgroundColor","shadowColor","surfaceTintColor","colorDefault","colorHovered","sideColor","iconThemeColor"))
            local.put(name,new PropertyValue.ColorValue(0xffabcdefL));
        local.put("sideStateful",new PropertyValue.BooleanValue(true));local.put("sideWidth",n(2));local.put("sideHoveredMode",s("inherit"));
        local.put("mouseCursorDefault",s("basic"));local.put("mouseCursorHovered",s("click"));
        local.put("visualDensityHorizontal",n(-2));local.put("visualDensityVertical",n(-1));
        for(String name:List.of("Size","Fill","Weight","Grade","OpticalSize","Opacity"))local.put("iconTheme"+name,n(name.equals("Fill")?1:name.equals("Opacity")?2:20));
        local.put("iconThemeApplyTextScaling",new PropertyValue.BooleanValue(true));
        local.put("iconThemeShadows",new PropertyValue.ShadowListValue(List.of()));
        for(String part:ChipWidgetPropertySchema.ANIMATIONS)for(String name:ChipWidgetPropertySchema.animationLeaves(ChipWidgetPropertySchema.animationPrefix(part)))
            local.put(name,name.endsWith("Us")?new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(123000)):s("easeIn"));
        for(var e:local.entrySet())current=apply(current,new SetProperty(chip.id(),new PropertyName(e.getKey()),e.getValue()));
        for(String slot:List.of("avatar","deleteIcon")){
            var text=new WidgetNode(StableId.random(),TextWidgetPropertySchema.TEXT_TYPE,Map.of(new PropertyName("data"),s(slot)),Map.of());
            current=apply(current,new AddWidget(new WidgetPlacement(chip.id(),new SlotName(slot),0),text));
        }
        assertAnalysis(before,current,"probe.dart",true);save("locals",current);
        before=reopen(seed,source(seed));current=before;
        for(String name:ChipWidgetPropertySchema.textLeaves()){
            if(Set.of("labelStyleForeground","labelStyleBackground","labelStyleFontFamily","labelStyleLocaleLanguageCode").contains(name))continue;
            current=apply(current,new SetProperty(chip.id(),new PropertyName(name),io.github.vgrytsenko2022.plugin.designer.properties.BadgePropertyContractTest.value("textStyle"+name.substring("labelStyle".length()))));
        }
        current=apply(current,new SetProperty(chip.id(),new PropertyName("labelStyleFontFamily"),s("Inter")));
        current=apply(current,new SetProperty(chip.id(),new PropertyName("labelStyleLocaleLanguageCode"),s("uk")));
        assertAnalysis(before,current,"probe.dart",true);save("style",current);
        before=reopen(current,source(current));current=before;
        for(String part:List.of("Foreground","Background"))current=apply(current,new PatchProperties(chip.id(),List.of(
            new PatchProperties.ResetPatch(new PropertyName(part.equals("Foreground")?"labelStyleColor":"labelStyleBackgroundColor")),
            new PatchProperties.SetPatch(new PropertyName("labelStyle"+part),io.github.vgrytsenko2022.plugin.designer.properties.BadgePropertyContractTest.value("textStyle"+part)))));
        assertAnalysis(before,current,"probe.dart",true);save("paints",current);
        before=reopen(seed,source(seed));current=apply(before,new CreateEventHandler(chip.id(),new PropertyName("onDeleted"),"_delete"));
        assertAnalysis(before,current,"probe.dart",true);save("events",current);
        Files.writeString(project.resolve("native_test.dart"),nativeTests());
        run(List.of(flutter.toString(),"test","native_test.dart","--reporter","expanded"),"native.log");
        System.out.println("Chip: "+(cases.size()*24+4)+" generated/native cases; "+referenceFields+" typed fields; "+rejected+" unsafe source cases rejected.");
    }
    private static PropertyValue.StringValue s(String v){return new PropertyValue.StringValue(v);}
    private static PropertyValue.DoubleValue n(double v){return new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(v));}
    private static PropertyValue.DartObjectReferenceValue ref(String name){return new PropertyValue.DartObjectReferenceValue(Optional.empty(),name,Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());}
    private void save(String name,DesignerCommandSession s)throws Exception{saveCase(name,s);cases.add(name);}
    private String nativeTests(){
        var out=new StringBuilder("import 'package:flutter/material.dart';\nimport 'package:flutter_test/flutter_test.dart';\n");
        for(String name:cases)out.append("import 'package:dialog_contract/").append(name).append(".dart' as ").append(name).append(";\n");
        out.append("void main(){\n");
        for(String name:cases)for(String platform:List.of("android","fuchsia","iOS","linux","macOS","windows"))
            for(boolean rtl:List.of(false,true))for(boolean m3:List.of(false,true))
            out.append("""
                testWidgets('NAME PLATFORM RTL M3',(t)async{
                  await t.pumpWidget(MaterialApp(theme:ThemeData(useMaterial3:M3,platform:TargetPlatform.PLATFORM),
                    home:Directionality(textDirection:DIR,child:Scaffold(body:NAME.Sample()))));
                  await t.pumpAndSettle();expect(find.byType(Chip),findsOneWidget);expect(t.takeException(),isNull);
                  await t.pumpWidget(const SizedBox());await t.pump();expect(t.takeException(),isNull);
                });
                """.replace("NAME",name).replace("PLATFORM",platform).replace("RTL",Boolean.toString(rtl)).replace("DIR",rtl?"TextDirection.rtl":"TextDirection.ltr").replace("M3",Boolean.toString(m3)));
        out.append("""
            testWidgets('delete is a request not removal and body never invokes it',(t)async{
              await t.pumpWidget(const MaterialApp(home:Scaffold(body:sources0.Sample())));
              final state=t.state<sources0.Storage>(find.byType(sources0.Sample));
              await t.tap(find.text('Chip'));await t.pumpAndSettle();expect(state.deletions,0);
              await t.tap(find.byTooltip('Remove item'));await t.pumpAndSettle();
              expect(state.deletions,1);expect(find.byType(Chip),findsOneWidget);
            });
            testWidgets('delete visibility and localized or suppressed tooltip',(t)async{
              Widget view(VoidCallback? fn,String? tip)=>MaterialApp(home:Scaffold(body:Chip(label:const Text('Label'),
                deleteIcon:const Text('Delete'),onDeleted:fn,deleteButtonTooltipMessage:tip)));
              await t.pumpWidget(view(null,null));expect(find.text('Delete'),findsNothing);
              await t.pumpWidget(view((){},null));expect(find.byTooltip('Delete'),findsOneWidget);
              await t.pumpWidget(view((){},''));expect(find.text('Delete'),findsOneWidget);
              expect(t.widget<Tooltip>(find.byType(Tooltip)).message,'');
              expect(t.state<TooltipState>(find.byType(Tooltip)).ensureTooltipVisible(),false);
              expect(t.takeException(),isNull);
            });
            testWidgets('animation durations initialize once and new key applies changes',(t)async{
              ChipAnimationStyle style(int v)=>ChipAnimationStyle(
                enableAnimation:AnimationStyle(duration:Duration(milliseconds:v),reverseDuration:Duration(milliseconds:v+1),curve:Curves.linear),
                selectAnimation:AnimationStyle(duration:Duration(milliseconds:v+2),curve:Curves.linear),
                avatarDrawerAnimation:AnimationStyle(duration:Duration(milliseconds:v+3),curve:Curves.linear),
                deleteDrawerAnimation:AnimationStyle(duration:Duration(milliseconds:v+4),curve:Curves.linear));
              Widget view(String key,int v)=>MaterialApp(home:Scaffold(body:Chip(key:ValueKey(key),label:const Text('Chip'),chipAnimationStyle:style(v))));
              await t.pumpWidget(view('one',100));dynamic state=t.state(find.byType(RawChip));
              expect(state.enableController.duration,const Duration(milliseconds:100));
              expect(state.enableController.reverseDuration,const Duration(milliseconds:101));
              expect(state.selectController.duration,const Duration(milliseconds:102));
              expect(state.avatarDrawerController.duration,const Duration(milliseconds:103));
              expect(state.deleteDrawerController.duration,const Duration(milliseconds:104));
              expect((state.enableAnimation as CurvedAnimation).curve,Curves.fastOutSlowIn);
              await t.pumpWidget(view('one',200));expect(state.enableController.duration,const Duration(milliseconds:100));
              await t.pumpWidget(view('two',200));state=t.state(find.byType(RawChip));
              expect(state.enableController.duration,const Duration(milliseconds:200));expect(t.takeException(),isNull);
            });
            testWidgets('local families reach native Chip unchanged',(t)async{
              await t.pumpWidget(const MaterialApp(home:Scaffold(body:locals.Sample())));
              final chip=t.widget<Chip>(find.byType(Chip));
              expect(chip.visualDensity,const VisualDensity(horizontal:-2,vertical:-1));
              expect(chip.iconTheme!.opacity,1);expect(chip.iconTheme!.shadows,isEmpty);
              expect(chip.chipAnimationStyle!.enableAnimation!.duration,const Duration(milliseconds:123));
              expect(chip.chipAnimationStyle!.enableAnimation!.curve,Curves.easeIn);
              expect((chip.side as WidgetStateBorderSide).resolve({WidgetState.hovered}),isNull);
              expect((chip.mouseCursor as WidgetStateMouseCursor).resolve({WidgetState.hovered}),SystemMouseCursors.click);
              expect(find.text('avatar'),findsOneWidget);expect(find.text('deleteIcon'),findsOneWidget);expect(t.takeException(),isNull);
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
