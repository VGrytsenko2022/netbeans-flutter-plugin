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

@EnabledIfSystemProperty(named="flutter.events.sdk",matches=".+")
class FlexibleSpaceBarSettingsRealSdkTest {
    @TempDir Path project;
    private Path sdk,flutter,dart,lib;
    private static final SlotName CHILD=new SlotName("child"),SLIVERS=new SlotName("slivers");
    private static final StableId SETTINGS_ID=StableId.random(), BOX_ID=StableId.random();
    @Test void wrappingRequiredFieldsAtomicRangesRollbackReopenAndNativeInheritance() throws Exception {
        initialize();
        var title=new WidgetNode(StableId.random(),new WidgetTypeId("flutter.widgets.Text"),
                Map.of(p("data"),new PropertyValue.StringValue("Settings title")),Map.of());
        var bar=new WidgetNode(StableId.random(),FlexibleSpaceBarWidgetPropertySchema.TYPE,Map.of(),
                Map.of(new SlotName("title"),WidgetSlot.SingleSlot.of(title)));
        var box=new WidgetNode(BOX_ID,new WidgetTypeId("flutter.widgets.SizedBox"),
                Map.of(p("width"),integer(320),p("height"),integer(240)),Map.of(CHILD,WidgetSlot.SingleSlot.of(bar)));
        var root=new WidgetNode(StableId.random(),new WidgetTypeId("flutter.widgets.Center"),Map.of(),Map.of(CHILD,WidgetSlot.SingleSlot.of(box)));
        var baseline=open(root,"probe.dart","// User member is preserved.\nint _userValue() => 73;\n");
        var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(FlexibleSpaceBarSettingsWidgetPropertySchema.TYPE).orElseThrow(),SETTINGS_ID);
        var created=apply(baseline,new WrapWidget(BOX_ID,prototype,CHILD,0));
        assertAnalysis(baseline,created,"probe.dart",true);saveCase("created",created);
        created=reopen(created,source(created));
        for(var bad:List.<DesignerCommand>of(new SetProperty(SETTINGS_ID,p("minExtent"),integer(300)),
                new SetProperty(SETTINGS_ID,p("maxExtent"),integer(100)),
                new SetProperty(SETTINGS_ID,p("currentExtent"),integer(0)),
                new SetProperty(SETTINGS_ID,p("toolbarOpacity"),integer(-1)),
                new RemoveWidget(BOX_ID))) {
            var rejected=created.apply(bad);assertFalse(rejected.changed());
            assertArrayEquals(created.current().dartCandidateBytes(),rejected.session().current().dartCandidateBytes());
            assertEquals(created.current().fdSnapshot(),rejected.session().current().fdSnapshot());
        }
        for(String field:List.of("toolbarOpacity","minExtent","maxExtent","currentExtent"))
            assertFalse(created.apply(new PatchProperties(SETTINGS_ID,List.of(new PatchProperties.ResetPatch(p(field))))).changed());
        var cases=new ArrayList<>(List.of("created"));
        int i=0;
        for(var values:List.of(
                Map.<String,PropertyValue>of("toolbarOpacity",new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(0.5))),
                Map.<String,PropertyValue>of("minExtent",integer(20)),
                Map.<String,PropertyValue>of("maxExtent",integer(300)),
                Map.<String,PropertyValue>of("currentExtent",integer(120)),
                Map.<String,PropertyValue>of("isScrolledUnder",new PropertyValue.BooleanValue(true)),
                Map.<String,PropertyValue>of("hasLeading",new PropertyValue.BooleanValue(false)),
                Map.<String,PropertyValue>of("isScrolledUnder",new PropertyValue.NullValue(),"hasLeading",new PropertyValue.NullValue()),
                Map.<String,PropertyValue>of("minExtent",integer(0),"maxExtent",integer(0),"currentExtent",integer(0)),
                Map.<String,PropertyValue>of("minExtent",integer(1000),"maxExtent",integer(1000),"currentExtent",integer(1000)))) {
            var changes=values.entrySet().stream().map(e->(PatchProperties.Patch)new PatchProperties.SetPatch(p(e.getKey()),e.getValue())).toList();
            var candidate=apply(created,new PatchProperties(SETTINGS_ID,changes));
            assertAnalysis(created,candidate,"probe.dart",true);
            assertArrayEquals(candidate.current().dartCandidateBytes(),candidate.undo().session().redo().session().current().dartCandidateBytes());
            String name="configured"+i++;saveCase(name,candidate);cases.add(name);
        }
        run(List.of(flutter.toString(),"analyze","--no-fatal-warnings","--no-fatal-infos","lib"),"analyze.log");
        var imports=new StringBuilder("import 'package:flutter/material.dart';\nimport 'package:flutter_test/flutter_test.dart';\n");
        var widgets=new StringJoiner(",");
        for(String name:cases){imports.append("import '../lib/"+name+".dart' as "+name+";\n");widgets.add("const "+name+".Sample()");}
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("native_test.dart"),imports+"""
                void main() {
                  final cases=<Widget>[
                """+widgets+"""
                  ];
                  for(var i=0;i<cases.length;i++)testWidgets('settings generated case $i',(tester)async {
                    await tester.pumpWidget(MaterialApp(home:cases[i]));
                    final bar=find.byType(FlexibleSpaceBar);
                    final settings=tester.element(bar).dependOnInheritedWidgetOfExactType<FlexibleSpaceBarSettings>()!;
                    expect(tester.getSize(bar),const Size(320,240));
                    if(i==0){expect(settings.minExtent,56);expect(settings.maxExtent,200);expect(settings.currentExtent,200);expect(settings.toolbarOpacity,1);}
                    if(i==1)expect(settings.toolbarOpacity,0.5);
                    if(i==2)expect(settings.minExtent,20);
                    if(i==3)expect(settings.maxExtent,300);
                    if(i==4)expect(settings.currentExtent,120);
                    if(i==5)expect(settings.isScrolledUnder,true);
                    if(i==6)expect(settings.hasLeading,false);
                    if(i==7){expect(settings.hasLeading,isNull);expect(settings.isScrolledUnder,isNull);}
                    if(i>=8){expect(settings.minExtent,i==8?0:1000);expect(settings.currentExtent,settings.minExtent);expect(settings.maxExtent,settings.minExtent);}
                    expect(find.text('Settings title'),findsOneWidget);expect(tester.takeException(),isNull);
                  });
                }
                """);
        run(List.of(flutter.toString(),"test","--reporter","expanded"),"native-test.log");
    }
    private static PropertyName p(String value){return new PropertyName(value);}
    private static PropertyValue.IntegerValue integer(long value){return new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(value));}
    private static PropertyValue.DartObjectReferenceValue ref(String name,boolean call){return new PropertyValue.DartObjectReferenceValue(Optional.empty(),name,Optional.empty(),
        call?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,call?Optional.of(false):Optional.empty());}
    private static PropertyValue.DartObjectReferenceValue imported(String member,boolean call){return new PropertyValue.DartObjectReferenceValue(Optional.of("package:flexible_space_bar_settings_contract/style.dart"),"Values",Optional.of(member),
        call?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,call?Optional.of(false):Optional.empty());}
    private static PropertyValue.DoubleValue number(int value) {
        return new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(value));
    }
    private static WidgetNode box(int extent) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.SizedBox"),
                Map.of(new PropertyName("width"), number(extent), new PropertyName("height"), number(extent)), Map.of());
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
                name: flexible_space_bar_settings_contract
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


