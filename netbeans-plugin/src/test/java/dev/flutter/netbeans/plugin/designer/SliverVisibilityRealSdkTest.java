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
class SliverVisibilityRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;
    private static final StableId GROUP_ID = StableId.random(), NESTED_ID = StableId.random();
    private static final SlotName SLIVERS = new SlotName("slivers");

    @Test void allConstructorAndMaintenanceBranchesPassAnalyzerSaveHistoryAndNativeSdk() throws Exception {
        initialize();
        var imports = new StringBuilder();
        var cases = new StringBuilder();
        for (boolean maintain : List.of(false, true)) {
            String prefix = maintain ? "maintain" : "normal";
            var child = adapter("Main body", 40);
            var baseline = open(root(group(GROUP_ID, List.of(child, adapter("Sibling", 60)))),
                    "probe.dart", "// User member is preserved.\nint _userValue() => 73;");
            var type = maintain ? SliverVisibilityWidgetPropertySchema.MAINTAIN_TYPE : SliverVisibilityWidgetPropertySchema.TYPE;
            var prototype = WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(type).orElseThrow(), NESTED_ID);
            var wrapped = apply(baseline, new WrapWidget(child.id(), prototype, new SlotName("sliver"), 0));
            assertAnalysis(baseline, wrapped, "probe.dart", true);
            assertArrayEquals(baseline.current().dartCandidateBytes(), wrapped.undo().session().current().dartCandidateBytes());
            assertArrayEquals(wrapped.current().dartCandidateBytes(), wrapped.undo().session().redo().session().current().dartCandidateBytes());
            recordCase(prefix + "Default", wrapped, maintain ? 63 : 1, false, imports, cases);
            var current = reopen(wrapped, source(wrapped));
            var replacement = adapter("Replacement", 72);
            var added = apply(current, new AddWidget(new WidgetPlacement(NESTED_ID, new SlotName("replacementSliver"), 0), replacement));
            assertAnalysis(current, added, "probe.dart", true); current = reopen(added, source(added));
            for (int bits=0;bits<64;bits++) {
                if (maintain ? bits != 62 && bits != 63 : !validFlags(bits)) continue;
                var patches = new ArrayList<PatchProperties.Patch>();
                for (var entry : flags(bits).entrySet()) {
                    if (!maintain || entry.getKey().value().equals("visible")) patches.add(new PatchProperties.SetPatch(entry.getKey(),entry.getValue()));
                }
                var changed = apply(current,new PatchProperties(NESTED_ID,patches));
                assertAnalysis(current,changed,"probe.dart",true);
                assertArrayEquals(current.current().dartCandidateBytes(),changed.undo().session().current().dartCandidateBytes());
                recordCase(prefix + "Flags" + bits,changed,bits,true,imports,cases);
                current=reopen(changed,source(changed));
            }
            var resets = new ArrayList<PatchProperties.Patch>();
            for (var p : BuiltInWidgetCatalog.getDefault().find(type).orElseThrow().properties()) resets.add(new PatchProperties.ResetPatch(p.name()));
            var reset=apply(current,new PatchProperties(NESTED_ID,resets));
            assertAnalysis(current,reset,"probe.dart",true); current=reopen(reset,source(reset));
            var moved=apply(current,new MoveWidget(replacement.id(),place(current.current().document().root().id(),1)));
            assertAnalysis(current,moved,"probe.dart",true);
            assertArrayEquals(current.current().dartCandidateBytes(),moved.undo().session().current().dartCandidateBytes());
            recordCase(prefix+"Moved",moved,maintain?63:1,false,imports,cases);
        }
        String dartTest = """
                import 'package:flutter/material.dart';
                import 'package:flutter/rendering.dart';
                import 'package:flutter_test/flutter_test.dart';
                %s
                void main() {
                  final cases = <String, (Widget, int, bool)>{
                    %s
                  };
                  for (final entry in cases.entries) {
                    for (final horizontal in [false,true]) {
                      for (final reverse in [false,true]) {
                        for (final rtl in [false,true]) {
                          testWidgets('${entry.key} h=$horizontal r=$reverse rtl=$rtl', (tester) async {
                            final bits=entry.value.$2;
                            bool flag(int index) => bits & (1 << index) != 0;
                            await tester.pumpWidget(MaterialApp(home:entry.value.$1));
                            await tester.pumpAndSettle();
                            final sliver=tester.widget<SliverVisibility>(find.byType(SliverVisibility,skipOffstage:false));
                            expect([sliver.visible,sliver.maintainState,sliver.maintainAnimation,sliver.maintainSize,sliver.maintainSemantics,sliver.maintainInteractivity],
                              [for(var i=0;i<6;i++)flag(i)]);
                            await tester.pumpWidget(MaterialApp(home:Directionality(
                              textDirection:rtl?TextDirection.rtl:TextDirection.ltr,child:Center(child:SizedBox(width:300,height:160,
                              child:CustomScrollView(primary:false,scrollDirection:horizontal?Axis.horizontal:Axis.vertical,
                              reverse:reverse,slivers:[sliver]))))));
                            await tester.pumpAndSettle();
                            final render=tester.renderObject<RenderSliver>(find.byType(SliverVisibility,skipOffstage:false));
                            final extent=flag(0)||flag(3)?40:flag(1)?0:entry.value.$3?72:0;
                            expect(render.geometry!.scrollExtent,extent);
                            expect(render.geometry!.paintExtent,extent);
                            expect(tester.takeException(),isNull);
                          });
                        }
                      }
                    }
                  }
                }
                """.formatted(imports,cases);
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("visibility_test.dart"),dartTest);
        run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");
    }
    private void recordCase(String name, DesignerCommandSession session, int bits, boolean replacement,
            StringBuilder imports, StringBuilder cases) throws Exception {
        saveCase(name,session);
        imports.append("import '../lib/").append(name).append(".dart' as ").append(name).append(";\n");
        cases.append("'").append(name).append("': (const ").append(name).append(".Sample(), ").append(bits).append(", ").append(replacement).append("),\n");
    }
    private static boolean validFlags(int b) {
        return ((b&4)==0 || (b&2)!=0) && ((b&8)==0 || (b&4)!=0) && ((b&16)==0 || (b&8)!=0) && ((b&32)==0 || (b&8)!=0);
    }
    private static Map<PropertyName,PropertyValue> flags(int bits) {
        var result = new LinkedHashMap<PropertyName,PropertyValue>();
        var fields=SliverVisibilityWidgetPropertySchema.FIELDS;
        for(int i=0;i<fields.size();i++) result.put(new PropertyName(fields.get(i).name()),new PropertyValue.BooleanValue((bits&(1<<i))!=0));
        return result;
    }
    private void saveCase(String name, DesignerCommandSession session) throws Exception {
        var reopened = reopen(session, source(session));
        assertEquals(session.current().fdSnapshot(), reopened.current().fdSnapshot());
        assertArrayEquals(session.current().dartCandidateBytes(), reopened.current().dartCandidateBytes());
        assertTrue(source(reopened).contains("// User member is preserved."));
        assertTrue(source(reopened).contains("int _userValue() => 73;"));
        Files.write(lib.resolve(name + ".dart"), reopened.current().dartCandidateBytes());
    }
    private static WidgetPlacement place(StableId id, int index) { return new WidgetPlacement(id, SLIVERS, index); }
    private static WidgetNode group(StableId id, List<WidgetNode> children) {
        return new WidgetNode(id, SliverCrossAxisGroupWidgetPropertySchema.TYPE, Map.of(), Map.of(SLIVERS, new WidgetSlot.ListSlot(children)));
    }
    private static WidgetNode adapter(String label, int extent) {
        var text = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue(label)), Map.of());
        var size = new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(extent));
        var box = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.SizedBox"),
                Map.of(new PropertyName("width"), size, new PropertyName("height"), size),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text)));
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.SliverToBoxAdapter"), Map.of(),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(box)));
    }
    private static WidgetNode root(WidgetNode group) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.CustomScrollView"),
                Map.of(new PropertyName("primary"), new PropertyValue.BooleanValue(false)),
                Map.of(SLIVERS, new WidgetSlot.ListSlot(List.of(group))));
    }
    private void initialize() throws Exception {
        sdk = Path.of(System.getProperty("flutter.events.sdk")).toRealPath();
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        flutter = sdk.resolve(windows ? "bin/flutter.bat" : "bin/flutter");
        dart = sdk.resolve(windows ? "bin/cache/dart-sdk/bin/dart.exe" : "bin/cache/dart-sdk/bin/dart");
        Files.writeString(project.resolve("pubspec.yaml"), """
                name: sliver_visibility_contract
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
            if (!pass) assertTrue(result.symbolEvidence().stream().anyMatch(evidence -> evidence.staticTypeEvidence()
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

