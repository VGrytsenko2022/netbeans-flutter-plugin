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
class FlexibleSpaceBarRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;
    private static final StableId HEADER_ID = StableId.random();
    private static final StableId FLEX_ID = StableId.random();
    private static final SlotName SLIVERS = new SlotName("slivers");
    private static final SlotName CHILD = new SlotName("child");

    @Test void allOwnersPresetsNullableAndGenericReferencesPassStrictSdkEvidenceAndRuntime() throws Exception {
        initialize();
        Files.writeString(lib.resolve("style.dart"), """
                import 'package:flutter/material.dart';
                class Values {
                  static EdgeInsetsGeometry? get padding => null;
                  static EdgeInsetsGeometry? paddingFactory() => const EdgeInsetsDirectional.only(start: 12);
                  static List<StretchMode> get modes => const [StretchMode.blurBackground, StretchMode.fadeTitle];
                  static List<StretchMode> modesFactory() => const [];
                }
                """);
        var owners = new ArrayList<>(List.of("flutter.material.AppBar"));
        owners.addAll(SliverAppBarWidgetPropertySchema.TYPES);
        int index = 0;
        for (String owner : owners) {
            var header = WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId(owner)).orElseThrow(), HEADER_ID);
            var root = owner.endsWith(".AppBar")
                    ? new WidgetNode(StableId.random(), new WidgetTypeId("flutter.material.Scaffold"), Map.of(),
                        Map.of(new SlotName("appBar"), WidgetSlot.SingleSlot.of(header)))
                    : root(header);
            var baseline = open(root, "probe.dart", """
                    // User member is preserved.
                    int _userValue() => 73;
                    EdgeInsetsGeometry? get _padding => null;
                    EdgeInsetsGeometry? _paddingFactory() => const EdgeInsets.all(8);
                    List<StretchMode> get _modes => const [StretchMode.zoomBackground];
                    List<StretchMode> _modesFactory() => const [];
                    int get _wrong => 1;
                    dynamic get _dynamic => null;
                    List<String> get _wrongList => const [];
                    List<dynamic> get _dynamicList => const [];
                    List<StretchMode>? get _nullableList => null;
                    """);
            var flex = WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(FlexibleSpaceBarWidgetPropertySchema.TYPE).orElseThrow(), FLEX_ID);
            var created = apply(baseline, new AddWidget(new WidgetPlacement(HEADER_ID, new SlotName("flexibleSpace"), 0), flex));
            assertAnalysis(baseline, created, "probe.dart", true);
            saveCase("created"+index, created);
            created = reopen(created, source(created));
            var configured = apply(created, new PatchProperties(FLEX_ID, List.of(
                    new PatchProperties.SetPatch(p("centerTitle"), new PropertyValue.BooleanValue(false)),
                    new PatchProperties.SetPatch(p("titlePadding"), new PropertyValue.NullValue()),
                    new PatchProperties.SetPatch(p("collapseMode"), new PropertyValue.EnumValue("CollapseMode","pin")),
                    new PatchProperties.SetPatch(p("stretchModes"), new PropertyValue.StringValue("zoomBackground,blurBackground,fadeTitle")),
                    new PatchProperties.SetPatch(p("expandedTitleScale"), integer(1)))));
            for (String slot : List.of("title","background"))
                configured = apply(configured, new AddWidget(new WidgetPlacement(FLEX_ID, new SlotName(slot), 0), box(24)));
            assertAnalysis(created, configured, "probe.dart", true);
            saveCase("configured"+index, configured);
            assertArrayEquals(configured.current().dartCandidateBytes(), configured.undo().session().redo().session().current().dartCandidateBytes());
            if(index == 0) {
                for (String preset : FlexibleSpaceBarWidgetPropertySchema.STRETCH_PRESETS) {
                    var changed = apply(created, new SetProperty(FLEX_ID,p("stretchModes"),new PropertyValue.StringValue(preset)));
                    assertAnalysis(created,changed,"probe.dart",true);
                }
                for (boolean imported : List.of(false,true)) for (boolean call : List.of(false,true)) {
                    var refs = apply(created,new PatchProperties(FLEX_ID,List.of(
                        new PatchProperties.SetPatch(p("titlePadding"),imported?imported(call?"paddingFactory":"padding",call):ref(call?"_paddingFactory":"_padding",call)),
                        new PatchProperties.SetPatch(p("stretchModes"),imported?imported(call?"modesFactory":"modes",call):ref(call?"_modesFactory":"_modes",call)))));
                    assertAnalysis(created,refs,"probe.dart",true);
                    saveCase("references"+imported+call,refs);
                }
                for (String field : List.of("titlePadding","stretchModes")) for (String bad : List.of("_wrong","_dynamic")) {
                    assertAnalysis(created,apply(created,new SetProperty(FLEX_ID,p(field),ref(bad,false))),"probe.dart",false);
                }
                for(String bad:List.of("_wrongList","_dynamicList","_nullableList"))
                    assertAnalysis(created,apply(created,new SetProperty(FLEX_ID,p("stretchModes"),ref(bad,false))),"probe.dart",false);
            }
            index++;
        }
        run(List.of(flutter.toString(),"analyze","--no-fatal-warnings","--no-fatal-infos","lib"),"analyze.log");
        StringBuilder imports = new StringBuilder("import 'package:flutter/material.dart';\nimport 'package:flutter_test/flutter_test.dart';\n");
        StringJoiner cases = new StringJoiner(",");
        for(int n=0;n<4;n++) for(String kind:List.of("created","configured")){
            String alias=kind+n; imports.append("import '../lib/"+alias+".dart' as "+alias+";\n"); cases.add("const "+alias+".Sample()");
        }
        for(boolean imp:List.of(false,true))for(boolean call:List.of(false,true)){
            String alias="references"+imp+call; imports.append("import '../lib/"+alias+".dart' as "+alias+";\n"); cases.add("const "+alias+".Sample()");
        }
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("native_test.dart"),imports+"""
                void main() {
                  final cases=<Widget>[
                """+cases+"""
                  ];
                  for(var i=0;i<cases.length;i++) testWidgets('generated flexible space case $i',(tester)async {
                    await tester.pumpWidget(MaterialApp(home:cases[i]));
                    final native=tester.widget<FlexibleSpaceBar>(find.byType(FlexibleSpaceBar));
                    if(i<8 && i%2==1) {
                      expect(native.centerTitle,false);expect(native.titlePadding,isNull);
                      expect(native.expandedTitleScale,1);expect(native.collapseMode,CollapseMode.pin);
                      expect(native.stretchModes,StretchMode.values);expect(native.title,isNotNull);expect(native.background,isNotNull);
                    }
                    expect(tester.takeException(),isNull);
                  });
                }
                """);
        run(List.of(flutter.toString(),"test","--reporter","expanded"),"native-test.log");
    }
    private static PropertyName p(String value){return new PropertyName(value);}
    private static PropertyValue.IntegerValue integer(long value){return new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(value));}
    private static PropertyValue.DartObjectReferenceValue ref(String name,boolean call){return new PropertyValue.DartObjectReferenceValue(Optional.empty(),name,Optional.empty(),
        call?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,call?Optional.of(false):Optional.empty());}
    private static PropertyValue.DartObjectReferenceValue imported(String member,boolean call){return new PropertyValue.DartObjectReferenceValue(Optional.of("package:flexible_space_bar_contract/style.dart"),"Values",Optional.of(member),
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
                name: flexible_space_bar_contract
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

