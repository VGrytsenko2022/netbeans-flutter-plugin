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
class SliverFloatingHeaderRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;
    private static final StableId HEADER_ID = StableId.random();
    private static final SlotName SLIVERS = new SlotName("slivers");
    private static final SlotName CHILD = new SlotName("child");

    @Test void requiredChildHistoryAllAnimationArgumentsReferencesAndNativeRuntimePassSdk() throws Exception {
        initialize();
        var baseline=open(root(adapter("Before",40)),"probe.dart","""
                // User member is preserved.
                int _userValue() => 73;
                AnimationStyle get _style => const AnimationStyle(duration: Duration(milliseconds: 120), reverseDuration: Duration(milliseconds: 240));
                Duration get _duration => const Duration(milliseconds: 120);
                Duration _reverseDuration() => const Duration(milliseconds: 240);
                Curve get _curve => Curves.linear;
                Curve _reverseCurve() => Curves.easeOut;
                int get _wrong => 1;
                dynamic get _dynamic => null;
                AnimationStyle? get _nullableStyle => null;
                Duration? _nullableDuration() => null;
                Curve? get _nullableCurve => null;
                """);
        var def=BuiltInWidgetCatalog.getDefault().find(SliverFloatingHeaderWidgetPropertySchema.TYPE).orElseThrow();
        var header=WidgetNodePrototypeFactory.create(def,HEADER_ID);
        var created=apply(baseline,new AddWidget(place(baseline.current().document().root().id(),1),header));
        assertAnalysis(baseline,created,"probe.dart",true);saveCase("created",created);
        assertArrayEquals(baseline.current().dartCandidateBytes(),created.undo().session().current().dartCandidateBytes());
        assertArrayEquals(created.current().dartCandidateBytes(),created.undo().session().redo().session().current().dartCandidateBytes());
        var saved=reopen(created,source(created));
        var child=((WidgetSlot.SingleSlot)header.slots().get(CHILD)).child().orElseThrow();
        assertFalse(saved.apply(new RemoveWidget(child.id())).changed());
        var before=((WidgetSlot.ListSlot)saved.current().document().root().slots().get(SLIVERS)).children().getFirst();
        assertFalse(saved.apply(new MoveWidget(child.id(),new WidgetPlacement(before.id(),CHILD,0))).changed());
        var replacement=box(80);
        var replaced=apply(saved,new ReplaceSlotChild(HEADER_ID,CHILD,child.id(),new ReplaceSlotChild.NewSubtree(replacement)));
        assertAnalysis(saved,replaced,"probe.dart",true);saveCase("replaced",replaced);
        assertArrayEquals(saved.current().dartCandidateBytes(),replaced.undo().session().current().dartCandidateBytes());

        var styles=new ArrayList<Map<PropertyName,PropertyValue>>();
        styles.add(Map.of());
        styles.add(Map.of(p("animationStyle"),new PropertyValue.NullValue()));
        styles.add(Map.of(p("animationStyle"),new PropertyValue.StringValue("noAnimation")));
        var local=new LinkedHashMap<PropertyName,PropertyValue>();
        local.put(p("animationStyleDurationUs"),integer(120000));local.put(p("animationStyleReverseDurationUs"),integer(240000));
        local.put(p("animationStyleCurve"),new PropertyValue.StringValue("linear"));local.put(p("animationStyleReverseCurve"),new PropertyValue.StringValue("easeOut"));
        styles.add(local);
        styles.add(Map.of(p("animationStyle"),ref("_style",false)));
        styles.add(Map.of(p("animationStyleDurationUs"),ref("_duration",false),p("animationStyleReverseDurationUs"),ref("_reverseDuration",true),
                p("animationStyleCurve"),ref("_curve",false),p("animationStyleReverseCurve"),ref("_reverseCurve",true)));
        styles.add(Map.of(p("animationStyleDurationUs"),new PropertyValue.NullValue(),p("animationStyleReverseDurationUs"),new PropertyValue.NullValue(),
                p("animationStyleCurve"),new PropertyValue.NullValue(),p("animationStyleReverseCurve"),new PropertyValue.NullValue()));
        Files.writeString(lib.resolve("style.dart"),"""
                import 'package:flutter/widgets.dart';
                class Values {
                  static const style = AnimationStyle(duration: Duration(milliseconds: 120), reverseDuration: Duration(milliseconds: 240));
                  static const duration = Duration(milliseconds: 120);
                  static Duration reverseDuration() => const Duration(milliseconds: 240);
                  static const curve = Curves.linear;
                  static Curve reverseCurve() => Curves.easeOut;
                }
                """);
        styles.add(Map.of(p("animationStyle"),imported("style",false)));
        styles.add(Map.of(p("animationStyleDurationUs"),imported("duration",false),p("animationStyleReverseDurationUs"),imported("reverseDuration",true),
                p("animationStyleCurve"),imported("curve",false),p("animationStyleReverseCurve"),imported("reverseCurve",true)));
        styles.add(Map.of(p("animationStyle"),ref("_nullableStyle",false)));
        styles.add(Map.of(p("animationStyleDurationUs"),ref("_nullableDuration",true),p("animationStyleReverseDurationUs"),ref("_nullableDuration",true),
                p("animationStyleCurve"),ref("_nullableCurve",false),p("animationStyleReverseCurve"),ref("_nullableCurve",false)));
        StringBuilder imports=new StringBuilder("import 'package:flutter/material.dart';\nimport 'package:flutter_test/flutter_test.dart';\n");
        StringJoiner cases=new StringJoiner(",");
        for(int index=0;index<styles.size();index++){
            var properties=new LinkedHashMap<>(styles.get(index));
            properties.put(p("snapMode"),index%3==0?new PropertyValue.NullValue():new PropertyValue.EnumValue("FloatingHeaderSnapMode",index%3==1?"overlay":"scroll"));
            var candidate=replaced;
            for(var entry:properties.entrySet())candidate=apply(candidate,new SetProperty(HEADER_ID,entry.getKey(),entry.getValue()));
            assertAnalysis(saved,candidate,"probe.dart",true);saveCase("case"+index,candidate);
            imports.append("import '../lib/case"+index+".dart' as c"+index+";\n");cases.add("const c"+index+".Sample()");
        }
        for(String field:List.of("animationStyle","animationStyleDurationUs","animationStyleCurve")){
            for(String wrong:List.of("_wrong","_dynamic")){
                var bad=apply(replaced,new SetProperty(HEADER_ID,p(field),ref(wrong,false)));
                assertAnalysis(saved,bad,"probe.dart",false);
            }
        }
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("floating_test.dart"),imports+"""
                void main() {
                  final cases=<Widget>[
                """+cases+"""
                  ];
                  for(var i=0;i<cases.length;i++) testWidgets('generated constructor case $i',(tester)async {
                    await tester.pumpWidget(MaterialApp(home:cases[i]));
                    final native=tester.widget<SliverFloatingHeader>(find.byType(SliverFloatingHeader));
                    expect(native.child,isNotNull);
                    expect(native.snapMode,i%3==0?null:i%3==1?FloatingHeaderSnapMode.overlay:FloatingHeaderSnapMode.scroll);
                    if([3,4,5,7,8].contains(i)){
                      expect(native.animationStyle!.duration,const Duration(milliseconds:120));
                      expect(native.animationStyle!.reverseDuration,const Duration(milliseconds:240));
                    }
                    if(i==2)expect(native.animationStyle,AnimationStyle.noAnimation);
                    if(i==9)expect(native.animationStyle,isNull);
                    if(i==10){expect(native.animationStyle!.duration,isNull);expect(native.animationStyle!.curve,isNull);}
                    await tester.pumpWidget(const SizedBox.shrink());
                    await tester.pumpWidget(MaterialApp(home:CustomScrollView(slivers:[
                      native,const SliverToBoxAdapter(child:SizedBox(height:2000))])));
                    await tester.timedDrag(find.byType(CustomScrollView),const Offset(0,-400),const Duration(milliseconds:500));
                    await tester.pumpAndSettle();
                    await tester.timedDrag(find.byType(CustomScrollView),const Offset(0,30),const Duration(milliseconds:500));
                    await tester.pumpAndSettle();
                    expect(tester.takeException(),isNull);
                  });
                }
                """);
        run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");
    }
    private static PropertyName p(String value){return new PropertyName(value);}
    private static PropertyValue.IntegerValue integer(long value){return new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(value));}
    private static PropertyValue.DartObjectReferenceValue ref(String name,boolean call){return new PropertyValue.DartObjectReferenceValue(Optional.empty(),name,Optional.empty(),
        call?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,call?Optional.of(false):Optional.empty());}
    private static PropertyValue.DartObjectReferenceValue imported(String member,boolean call){return new PropertyValue.DartObjectReferenceValue(Optional.of("package:floating_header_contract/style.dart"),"Values",Optional.of(member),
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
                name: floating_header_contract
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
