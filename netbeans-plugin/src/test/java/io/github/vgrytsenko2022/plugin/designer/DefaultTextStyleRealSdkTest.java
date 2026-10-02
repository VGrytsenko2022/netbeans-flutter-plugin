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
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
class DefaultTextStyleRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;
    private static final StableId GROUP_ID = StableId.random(), NESTED_ID = StableId.random();
    private static final SlotName CHILDREN = new SlotName("children");

    @Test void bothFactoriesStrictTypesNullableInheritanceSaveReopenAndNative() throws Exception {
        initialize();
        Files.writeString(lib.resolve("references.dart"), """
            import 'package:flutter/widgets.dart';
            class StyleValues {
              static TextStyle get style => const TextStyle(fontSize: 19, fontWeight: FontWeight.w700);
              static TextStyle? nullableStyle() => null;
              static TextHeightBehavior? height() => const TextHeightBehavior(applyHeightToFirstAscent: false);
              static int? get lines => 2;
            }
            """);
        var baseline=open(group(GROUP_ID,List.of(adapter("Sibling",100))),"probe.dart","""
            // User member is preserved.
            int _userValue() => 73;
            TextStyle get _style => const TextStyle(fontSize: 19, fontWeight: FontWeight.w700);
            TextStyle? _nullableStyle() => null;
            TextHeightBehavior? get _height => const TextHeightBehavior(applyHeightToFirstAscent: false);
            int? get _lines => 2;
            String get _bad => 'wrong';
            dynamic get _dynamic => const TextStyle();
            """);
        for(boolean merge:List.of(false,true)){
            String prefix=merge?"merge_":"direct_";
            WidgetTypeId type=merge?DefaultTextStyleWidgetPropertySchema.MERGE_TYPE:DefaultTextStyleWidgetPropertySchema.TYPE;
            var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(type).orElseThrow(),NESTED_ID);
            var wrapped=apply(baseline,new WrapWidget(GROUP_ID,prototype,new SlotName("child"),0));
            assertAnalysis(baseline,wrapped,"probe.dart",true);saveCase(prefix+"default",wrapped);
            wrapped=reopen(wrapped,source(wrapped));
            var typed=apply(wrapped,new PatchProperties(NESTED_ID,List.of(
                new PatchProperties.SetPatch(new PropertyName("style"),ref("_style",false)),
                new PatchProperties.SetPatch(new PropertyName("textHeightBehavior"),ref("_height",false)),
                new PatchProperties.SetPatch(new PropertyName("maxLines"),ref("_lines",false)))));
            assertAnalysis(wrapped,typed,"probe.dart",true);saveCase(prefix+"typed",typed);
            typed=reopen(typed,source(typed));
            var imported=apply(wrapped,new PatchProperties(NESTED_ID,List.of(
                new PatchProperties.SetPatch(new PropertyName("style"),importedRef("style",false)),
                new PatchProperties.SetPatch(new PropertyName("textHeightBehavior"),importedRef("height",true)),
                new PatchProperties.SetPatch(new PropertyName("maxLines"),importedRef("lines",false)))));
            assertAnalysis(wrapped,imported,"probe.dart",true);saveCase(prefix+"imported",imported);
            var patches=new ArrayList<PatchProperties.Patch>();
            for(String field:merge?List.of("style","textAlign","softWrap","overflow","maxLines","textWidthBasis","textHeightBehavior")
                    :List.of("textAlign","maxLines","textHeightBehavior")){
                patches.add(new PatchProperties.SetPatch(new PropertyName(field),new PropertyValue.NullValue()));
            }
            var nullable=apply(wrapped,new PatchProperties(NESTED_ID,patches));
            assertAnalysis(wrapped,nullable,"probe.dart",true);saveCase(prefix+"null",nullable);
            assertArrayEquals(wrapped.current().dartCandidateBytes(),nullable.undo().session().current().dartCandidateBytes());
            var local=apply(wrapped,new PatchProperties(NESTED_ID,List.of(
                new PatchProperties.SetPatch(new PropertyName("style"),new PropertyValue.StringValue("local")),
                new PatchProperties.SetPatch(new PropertyName("styleFontSize"),new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(22))),
                new PatchProperties.SetPatch(new PropertyName("styleFontWeight"),new PropertyValue.EnumValue("FontWeight","w700")),
                new PatchProperties.SetPatch(new PropertyName("styleDecorationUnderline"),new PropertyValue.BooleanValue(true)),
                new PatchProperties.SetPatch(new PropertyName("textHeightApplyFirstAscent"),new PropertyValue.BooleanValue(false)))));
            assertAnalysis(wrapped,local,"probe.dart",true);saveCase(prefix+"local",local);
            local=reopen(local,source(local));
            var replaced=apply(local,new SetProperty(NESTED_ID,new PropertyName("styleInherit"),new PropertyValue.BooleanValue(false)));
            assertAnalysis(local,replaced,"probe.dart",true);saveCase(prefix+"replace",replaced);
            var themed=apply(wrapped,new PatchProperties(NESTED_ID,List.of(
                new PatchProperties.SetPatch(new PropertyName("style"),new PropertyValue.StringValue("local")),
                new PatchProperties.SetPatch(new PropertyName("styleThemeTextStyle"),new PropertyValue.ThemeTokenValue(new ThemeToken("material.textTheme.bodyLarge"))))));
            assertAnalysis(wrapped,themed,"probe.dart",true);saveCase(prefix+"theme",themed);
            for(String field:List.of("style","textHeightBehavior","maxLines"))
                assertAnalysis(typed,apply(typed,new SetProperty(NESTED_ID,new PropertyName(field),ref("_bad",false))),"probe.dart",false);
            assertAnalysis(typed,apply(typed,new SetProperty(NESTED_ID,new PropertyName("style"),ref("_dynamic",false))),"probe.dart",false);
            for(var ref:List.of(ref("_nullableStyle",true),importedRef("nullableStyle",true))){
                var nullableStyle=apply(wrapped,new SetProperty(NESTED_ID,new PropertyName("style"),ref));
                assertAnalysis(wrapped,nullableStyle,"probe.dart",merge);
                if(merge)saveCase(prefix+(ref.libraryUri().isPresent()?"nullable_imported":"nullable_current"),nullableStyle);
            }
        }
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("style_test.dart"),runtime());
        run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");
    }
    private static PropertyValue.DartObjectReferenceValue importedRef(String member,boolean factory){
        return new PropertyValue.DartObjectReferenceValue(Optional.of("package:default_text_style_contract/references.dart"),
            "StyleValues",Optional.of(member),
            factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
            factory?Optional.of(false):Optional.empty());
    }
    private static String runtime(){
        var imports=new StringBuilder("import 'package:flutter/material.dart';\nimport 'package:flutter_test/flutter_test.dart';\n");
        var cases=new StringBuilder("final cases=<String,Widget>{");
        for(String prefix:List.of("direct_","merge_"))for(String suffix:List.of("default","typed","imported","null","local","replace","theme")){
            String name=prefix+suffix;
            imports.append("import '../lib/").append(name).append(".dart' as case_").append(name).append(";\n");
            cases.append("'").append(name).append("':const case_").append(name).append(".Sample(),");
        }
        for(String suffix:List.of("current","imported")){
            String name="merge_nullable_"+suffix;
            imports.append("import '../lib/").append(name).append(".dart' as case_").append(name).append(";\n");
            cases.append("'").append(name).append("':const case_").append(name).append(".Sample(),");
        }
        return imports.toString()+"void main(){"+cases+"};"+"""
            for(final e in cases.entries)for(final rtl in [false,true]){
              testWidgets('generated ${e.key} rtl=$rtl',(tester)async{
                Widget host(Widget child)=>MaterialApp(home:Directionality(textDirection:rtl?TextDirection.rtl:TextDirection.ltr,
                  child:DefaultTextStyle(style:const TextStyle(fontSize:24,color:Color(0xFF123456)),
                    textAlign:TextAlign.right,softWrap:false,overflow:TextOverflow.fade,maxLines:3,
                    textWidthBasis:TextWidthBasis.longestLine,
                    textHeightBehavior:const TextHeightBehavior(applyHeightToFirstAscent:false,applyHeightToLastDescent:false),child:child)));
                await tester.pumpWidget(host(e.value));await tester.pumpAndSettle();
                final n=DefaultTextStyle.of(tester.element(find.text('Sibling')));
                final merge=e.key.startsWith('merge_');
                if(e.key.endsWith('_typed')||e.key.endsWith('_imported')&&!e.key.contains('nullable')){
                  expect(n.style.fontSize,19);expect(n.style.fontWeight,FontWeight.w700);
                  expect(n.maxLines,2);expect(n.textHeightBehavior?.applyHeightToFirstAscent,false);
                  expect(n.textHeightBehavior?.applyHeightToLastDescent,true);
                }else if(e.key.endsWith('_local')||e.key.endsWith('_replace')){
                  expect(n.style.fontSize,22);expect(n.style.fontWeight,FontWeight.w700);
                  expect(n.style.decoration,TextDecoration.underline);
                  expect(n.style.color,merge&&!e.key.endsWith('_replace')?const Color(0xFF123456):null);
                  expect(n.textHeightBehavior?.applyHeightToLastDescent,true);
                  expect(n.maxLines,merge?3:null);
                }else if(e.key.endsWith('_theme')){expect(n.style.fontSize,isNotNull);}
                else{
                  expect(n.style.fontSize,merge?24:null);expect(n.style.color,merge?const Color(0xFF123456):null);
                  expect(n.maxLines,merge?3:null);expect(n.textAlign,merge?TextAlign.right:null);
                  expect(n.textHeightBehavior?.applyHeightToFirstAscent,merge?false:null);
                }
                expect(n.softWrap,!merge);expect(n.overflow,merge?TextOverflow.fade:TextOverflow.clip);
                expect(n.textWidthBasis,merge?TextWidthBasis.longestLine:TextWidthBasis.parent);
                expect(tester.takeException(),isNull);
              });
            }
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
                name: default_text_style_contract
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
