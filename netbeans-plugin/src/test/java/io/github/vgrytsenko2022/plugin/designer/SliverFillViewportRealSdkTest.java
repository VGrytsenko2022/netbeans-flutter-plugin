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
class SliverFillViewportRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;
    private static final StableId SLIVER_ID = StableId.random();
    @Test void visualChildrenAndCustomDelegatesPassEvidenceHistoryAndRealRuntime() throws Exception {
        initialize();
        Files.writeString(lib.resolve("builders.dart"), """
                import 'package:flutter/widgets.dart';
                int? indexes(Widget child, int index) => index.isEven ? index : null;
                SemanticIndexCallback get configuredIndexes => indexes;
                SemanticIndexCallback makeIndexes() => indexes;
                dynamic badIndexes(Widget child, int index) => index;
                double wrongIndexes(Widget child, int index) => 0;
                int? badArguments(Widget child, String index) => null;
                SemanticIndexCallback? get nullableIndexes => null;
                SliverChildDelegate get children => SliverChildBuilderDelegate((c,i)=>Text('Project page $i'),childCount:2);
                SliverChildDelegate makeChildren() => SliverChildListDelegate(const [Text('Project list')]);
                class CustomChildren extends SliverChildDelegate {
                  const CustomChildren();
                  @override Widget? build(BuildContext context,int index) => index==0 ? const Text('Custom page') : null;
                  @override int get estimatedChildCount => 1;
                  @override bool shouldRebuild(covariant CustomChildren oldDelegate) => false;
                }
                const customChildren=CustomChildren();
                dynamic get dynamicChildren => children;
                int get wrongChildren => 1;
                SliverChildDelegate? get nullableChildren => null;
                """);
        var imports = new StringBuilder();
        var entries = new StringBuilder();
        int index = 0;
        for (String fraction : List.of("0.5", "1.0", "1.5")) for (Boolean pad : Arrays.asList(null, false, true))
        for (boolean empty : List.of(false,true)) {
            var baseline = open(root(false, empty), "probe.dart", "// user member\nfinal int retained=7;");
            var candidate = apply(baseline, new SetProperty(SLIVER_ID,p("viewportFraction"),new PropertyValue.DoubleValue(new java.math.BigDecimal(fraction))));
            if (pad != null) {
                candidate=apply(candidate,new SetProperty(SLIVER_ID,p("padEnds"),new PropertyValue.BooleanValue(pad)));
                candidate=apply(candidate,new SetProperty(SLIVER_ID,p("allowImplicitScrolling"),new PropertyValue.BooleanValue(!pad)));
            }
            for (String flag : List.of("addAutomaticKeepAlives","addRepaintBoundaries","addSemanticIndexes"))
                candidate=apply(candidate,new SetProperty(SLIVER_ID,p(flag),new PropertyValue.BooleanValue(false)));
            candidate=apply(candidate,new SetProperty(SLIVER_ID,p("semanticIndexOffset"),new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(4))));
            assertAnalysis(baseline,candidate,"probe.dart",true);
            var saved=reopen(candidate,source(candidate));
            assertEquals(candidate.current().fdSnapshot(),saved.current().fdSnapshot());
            assertTrue(source(saved).contains("final int retained=7"));
            var reset=apply(saved,new ResetProperty(SLIVER_ID,p("viewportFraction")));
            assertAnalysis(saved,reset,"probe.dart",true);
            var undo=reset.undo(); assertTrue(undo.changed()); assertArrayEquals(saved.current().dartCandidateBytes(),undo.session().current().dartCandidateBytes());
            var redo=undo.session().redo(); assertTrue(redo.changed()); assertArrayEquals(reset.current().dartCandidateBytes(),redo.session().current().dartCandidateBytes());
            String name="case_"+index;
            Files.write(lib.resolve(name+".dart"),saved.current().dartCandidateBytes());
            imports.append("import '../lib/").append(name).append(".dart' as c").append(index).append(";\n");
            entries.append("('").append(name).append("',const c").append(index).append(".Sample(),")
                    .append(fraction).append(",").append(pad==null||pad).append(",").append(pad==null||!pad).append("),\n");
            index++;
        }
        for (boolean delegate : List.of(false,true)) {
            String field=delegate?"delegate":"semanticIndexCallback";
            var baseline=open(root(delegate,false),"probe.dart","");
            for (String good : delegate ? List.of("children","makeChildren","customChildren") : List.of("indexes","configuredIndexes","makeIndexes")) {
                var candidate=apply(baseline,new SetProperty(SLIVER_ID,p(field),imported(good,Optional.empty(),good.startsWith("make"))));
                assertAnalysis(baseline,candidate,"probe.dart",true);
                var saved=reopen(candidate,source(candidate));
                assertEquals(candidate.current().fdSnapshot(),saved.current().fdSnapshot());
                String name="case_"+index;
                Files.write(lib.resolve(name+".dart"),saved.current().dartCandidateBytes());
                imports.append("import '../lib/").append(name).append(".dart' as c").append(index).append(";\n");
                entries.append("('").append(name).append("',const c").append(index).append(".Sample(),1.0,true,true),\n");
                index++;
                if(delegate) assertAnalysis(saved,apply(saved,new SetProperty(SLIVER_ID,p(field),new PropertyValue.StringValue("empty"))),"probe.dart",true);
                else assertAnalysis(saved,apply(saved,new ResetProperty(SLIVER_ID,p(field))),"probe.dart",true);
            }
            var adversarial=reopen(baseline,"// ignore_for_file: invalid_assignment, argument_type_not_assignable\n"+source(baseline));
            for(String bad : delegate ? List.of("dynamicChildren","wrongChildren","nullableChildren")
                    : List.of("badIndexes","wrongIndexes","badArguments","nullableIndexes"))
                assertAnalysis(adversarial,apply(adversarial,new SetProperty(SLIVER_ID,p(field),imported(bad))),"probe.dart",false);
        }
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("viewport_test.dart"),
                "import 'package:flutter/material.dart';\nimport 'package:flutter_test/flutter_test.dart';\n"+imports
                +"void main(){for(final entry in <(String,Widget,double,bool,bool)>["+entries+"]) {\n"
                +"""
                  testWidgets(entry.$1,(tester) async {
                    await tester.pumpWidget(MaterialApp(home:entry.$2)); await tester.pump();
                    final sdk=tester.widget<SliverFillViewport>(find.byType(SliverFillViewport,skipOffstage:false));
                    expect(sdk.viewportFraction,entry.$3); expect(sdk.padEnds,entry.$4); expect(sdk.allowImplicitScrolling,entry.$5);
                    final index=int.parse(entry.$1.substring(5));
                    if(index<18) {
                      expect(find.text('Fill body'),index.isEven?findsOneWidget:findsNothing);
                      final delegate=sdk.delegate as SliverChildListDelegate;
                      expect(delegate.addAutomaticKeepAlives,isFalse); expect(delegate.addRepaintBoundaries,isFalse);
                      expect(delegate.addSemanticIndexes,isFalse); expect(delegate.semanticIndexOffset,4);
                    } else if(index>=21) {
                      expect(find.textContaining(index==21?'Project page':index==22?'Project list':'Custom page'),findsWidgets);
                    }
                    expect(tester.takeException(),isNull);
                  });
                }}
                """);
        run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");
    }
    private static PropertyName p(String name) { return new PropertyName(name); }
    private static WidgetNode box(String type, Map<PropertyName,PropertyValue> properties, WidgetNode child) {
        return new WidgetNode(StableId.random(), new WidgetTypeId(type), properties,
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child)));
    }
    private static WidgetNode root(boolean delegate, boolean empty) {
        var d = BuiltInWidgetCatalog.getDefault().find(delegate ? SliverFillViewportWidgetPropertySchema.DELEGATE : SliverFillViewportWidgetPropertySchema.CHILDREN).orElseThrow();
        var prototype = WidgetNodePrototypeFactory.create(d, SLIVER_ID);
        var text = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(p("data"), new PropertyValue.StringValue("Fill body")), Map.of());
        var fill = new WidgetNode(prototype.id(), prototype.type(), prototype.properties(),
                delegate ? Map.of() : Map.of(new SlotName("children"), new WidgetSlot.ListSlot(empty ? List.of() : List.of(text))));
        var viewport = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.CustomScrollView"),
                Map.of(p("primary"), new PropertyValue.BooleanValue(false)),
                Map.of(new SlotName("slivers"), new WidgetSlot.ListSlot(List.of(fill))));
        return box("flutter.widgets.Center", Map.of(), box("flutter.widgets.SizedBox",
                Map.of(p("width"), new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(300)),
                       p("height"), new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(160))), viewport));
    }
    private void initialize() throws Exception {
        sdk = Path.of(System.getProperty("flutter.events.sdk")).toRealPath();
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        flutter = sdk.resolve(windows ? "bin/flutter.bat" : "bin/flutter");
        dart = sdk.resolve(windows ? "bin/cache/dart-sdk/bin/dart.exe" : "bin/cache/dart-sdk/bin/dart");
        Files.writeString(project.resolve("pubspec.yaml"), """
                name: sliver_fill_contract
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
    private static PropertyValue imported(String name) { return imported(name, Optional.empty(), false); }
    private static PropertyValue imported(String name, Optional<String> member, boolean factory) {
        return new PropertyValue.DartObjectReferenceValue(Optional.of("package:sliver_fill_contract/builders.dart"), name, member,
                factory ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION
                        : PropertyValue.DartObjectReferenceValue.Access.REFERENCE, factory ? Optional.of(false) : Optional.empty());
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






