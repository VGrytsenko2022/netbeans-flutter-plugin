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
class ThemeRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;
    private static final StableId GROUP_ID = StableId.random(), NESTED_ID = StableId.random();
    private static final SlotName CHILDREN = new SlotName("children");

    @Test
    void presetsLocalAndImportedThemeDataReferencesSaveReopenUndoAndRuntime() throws Exception {
        initialize();
        Files.writeString(lib.resolve("themes.dart"), """
                import 'package:flutter/material.dart';
                ThemeData get configured => ThemeData.dark();
                ThemeData createTheme() => ThemeData.dark();
                class Themes {
                  static ThemeData get configured => ThemeData.dark();
                  static ThemeData createTheme() => ThemeData.dark();
                }
                """);
        var child = adapter("Themed child", 80);
        var baseline = open(group(GROUP_ID, List.of(child)), "probe.dart", """
                // User member is preserved.
                int _userValue() => 73;
                ThemeData get _theme => ThemeData.dark().copyWith(
                  scaffoldBackgroundColor: const Color(0xff112233),
                  iconTheme: const IconThemeData(color: Color(0xff33aa55), size: 31),
                  textSelectionTheme: const TextSelectionThemeData(cursorColor: Color(0xff5500aa)));
                ThemeData _themeFactory() => _theme;
                String get _bad => 'wrong';
                ThemeData? get _nullableTheme => null;
                dynamic get _dynamicTheme => ThemeData.light();
                """);
        var prototype = WidgetNodePrototypeFactory.create(
                BuiltInWidgetCatalog.getDefault().find(ThemeWidgetPropertySchema.TYPE).orElseThrow(), NESTED_ID);
        var wrapped = apply(baseline, new WrapWidget(child.id(), prototype, new SlotName("child"), 0));
        assertAnalysis(baseline, wrapped, "probe.dart", true);
        wrapped = reopen(wrapped, source(wrapped));
        var names = new ArrayList<String>();
        for (String preset : ThemeWidgetPropertySchema.PRESETS) {
            var candidate = preset.equals("light") ? wrapped : apply(wrapped,
                    new SetProperty(NESTED_ID, new PropertyName("data"), new PropertyValue.StringValue(preset)));
            if (candidate != wrapped) assertAnalysis(wrapped, candidate, "probe.dart", true);
            var name = preset.toLowerCase(Locale.ROOT);
            saveCase(name, candidate);
            names.add(name);
        }
        var typed = apply(wrapped, new SetProperty(NESTED_ID, new PropertyName("data"), ref("_theme", false)));
        assertAnalysis(wrapped, typed, "probe.dart", true);
        saveCase("typed", typed); names.add("typed");
        typed = reopen(typed, source(typed));
        var factory = apply(typed, new SetProperty(NESTED_ID, new PropertyName("data"), ref("_themeFactory", true)));
        assertAnalysis(typed, factory, "probe.dart", true);
        saveCase("factory", factory); names.add("factory");
        assertArrayEquals(typed.current().dartCandidateBytes(), factory.undo().session().current().dartCandidateBytes());
        for (boolean member : List.of(false, true)) for (boolean invocation : List.of(false, true)) {
            String symbol = invocation ? "createTheme" : "configured";
            var reference = new PropertyValue.DartObjectReferenceValue(Optional.of("package:theme_contract/themes.dart"),
                    member ? "Themes" : symbol, member ? Optional.of(symbol) : Optional.empty(),
                    invocation ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION
                            : PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                    invocation ? Optional.of(false) : Optional.empty());
            var imported = apply(typed, new SetProperty(NESTED_ID, new PropertyName("data"), reference));
            assertAnalysis(typed, imported, "probe.dart", true);
            String name = "imported_" + (member ? "member" : "top") + (invocation ? "_factory" : "_getter");
            saveCase(name, imported); names.add(name);
        }
        for (String bad : List.of("_bad", "_nullableTheme", "_dynamicTheme"))
            assertAnalysis(typed, apply(typed, new SetProperty(NESTED_ID, new PropertyName("data"), ref(bad, false))), "probe.dart", false);
        String imports = names.stream().map(n -> "import '../lib/" + n + ".dart' as case_" + n + ";").collect(java.util.stream.Collectors.joining("\n"));
        String cases = names.stream().map(n -> "'" + n + "': const case_" + n + ".Sample()").collect(java.util.stream.Collectors.joining(","));
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("theme_test.dart"), """
                import 'package:flutter/material.dart';
                import 'package:flutter/cupertino.dart';
                import 'package:flutter_test/flutter_test.dart';
                """ + imports + "\nvoid main(){ final cases=<String,Widget>{" + cases + "};\n" + """
                  for(final e in cases.entries)for(final rtl in [false,true]){
                    testWidgets('generated ${e.key} rtl=$rtl',(tester)async{
                      await tester.pumpWidget(MaterialApp(home:Directionality(
                        textDirection:rtl?TextDirection.rtl:TextDirection.ltr, child:e.value)));
                      final target=find.ancestor(of:find.text('Themed child'),matching:find.byType(Theme)).first;
                      final n=tester.widget<Theme>(target), context=tester.element(find.text('Themed child'));
                      final theme=Theme.of(context);
                      expect(n.child,isA<SizedBox>());
                      if(['typed','factory'].contains(e.key)){
                        expect(theme.scaffoldBackgroundColor,const Color(0xff112233));
                        expect(IconTheme.of(context).size,31);
                        expect(DefaultSelectionStyle.of(context).cursorColor,const Color(0xff5500aa));
                      }else{
                        expect(n.data.brightness,e.key.startsWith('dark')||e.key.startsWith('imported_')?Brightness.dark:Brightness.light);
                        expect(n.data.useMaterial3,!e.key.endsWith('m2'));
                      }
                      expect(tester.takeException(),isNull);
                    });
                  }
                  testWidgets('Theme updates immediately preserving descendant State',(tester)async{
                    Widget page(ThemeData data)=>MaterialApp(home:Theme(
                      data:data,child:const Material(child:TextField(key:ValueKey('field')))));
                    await tester.pumpWidget(page(ThemeData.light()));
                    final state=tester.state(find.byType(TextField));
                    await tester.pumpWidget(page(ThemeData.dark()));
                    expect(Theme.of(tester.element(find.byType(TextField))).brightness,Brightness.dark);
                    expect(tester.state(find.byType(TextField)),same(state));
                    expect(tester.takeException(),isNull);
                  });
                  testWidgets('native inherited Cupertino and selection precedence',(tester)async{
                    late BuildContext below;
                    final data=ThemeData.light().copyWith(
                      cupertinoOverrideTheme:const CupertinoThemeData(primaryColor:Color(0xff112233)));
                    Widget inner()=>DefaultSelectionStyle(cursorColor:const Color(0xffabcdef),
                      selectionColor:const Color(0xfffedcba),child:Theme(data:data,
                        child:Builder(builder:(context){below=context;return const SizedBox();})));
                    Widget host(Widget child)=>MediaQuery(data:const MediaQueryData(),child:Directionality(
                      textDirection:TextDirection.ltr,child:child));
                    await tester.pumpWidget(host(inner()));
                    expect(CupertinoTheme.of(below).primaryColor,const Color(0xff112233));
                    expect(DefaultSelectionStyle.of(below).cursorColor,const Color(0xffabcdef));
                    expect(DefaultSelectionStyle.of(below).selectionColor,const Color(0xfffedcba));
                    await tester.pumpWidget(host(CupertinoTheme(
                      data:const CupertinoThemeData(primaryColor:Color(0xff224466)),child:inner())));
                    expect(CupertinoTheme.of(below).primaryColor,const Color(0xff224466));
                    expect(tester.takeException(),isNull);
                  });
                }
                """);
        run(List.of(flutter.toString(), "test", "--reporter", "expanded"), "flutter-test.log");
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
                name: theme_contract
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
        for(var probe:ticket.request().symbolProbes())probe.staticTypeProbe().filter(t->t.expectedDartType().equals("ThemeData")).ifPresent(t->assertEquals("package:flutter/material.dart",t.expectedTypeLibraryUri()));
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
