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
class SliverAppBarRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;
    private static final StableId HEADER_ID = StableId.random();
    private static final SlotName SLIVERS = new SlotName("slivers");
    private static final SlotName CHILD = new SlotName("child");

    @Test void allVariantsSlotsStyleReferencesAsyncEventAndHistoryPassRealSdk() throws Exception {
        initialize();
        Files.writeString(lib.resolve("style.dart"), """
                import 'package:flutter/material.dart';
                import 'package:flutter/services.dart';
                class Values {
                  static ShapeBorder? shape() => const RoundedRectangleBorder();
                  static IconThemeData? iconTheme() => const IconThemeData(size: 22);
                  static IconThemeData? actionsIconTheme() => const IconThemeData(size: 20);
                  static TextStyle? toolbarTextStyle() => const TextStyle(fontSize: 14);
                  static TextStyle? titleTextStyle() => const TextStyle(fontSize: 24);
                  static SystemUiOverlayStyle? systemOverlayStyle() => const SystemUiOverlayStyle(statusBarColor: Colors.red);
                  static Future<void> stretch() async {}
                }
                """);
        int index=0;
        for(String type:SliverAppBarWidgetPropertySchema.TYPES) {
            var baseline=open(root(adapter("Before",40)),"probe.dart","""
                    // User member is preserved.
                    int _userValue() => 73;
                    int get _wrong => 1;
                    dynamic get _dynamic => null;
                    Future<void> _stretch() async {}
                    """);
            var def=BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId(type)).orElseThrow();
            var header=WidgetNodePrototypeFactory.create(def,HEADER_ID);
            var created=apply(baseline,new AddWidget(place(baseline.current().document().root().id(),1),header));
            assertAnalysis(baseline,created,"probe.dart",true);saveCase("created"+index,created);
            created=reopen(created,source(created));
            var props=new LinkedHashMap<PropertyName,PropertyValue>();
            for(String name:List.of("automaticallyImplyLeading","automaticallyImplyActions","forceElevated","primary","centerTitle",
                    "excludeHeaderSemantics","floating","pinned","snap","stretch","forceMaterialTransparency","useDefaultSemanticsOrder"))
                props.put(p(name),new PropertyValue.BooleanValue(true));
            for(String name:List.of("elevation","scrolledUnderElevation","titleSpacing","leadingWidth"))props.put(p(name),number(8));
            props.put(p("toolbarHeight"),integer(72));props.put(p("collapsedHeight"),integer(80));props.put(p("expandedHeight"),integer(200));
            props.put(p("stretchTriggerOffset"),integer(110));props.put(p("onStretchTrigger"),ref("_stretch",false));
            for(String name:List.of("shadowColor","surfaceTintColor","backgroundColor","foregroundColor"))props.put(p(name),new PropertyValue.ColorValue(0xff112233L));
            props.put(p("clipBehavior"),new PropertyValue.EnumValue("Clip","antiAlias"));
            props.put(p("shapeKind"),new PropertyValue.StringValue("roundedRectangle"));props.put(p("shapeRadiusTopLeft"),number(8));
            props.put(p("iconThemeSize"),number(24));props.put(p("actionsIconThemeSize"),number(20));
            props.put(p("toolbarTextStyleFontSize"),number(14));props.put(p("titleTextStyleFontSize"),number(24));
            props.put(p("systemOverlayStyleStatusBarBrightness"),new PropertyValue.EnumValue("Brightness","dark"));
            var configured=created;
            var patches=new ArrayList<PatchProperties.Patch>();
            for(var entry:props.entrySet()) patches.add(new PatchProperties.SetPatch(entry.getKey(),entry.getValue()));
            configured=apply(configured,new PatchProperties(HEADER_ID,patches));
            for(String slot:List.of("leading","title","actions","flexibleSpace","bottom")){
                var child=slot.equals("bottom")?new WidgetNode(StableId.random(),new WidgetTypeId("flutter.widgets.PreferredSize"),
                        Map.of(p("preferredSize"),new PropertyValue.SizeValue(java.math.BigDecimal.valueOf(24),java.math.BigDecimal.valueOf(24))),Map.of(CHILD,WidgetSlot.SingleSlot.of(box(24)))):box(24);
                configured=apply(configured,new AddWidget(new WidgetPlacement(HEADER_ID,new SlotName(slot),0),child));
            }
            assertAnalysis(created,configured,"probe.dart",true);saveCase("configured"+index,configured);
            assertArrayEquals(configured.current().dartCandidateBytes(),configured.undo().session().redo().session().current().dartCandidateBytes());
            var whole=created;
            patches=new ArrayList<>();
            for(String field:SliverAppBarWidgetPropertySchema.WHOLE_TYPES.keySet())
                patches.add(new PatchProperties.SetPatch(p(field),imported(field,true)));
            patches.add(new PatchProperties.SetPatch(p("onStretchTrigger"),imported("stretch",false)));
            whole=apply(whole,new PatchProperties(HEADER_ID,patches));
            assertAnalysis(created,whole,"probe.dart",true);saveCase("references"+index,whole);
            for(String field:List.of("shape","systemOverlayStyle","onStretchTrigger"))for(String bad:List.of("_wrong","_dynamic")){
                var rejected=apply(created,new SetProperty(HEADER_ID,p(field),ref(bad,false)));
                assertAnalysis(created,rejected,"probe.dart",false);
            }
            index++;
        }
        run(List.of(flutter.toString(),"analyze","--no-fatal-warnings","--no-fatal-infos","lib"),"analyze.log");
        StringBuilder imports=new StringBuilder("import 'package:flutter/material.dart';\nimport 'package:flutter_test/flutter_test.dart';\n");
        StringJoiner cases=new StringJoiner(",");
        for(int n=0;n<3;n++)for(String kind:List.of("created","configured","references")){
            String alias=kind+n;imports.append("import '../lib/"+alias+".dart' as "+alias+";\n");cases.add("const "+alias+".Sample()");
        }
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("native_test.dart"),imports+"""
                void main() {
                  final cases=<Widget>[
                """+cases+"""
                  ];
                  for(var i=0;i<cases.length;i++) testWidgets('generated app bar case $i',(tester)async {
                    await tester.pumpWidget(MaterialApp(home:cases[i]));
                    final native=tester.widget<SliverAppBar>(find.byType(SliverAppBar));
                    if(i%3==0){expect(native.pinned,i>=3);expect(native.toolbarHeight,i<3?56:64);}
                    if(i%3==1){expect(native.bottom!.preferredSize.height,24);expect(native.actions!.length,1);expect(native.onStretchTrigger,isNotNull);}
                    if(i%3==2){expect(native.shape,isA<RoundedRectangleBorder>());expect(native.titleTextStyle!.fontSize,24);}
                    await tester.pumpWidget(const SizedBox.shrink());
                    await tester.pumpWidget(MaterialApp(home:CustomScrollView(slivers:[native,const SliverToBoxAdapter(child:SizedBox(height:2000))])));
                    await tester.timedDrag(find.byType(CustomScrollView),const Offset(0,-400),const Duration(milliseconds:500));
                    await tester.pumpAndSettle();
                    await tester.timedDrag(find.byType(CustomScrollView),const Offset(0,50),const Duration(milliseconds:500));
                    await tester.pumpAndSettle();expect(tester.takeException(),isNull);
                  });
                }
                """);
        run(List.of(flutter.toString(),"test","--reporter","expanded"),"native-test.log");
    }
    private static PropertyName p(String value){return new PropertyName(value);}
    private static PropertyValue.IntegerValue integer(long value){return new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(value));}
    private static PropertyValue.DartObjectReferenceValue ref(String name,boolean call){return new PropertyValue.DartObjectReferenceValue(Optional.empty(),name,Optional.empty(),
        call?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,call?Optional.of(false):Optional.empty());}
    private static PropertyValue.DartObjectReferenceValue imported(String member,boolean call){return new PropertyValue.DartObjectReferenceValue(Optional.of("package:sliver_app_bar_contract/style.dart"),"Values",Optional.of(member),
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
                name: sliver_app_bar_contract
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
