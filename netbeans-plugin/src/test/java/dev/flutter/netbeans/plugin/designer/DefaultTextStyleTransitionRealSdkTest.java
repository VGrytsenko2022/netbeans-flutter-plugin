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
class DefaultTextStyleTransitionRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;
    private static final StableId GROUP_ID = StableId.random(), NESTED_ID = StableId.random();
    private static final SlotName CHILDREN = new SlotName("children");

    @Test void completeGenerationStrictAnimationTypesAndLiveListenerLifecycle() throws Exception {
        initialize();
        Files.writeString(lib.resolve("references.dart"), """
            import 'package:flutter/widgets.dart';
            Animation<TextStyle> get animation => StyleValues.current;
            Animation<TextStyle> animationFactory() => StyleValues.current;
            class StyleValues {
              static Animation<TextStyle> current = const AlwaysStoppedAnimation<TextStyle>(TextStyle(fontSize:19,fontWeight:FontWeight.w700));
              static Animation<TextStyle> create() => current;
            }
            """);
        var baseline=open(group(GROUP_ID,List.of(adapter("Sibling",100))),"probe.dart","""
            // User member is preserved.
            int _userValue() => 73;
            static Animation<TextStyle> currentAnimation = const AlwaysStoppedAnimation<TextStyle>(TextStyle(fontSize:19,fontWeight:FontWeight.w700));
            static Animation<TextStyle> animationFactory() => currentAnimation;
            Animation<TextStyle> get _animation => currentAnimation;
            Animation<TextStyle> _animationFactory() => currentAnimation;
            int? get _lines => 2;
            TextStyle get _plainStyle => const TextStyle();
            Animation<TextStyle>? get _nullableAnimation => currentAnimation;
            Animation<TextStyle?> get _nullableValue => const AlwaysStoppedAnimation<TextStyle?>(null);
            Animation<Color> get _wrongValue => const AlwaysStoppedAnimation<Color>(Color(0xFF112233));
            Animation<dynamic> get _genericDynamic => const AlwaysStoppedAnimation<dynamic>('wrong');
            dynamic get _dynamic => currentAnimation;
            ValueNotifier<TextStyle> get _notAnimation => ValueNotifier<TextStyle>(const TextStyle());
            """);
        var p=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(DefaultTextStyleTransitionWidgetPropertySchema.TYPE).orElseThrow(),NESTED_ID);
        var wrapped=apply(baseline,new WrapWidget(GROUP_ID,p,new SlotName("child"),0));
        assertAnalysis(baseline,wrapped,"probe.dart",true);saveCase("stopped",wrapped);
        wrapped=reopen(wrapped,source(wrapped));
        var local=apply(wrapped,new PatchProperties(NESTED_ID,List.of(
            new PatchProperties.SetPatch(new PropertyName("styleFontSize"),new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(22))),
            new PatchProperties.SetPatch(new PropertyName("styleFontWeight"),new PropertyValue.EnumValue("FontWeight","w700")),
            new PatchProperties.SetPatch(new PropertyName("styleDecorationUnderline"),new PropertyValue.BooleanValue(true)),
            new PatchProperties.SetPatch(new PropertyName("textAlign"),new PropertyValue.EnumValue("TextAlign","end")),
            new PatchProperties.SetPatch(new PropertyName("softWrap"),new PropertyValue.BooleanValue(false)),
            new PatchProperties.SetPatch(new PropertyName("overflow"),new PropertyValue.EnumValue("TextOverflow","fade")),
            new PatchProperties.SetPatch(new PropertyName("maxLines"),ref("_lines",false)))));
        assertAnalysis(wrapped,local,"probe.dart",true);saveCase("local",local);
        assertArrayEquals(wrapped.current().dartCandidateBytes(),local.undo().session().current().dartCandidateBytes());
        var theme=apply(wrapped,new SetProperty(NESTED_ID,new PropertyName("styleThemeTextStyle"),
            new PropertyValue.ThemeTokenValue(new ThemeToken("material.textTheme.bodyLarge"))));
        assertAnalysis(wrapped,theme,"probe.dart",true);saveCase("themed",theme);
        var nullable=apply(wrapped,new PatchProperties(NESTED_ID,List.of(
            new PatchProperties.SetPatch(new PropertyName("textAlign"),new PropertyValue.NullValue()),
            new PatchProperties.SetPatch(new PropertyName("maxLines"),new PropertyValue.NullValue()))));
        assertAnalysis(wrapped,nullable,"probe.dart",true);saveCase("nullable",nullable);
        for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
            var ref=animationReference(imported,member,factory);
            var changed=apply(wrapped,new SetProperty(NESTED_ID,new PropertyName("style"),ref));
            assertAnalysis(wrapped,changed,"probe.dart",true);saveCase(referenceCase(imported,member,factory),changed);
        }
        for(String bad:List.of("_plainStyle","_nullableAnimation","_nullableValue","_wrongValue","_genericDynamic","_dynamic","_notAnimation"))
            assertAnalysis(wrapped,apply(wrapped,new SetProperty(NESTED_ID,new PropertyName("style"),ref(bad,false))),"probe.dart",false);
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("transition_test.dart"),runtime());
        run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");
    }
    private static String referenceCase(boolean imported,boolean member,boolean factory) {
        return "ref_"+(imported?"imported":"current")+"_"+(member?"member":"root")+"_"+(factory?"factory":"getter");
    }
    private static PropertyValue.DartObjectReferenceValue animationReference(boolean imported,boolean member,boolean factory) {
        String root=member?(imported?"StyleValues":"Storage"):(imported?(factory?"animationFactory":"animation"):(factory?"_animationFactory":"_animation"));
        String accessor=imported?(factory?"create":"current"):(factory?"animationFactory":"currentAnimation");
        return new PropertyValue.DartObjectReferenceValue(
            imported?Optional.of("package:default_text_style_transition_contract/references.dart"):Optional.empty(),
            root,member?Optional.of(accessor):Optional.empty(),
            factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
            factory?Optional.of(false):Optional.empty());
    }
    private static String runtime() {
        var imports=new StringBuilder("""
            import 'package:flutter/material.dart';
            import 'package:flutter_test/flutter_test.dart';
            import 'package:default_text_style_transition_contract/references.dart' as refs;
            """);
        var names=new ArrayList<>(List.of("stopped","local","themed","nullable"));
        for(boolean i:List.of(false,true))for(boolean m:List.of(false,true))for(boolean f:List.of(false,true))names.add(referenceCase(i,m,f));
        var cases=new StringBuilder("final cases=<String,Widget>{");
        for(String name:names){
            imports.append("import '../lib/").append(name).append(".dart' as case_").append(name).append(";\n");
            cases.append("'").append(name).append("':const case_").append(name).append(".Sample(),");
        }
        return imports.toString()+"""
            Widget host(Widget child,{bool rtl=false})=>MaterialApp(home:Directionality(
              textDirection:rtl?TextDirection.rtl:TextDirection.ltr,
              child:DefaultTextStyle(style:const TextStyle(fontSize:30,color:Colors.red),
                maxLines:3,textWidthBasis:TextWidthBasis.longestLine,
                textHeightBehavior:const TextHeightBehavior(applyHeightToFirstAscent:false),child:child)));
            class ProbeAnimation extends Animation<TextStyle>{
              ProbeAnimation(this._value);
              TextStyle _value;
              final listeners=<VoidCallback>{};
              @override TextStyle get value=>_value;
              void update(TextStyle value){_value=value;for(final f in List<VoidCallback>.of(listeners)){f();}}
              @override AnimationStatus get status=>AnimationStatus.forward;
              @override void addListener(VoidCallback f)=>listeners.add(f);
              @override void removeListener(VoidCallback f)=>listeners.remove(f);
              @override void addStatusListener(AnimationStatusListener listener){}
              @override void removeStatusListener(AnimationStatusListener listener){}
            }
            """+"void main(){"+cases+"};"+"""
            for(final e in cases.entries)for(final rtl in [false,true]){
              testWidgets('generated ${e.key} rtl=$rtl',(tester)async{
                await tester.pumpWidget(host(e.value,rtl:rtl));await tester.pumpAndSettle();
                final n=tester.widget<DefaultTextStyleTransition>(find.byType(DefaultTextStyleTransition));
                final v=DefaultTextStyle.of(tester.element(find.text('Sibling')));
                if(e.key=='stopped'||e.key=='nullable'){
                  expect(n.style,isA<AlwaysStoppedAnimation<TextStyle>>());expect(v.style,const TextStyle());
                }else if(e.key=='local'){
                  expect(n.style,isA<AlwaysStoppedAnimation<TextStyle>>());expect(v.style.fontSize,22);
                  expect(v.style.fontWeight,FontWeight.w700);expect(v.style.decoration,TextDecoration.underline);
                  expect(v.textAlign,TextAlign.end);expect(v.maxLines,2);expect(v.softWrap,false);expect(v.overflow,TextOverflow.fade);
                }else if(e.key=='themed'){expect(n.style.value.fontSize,isNotNull);}
                else{expect(n.style.value.fontSize,19);expect(n.style.value.fontWeight,FontWeight.w700);}
                if(e.key!='local'){
                  expect(v.textAlign,isNull);expect(v.maxLines,isNull);expect(v.softWrap,true);expect(v.overflow,TextOverflow.clip);
                }
                expect(v.textWidthBasis,TextWidthBasis.parent);expect(v.textHeightBehavior,isNull);
                expect(tester.takeException(),isNull);
              });
            }
            testWidgets('generated animation source updates and replacement detaches listeners',(tester)async{
              final first=ProbeAnimation(const TextStyle(fontSize:20)),second=ProbeAnimation(const TextStyle(fontSize:40));
              refs.StyleValues.current=first;
              await tester.pumpWidget(host(case_ref_imported_member_getter.Sample()));await tester.pumpAndSettle();
              final finder=find.byType(DefaultTextStyleTransition),state=tester.state(find.byType(DefaultTextStyleTransition));
              final child=tester.element(find.text('Sibling'));
              expect(first.listeners,hasLength(1));
              first.update(const TextStyle(fontSize:25));await tester.pump();
              expect(DefaultTextStyle.of(tester.element(find.text('Sibling'))).style.fontSize,25);
              refs.StyleValues.current=second;
              await tester.pumpWidget(host(case_ref_imported_member_getter.Sample()));
              expect(first.listeners,isEmpty);expect(second.listeners,hasLength(1));
              expect(identical(state,tester.state(finder)),true);expect(identical(child,tester.element(find.text('Sibling'))),true);
              first.update(const TextStyle(fontSize:99));await tester.pump();
              expect(DefaultTextStyle.of(tester.element(find.text('Sibling'))).style.fontSize,40);
              second.update(const TextStyle(fontSize:45));await tester.pump();
              expect(DefaultTextStyle.of(tester.element(find.text('Sibling'))).style.fontSize,45);
              await tester.pumpWidget(const SizedBox());expect(second.listeners,isEmpty);expect(tester.takeException(),isNull);
            });
            testWidgets('generated AnimationController TextStyleTween drives intermediate frames',(tester)async{
              final controller=AnimationController(vsync:tester,duration:const Duration(milliseconds:100));
              refs.StyleValues.current=TextStyleTween(begin:const TextStyle(fontSize:20,color:Colors.blue),
                end:const TextStyle(fontSize:40,color:Colors.red)).animate(controller);
              await tester.pumpWidget(host(case_ref_imported_member_getter.Sample()));
              final state=tester.state(find.byType(DefaultTextStyleTransition)),child=tester.element(find.text('Sibling'));
              controller.forward();await tester.pump();
              for(final fraction in [.25,.5,.75,1.0]){
                await tester.pump(const Duration(milliseconds:25));
                final v=DefaultTextStyle.of(tester.element(find.text('Sibling'))).style;
                expect(v.fontSize,closeTo(20+20*fraction,.01));
                final expected=Color.lerp(Colors.blue,Colors.red,fraction)!;
                expect(v.color!.r,closeTo(expected.r,1e-8));expect(v.color!.g,closeTo(expected.g,1e-8));
                expect(v.color!.b,closeTo(expected.b,1e-8));expect(v.color!.a,closeTo(expected.a,1e-8));
              }
              expect(identical(state,tester.state(find.byType(DefaultTextStyleTransition))),true);
              expect(identical(child,tester.element(find.text('Sibling'))),true);
              await tester.pumpWidget(const SizedBox());controller.dispose();expect(tester.takeException(),isNull);
            });
            }
            """;
    }
    private static PropertyValue.DartObjectReferenceValue ref(String name,boolean factory){
        return new PropertyValue.DartObjectReferenceValue(Optional.empty(),name,Optional.empty(),
            factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
            factory?Optional.of(false):Optional.empty());
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
    private static WidgetNode root(WidgetNode group) { return group; }
    private void initialize() throws Exception {
        sdk = Path.of(System.getProperty("flutter.events.sdk")).toRealPath();
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        flutter = sdk.resolve(windows ? "bin/flutter.bat" : "bin/flutter");
        dart = sdk.resolve(windows ? "bin/cache/dart-sdk/bin/dart.exe" : "bin/cache/dart-sdk/bin/dart");
        Files.writeString(project.resolve("pubspec.yaml"), """
                name: default_text_style_transition_contract
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
