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
class MaterialBannerRealSdkTest {
    @TempDir(cleanup=CleanupMode.ON_SUCCESS) Path project;
    private Path sdk,flutter,dart,lib;
    private final List<String> cases=new ArrayList<>();
    @Test void allArgumentsStylesTypedSourcesAndNativeMessengerLifecycle()throws Exception {
        initialize();
        String members="""
            // User member is preserved.
            int _userValue() => 73;
            int visible=0,presses=0;
            Key? get keyValue => const ValueKey('banner');
            Color? get colorValue => const Color(0xffabcdef);
            double? get elevationValue => 3;
            double get heightValue => 60;
            double? get nullableHeight => null;
            EdgeInsetsGeometry? get insetsValue => const EdgeInsetsDirectional.fromSTEB(4,6,8,10);
            TextStyle? get styleValue => const TextStyle(fontSize:18);
            Animation<double>? get animationValue => const AlwaysStoppedAnimation<double>(1);
            VoidCallback? callbackFactory() => shown;
            TextStyle? styleFactory() => styleValue;
            void shown(){visible++;}
            void pressed(){presses++;}
            void wrongCallback(int v){}
            Object get wrong => Object();
            dynamic get unsafe => null;
            """;
        var catalog=BuiltInWidgetCatalog.getDefault();
        var banner=WidgetNodePrototypeFactory.create(catalog.find(MaterialBannerWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());
        var action=((WidgetSlot.ListSlot)banner.slots().get(new SlotName("actions"))).children().getFirst();
        var seed=open(banner,"probe.dart",members);save("defaults",seed);
        var fields=new LinkedHashMap<String,String>();
        fields.put("key","keyValue");fields.put("contentTextStyle","styleValue");fields.put("elevation","elevationValue");
        for(String n:List.of("backgroundColor","surfaceTintColor","shadowColor","dividerColor"))fields.put(n,"colorValue");
        for(String n:List.of("padding","margin","leadingPadding"))fields.put(n,"insetsValue");
        fields.put("minActionBarHeight","heightValue");fields.put("animation","animationValue");fields.put("onVisible","shown");
        var before=reopen(seed,source(seed));var current=before;
        for(var e:fields.entrySet())current=apply(current,new SetProperty(banner.id(),new PropertyName(e.getKey()),ref(e.getValue())));
        current=apply(current,new SetProperty(action.id(),new PropertyName("onPressed"),ref("pressed")));
        assertAnalysis(before,current,"probe.dart",true);save("sources",current);
        int rejected=0;
        for(String name:fields.keySet())for(String bad:List.of("wrong","unsafe")){
            before=reopen(seed,source(seed));current=apply(before,new SetProperty(banner.id(),new PropertyName(name),ref(bad)));
            assertAnalysis(before,current,"probe.dart",false);rejected++;
        }
        for(String name:List.of("onVisible","minActionBarHeight")){
            before=reopen(seed,source(seed));current=apply(before,new SetProperty(banner.id(),new PropertyName(name),ref(name.equals("onVisible")?"wrongCallback":"nullableHeight")));
            assertAnalysis(before,current,"probe.dart",false);rejected++;
        }
        for(String name:List.of("onVisible","contentTextStyle")){
            before=reopen(seed,source(seed));current=apply(before,new SetProperty(banner.id(),new PropertyName(name),
                new PropertyValue.DartObjectReferenceValue(Optional.empty(),name.equals("onVisible")?"callbackFactory":"styleFactory",Optional.empty(),
                    PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION,Optional.of(false))));
            assertAnalysis(before,current,"probe.dart",true);save("factory_"+name,current);
        }
        before=reopen(seed,source(seed));current=before;
        for(var p:MaterialBannerWidgetPropertySchema.properties())if(p.acceptedKinds().contains(PropertyValueKind.NULL))
            current=apply(current,new SetProperty(banner.id(),p.name(),new PropertyValue.NullValue()));
        assertAnalysis(before,current,"probe.dart",true);save("nullable",current);
        before=reopen(seed,source(seed));current=before;
        var values=new LinkedHashMap<String,PropertyValue>();
        values.put("key",new PropertyValue.StringValue("local"));
        for(String n:List.of("backgroundColor","surfaceTintColor","shadowColor","dividerColor"))values.put(n,new PropertyValue.ColorValue(0xffabcdefL));
        for(String n:List.of("padding","margin","leadingPadding"))values.put(n,new PropertyValue.EdgeInsetsDirectionalValue(java.math.BigDecimal.valueOf(4),java.math.BigDecimal.valueOf(6),java.math.BigDecimal.valueOf(8),java.math.BigDecimal.valueOf(10)));
        values.put("elevation",new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(3)));
        values.put("minActionBarHeight",new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(60)));
        values.put("forceActionsBelow",new PropertyValue.BooleanValue(true));
        values.put("overflowAlignment",new PropertyValue.EnumValue("OverflowBarAlignment","start"));
        values.put("animation",new PropertyValue.DoubleValue(java.math.BigDecimal.ONE));
        for(var e:values.entrySet())current=apply(current,new SetProperty(banner.id(),new PropertyName(e.getKey()),e.getValue()));
        var leading=new WidgetNode(StableId.random(),TextWidgetPropertySchema.TEXT_TYPE,Map.of(new PropertyName("data"),new PropertyValue.StringValue("Lead")),Map.of());
        current=apply(current,new AddWidget(new WidgetPlacement(banner.id(),new SlotName("leading"),0),leading));
        var second=WidgetNodePrototypeFactory.create(catalog.find(action.type()).orElseThrow(),StableId.random());
        second=new WidgetNode(second.id(),second.type(),second.properties(),Map.of(new SlotName("child"),WidgetSlot.SingleSlot.of(
                new WidgetNode(StableId.random(),TextWidgetPropertySchema.TEXT_TYPE,Map.of(new PropertyName("data"),new PropertyValue.StringValue("Second")),Map.of()))));
        current=apply(current,new AddWidget(new WidgetPlacement(banner.id(),new SlotName("actions"),1),second));
        assertAnalysis(before,current,"probe.dart",true);save("locals",current);
        for(String alignment:List.of("start","center","end")){
            before=reopen(seed,source(seed));current=apply(before,new SetProperty(banner.id(),new PropertyName("overflowAlignment"),new PropertyValue.EnumValue("OverflowBarAlignment",alignment)));
            current=apply(current,new SetProperty(banner.id(),new PropertyName("forceActionsBelow"),new PropertyValue.BooleanValue(true)));
            assertAnalysis(before,current,"probe.dart",true);save("alignment_"+alignment,current);
        }
        before=reopen(seed,source(seed));current=before;
        current=apply(current,new SetProperty(banner.id(),new PropertyName("contentTextStyleFontFamily"),new PropertyValue.StringValue("Inter")));
        current=apply(current,new SetProperty(banner.id(),new PropertyName("contentTextStyleLocaleLanguageCode"),new PropertyValue.StringValue("uk")));
        for(String name:AlertDialogWidgetPropertySchema.localStyleProperties("contentTextStyle")){
            if(Set.of("contentTextStyleForeground","contentTextStyleBackground","contentTextStyleFontFamily","contentTextStyleLocaleLanguageCode").contains(name))continue;
            current=apply(current,new SetProperty(banner.id(),new PropertyName(name),
                io.github.vgrytsenko2022.plugin.designer.properties.BadgePropertyContractTest.value("textStyle"+name.substring("contentTextStyle".length()))));
        }
        assertAnalysis(before,current,"probe.dart",true);save("style",current);
        before=reopen(current,source(current));current=before;
        for(String part:List.of("Foreground","Background")){
            current=apply(current,new PatchProperties(banner.id(),List.of(
                new PatchProperties.ResetPatch(new PropertyName(part.equals("Foreground")?"contentTextStyleColor":"contentTextStyleBackgroundColor")),
                new PatchProperties.SetPatch(new PropertyName("contentTextStyle"+part),io.github.vgrytsenko2022.plugin.designer.properties.BadgePropertyContractTest.value("textStyle"+part)))));
        }
        assertAnalysis(before,current,"probe.dart",true);save("paints",current);
        before=reopen(seed,source(seed));current=apply(before,new CreateEventHandler(banner.id(),new PropertyName("onVisible"),"_shown"));
        current=apply(current,new CreateEventHandler(action.id(),new PropertyName("onPressed"),"_pressed"));
        assertAnalysis(before,current,"probe.dart",true);save("events",current);
        Files.writeString(project.resolve("native_test.dart"),nativeTests());
        run(List.of(flutter.toString(),"test","native_test.dart","--reporter","expanded"),"native.log");
        System.out.println("MaterialBanner: "+(cases.size()*24+6)+" generated/native cases; "+rejected+" unsafe source cases rejected.");
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
                testWidgets('NAME PLATFORM RTL M3',(tester)async{
                  await tester.pumpWidget(MaterialApp(theme:ThemeData(useMaterial3:M3,platform:TargetPlatform.PLATFORM),
                    home:Directionality(textDirection:DIR,child:Scaffold(body:NAME.Sample()))));
                  await tester.pumpAndSettle();expect(find.byType(MaterialBanner),findsOneWidget);
                  expect(tester.takeException(),isNull);
                  await tester.pumpWidget(const SizedBox());await tester.pump();expect(tester.takeException(),isNull);
                });
                """.replace("NAME",name).replace("PLATFORM",platform).replace("RTL",Boolean.toString(rtl)).replace("DIR",rtl?"TextDirection.rtl":"TextDirection.ltr").replace("M3",Boolean.toString(m3)));
        out.append("""
            testWidgets('static banner never reports visibility and does not time out',(t)async{
              int visible=0;
              await t.pumpWidget(MaterialApp(home:Scaffold(body:MaterialBanner(content:const Text('Static'),
                onVisible:()=>visible++,actions:[TextButton(onPressed:(){},child:const Text('Action'))]))));
              await t.pump(const Duration(minutes:1));expect(visible,0);expect(find.byType(MaterialBanner),findsOneWidget);
            });
            testWidgets('native animation reports first completion only and swaps safely',(t)async{
              int visible=0;final controller=AnimationController(vsync:const TestVSync(),duration:const Duration(milliseconds:100));
              Widget view(Animation<double>? a)=>MaterialApp(home:Scaffold(body:MaterialBanner(content:const Text('Animated'),
                animation:a,onVisible:()=>visible++,actions:[TextButton(onPressed:(){},child:const Text('Action'))])));
              await t.pumpWidget(view(controller));controller.forward();await t.pumpAndSettle();expect(visible,1);
              controller.reverse();await t.pumpAndSettle();controller.forward();await t.pumpAndSettle();expect(visible,1);
              await t.pumpWidget(view(null));await t.pumpAndSettle();expect(find.text('Animated'),findsOneWidget);
              await t.pumpWidget(const SizedBox());controller.dispose();expect(t.takeException(),isNull);
            });
            testWidgets('messenger replaces animation and user action does not auto dismiss',(t)async{
              await t.pumpWidget(const MaterialApp(home:Scaffold(body:sources.Sample())));
              final state=t.state<sources.Storage>(find.byType(sources.Sample));
              final seed=t.widget<MaterialBanner>(find.byType(MaterialBanner));expect(state.visible,0);
              final messenger=GlobalKey<ScaffoldMessengerState>();
              await t.pumpWidget(MaterialApp(scaffoldMessengerKey:messenger,home:const Scaffold()));
              final shown=messenger.currentState!.showMaterialBanner(seed);
              MaterialBannerClosedReason? reason;shown.closed.then((v)=>reason=v);await t.pumpAndSettle();
              expect(t.widget<MaterialBanner>(find.byType(MaterialBanner)).animation,isNot(same(seed.animation)));
              expect(state.visible,1);await t.tap(find.text('Action'));await t.pumpAndSettle();
              expect(state.presses,1);expect(find.byType(MaterialBanner),findsOneWidget);expect(reason,isNull);
              shown.close();await t.pumpAndSettle();expect(reason,MaterialBannerClosedReason.hide);expect(t.takeException(),isNull);
            });
            for(final reason in [MaterialBannerClosedReason.remove,MaterialBannerClosedReason.dismiss]){
              testWidgets('native explicit close reason $reason',(t)async{
                final messenger=GlobalKey<ScaffoldMessengerState>();
                await t.pumpWidget(MaterialApp(scaffoldMessengerKey:messenger,home:const Scaffold()));
                final shown=messenger.currentState!.showMaterialBanner(MaterialBanner(content:const Text('Close'),
                  actions:[TextButton(onPressed:(){},child:const Text('Action'))]));
                MaterialBannerClosedReason? actual;shown.closed.then((v)=>actual=v);await t.pumpAndSettle();
                if(reason==MaterialBannerClosedReason.dismiss){
                  final semantics=t.widgetList<Semantics>(find.descendant(of:find.byType(MaterialBanner),matching:find.byType(Semantics))).firstWhere((s)=>s.properties.onDismiss!=null);
                  semantics.properties.onDismiss!();
                }else{messenger.currentState!.removeCurrentMaterialBanner();}
                await t.pumpAndSettle();expect(actual,reason);expect(find.byType(MaterialBanner),findsNothing);expect(t.takeException(),isNull);
              });
            }
            testWidgets('messenger queues banners and close exposes the next one',(t)async{
              final messenger=GlobalKey<ScaffoldMessengerState>();
              await t.pumpWidget(MaterialApp(scaffoldMessengerKey:messenger,home:const Scaffold()));
              MaterialBanner bar(String label)=>MaterialBanner(content:Text(label),actions:[TextButton(onPressed:(){},child:const Text('Action'))]);
              final first=messenger.currentState!.showMaterialBanner(bar('First'));
              messenger.currentState!.showMaterialBanner(bar('Second'));await t.pumpAndSettle();
              expect(find.text('First'),findsOneWidget);expect(find.text('Second'),findsNothing);
              first.close();await t.pumpAndSettle();expect(find.text('Second'),findsOneWidget);
              messenger.currentState!.removeCurrentMaterialBanner();await t.pumpAndSettle();expect(t.takeException(),isNull);
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
