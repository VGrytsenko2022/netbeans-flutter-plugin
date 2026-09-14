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
class SliverFixedExtentListRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;
    private static final StableId SLIVER_ID = StableId.random();
    private final StringBuilder imports = new StringBuilder();
    private final StringBuilder entries = new StringBuilder();
    private int runtimeCases;
    @Test void allConstructorsPassTypedEvidenceSaveReopenHistoryAndRealFlutterRuntime() throws Exception {
        initialize();
        Files.writeString(lib.resolve("builders.dart"), """
                import 'package:flutter/widgets.dart';
                Widget? item(BuildContext context, int index) =>
                    index < 4 ? Text('Item $index', key: ValueKey<int>(index)) : null;
                NullableIndexedWidgetBuilder get configuredItem => item;
                NullableIndexedWidgetBuilder makeItem() => item;
                int? indexes(Key key) => key is ValueKey<int> ? key.value : null;
                ChildIndexGetter? get absentIndex => null;
                SliverChildDelegate get children => SliverChildBuilderDelegate(item, childCount: 4);
                SliverChildDelegate makeChildren() => SliverChildListDelegate(const [Text('Delegate item')]);
                class CustomChildren extends SliverChildDelegate {
                  const CustomChildren();
                  @override Widget? build(BuildContext context, int index) => index == 0 ? const Text('Custom item') : null;
                  @override int get estimatedChildCount => 1;
                  @override bool shouldRebuild(covariant CustomChildren oldDelegate) => false;
                }
                const customChildren = CustomChildren();
                dynamic dynamicItem(BuildContext c, int i) => null;
                Widget? wrongIndex(BuildContext c, String i) => null;
                Future<Widget?> asyncItem(BuildContext c, int i) async => null;
                NullableIndexedWidgetBuilder? get nullableItem => null;
                dynamic dynamicIndexes(Key key) => 0;
                double wrongIndexes(Key key) => 0;
                int? wrongArguments(String key) => null;
                dynamic get dynamicChildren => children;
                int get wrongChildren => 1;
                SliverChildDelegate? get nullableChildren => null;
                """);
        var list = SliverFixedExtentListWidgetPropertySchema.Kind.LIST;
        for (String extent : List.of("0", "24.5", "120")) for (boolean empty : List.of(false, true)) {
            var baseline = open(root(list, empty), "probe.dart", "// user member\nfinal int retained=7;");
            var candidate = apply(baseline, new SetProperty(SLIVER_ID, p("itemExtent"),
                    new PropertyValue.DoubleValue(new java.math.BigDecimal(extent))));
            for (String flag : List.of("addAutomaticKeepAlives", "addRepaintBoundaries", "addSemanticIndexes"))
                candidate = apply(candidate, new SetProperty(SLIVER_ID, p(flag), new PropertyValue.BooleanValue(empty)));
            assertAnalysis(baseline, candidate, "probe.dart", true);
            var saved = reopen(candidate, source(candidate));
            assertEquals(candidate.current().fdSnapshot(), saved.current().fdSnapshot());
            assertTrue(source(saved).contains("final int retained=7;"));
            assertFalse(saved.apply(new ResetProperty(SLIVER_ID, p("itemExtent"))).changed());
            var restored = apply(saved, new SetProperty(SLIVER_ID, p("itemExtent"),
                    new PropertyValue.DoubleValue(new java.math.BigDecimal("48"))));
            assertAnalysis(saved, restored, "probe.dart", true);
            var undo = restored.undo(); assertTrue(undo.changed());
            assertArrayEquals(saved.current().dartCandidateBytes(), undo.session().current().dartCandidateBytes());
            var redo = undo.session().redo(); assertTrue(redo.changed());
            assertArrayEquals(restored.current().dartCandidateBytes(), redo.session().current().dartCandidateBytes());
            saveRuntime(saved, extent, empty ? null : "Fixed body");
        }
        var builder = SliverFixedExtentListWidgetPropertySchema.Kind.BUILDER;
        for (PropertyValue count : Arrays.asList(null, new PropertyValue.NullValue(),
                new PropertyValue.IntegerValue(java.math.BigInteger.ZERO),
                new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(3)))) {
            var baseline = open(root(builder, true), "probe.dart", "");
            var candidate = apply(baseline, new SetProperty(SLIVER_ID, p("itemExtent"),
                    new PropertyValue.DoubleValue(new java.math.BigDecimal("60"))));
            if (count != null) candidate = apply(candidate, new SetProperty(SLIVER_ID, p("itemCount"), count));
            assertAnalysis(baseline, candidate, "probe.dart", true);
            var saved = reopen(candidate, source(candidate));
            assertEquals(candidate.current().fdSnapshot(), saved.current().fdSnapshot());
            if (count != null) assertAnalysis(saved, apply(saved, new ResetProperty(SLIVER_ID, p("itemCount"))), "probe.dart", true);
            saveRuntime(saved, "60", null);
        }
        var delegate = SliverFixedExtentListWidgetPropertySchema.Kind.DELEGATE;
        var emptyDelegate = open(root(delegate, true), "probe.dart", "");
        saveRuntime(emptyDelegate, "48", null);
        for (var kind : List.of(builder, delegate)) {
            String field = kind == builder ? "itemBuilder" : "delegate";
            var baseline = open(root(kind, false), "probe.dart", "");
            for (String good : kind == builder ? List.of("item", "configuredItem", "makeItem")
                    : List.of("children", "makeChildren", "customChildren")) {
                var candidate = apply(baseline, new SetProperty(SLIVER_ID, p(field),
                        imported(good, Optional.empty(), good.startsWith("make"))));
                if (kind == builder) {
                    candidate = apply(candidate, new SetProperty(SLIVER_ID, p("findChildIndexCallback"), imported("indexes")));
                    candidate = apply(candidate, new SetProperty(SLIVER_ID, p("semanticIndexOffset"),
                            new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(4))));
                    // Exercise both finite count exceeding the builder's early-null boundary and unbounded count.
                    if (good.equals("configuredItem")) candidate = apply(candidate,
                            new SetProperty(SLIVER_ID, p("itemCount"), new PropertyValue.IntegerValue(java.math.BigInteger.TEN)));
                }
                assertAnalysis(baseline, candidate, "probe.dart", true);
                var saved = reopen(candidate, source(candidate));
                assertEquals(candidate.current().fdSnapshot(), saved.current().fdSnapshot());
                saveRuntime(saved, "48", good.equals("makeChildren") ? "Delegate item"
                        : good.equals("customChildren") ? "Custom item" : "Item 0");
                assertAnalysis(saved, apply(saved, new SetProperty(SLIVER_ID, p(field),
                        new PropertyValue.StringValue("empty"))), "probe.dart", true);
            }
            var adversarial = reopen(baseline, "// ignore_for_file: invalid_assignment, argument_type_not_assignable\n" + source(baseline));
            for (String bad : kind == builder ? List.of("dynamicItem", "wrongIndex", "asyncItem", "nullableItem")
                    : List.of("dynamicChildren", "wrongChildren", "nullableChildren"))
                assertAnalysis(adversarial, apply(adversarial, new SetProperty(SLIVER_ID, p(field), imported(bad))), "probe.dart", false);
            if (kind == builder) {
                for (String bad : List.of("dynamicIndexes", "wrongIndexes", "wrongArguments"))
                    assertAnalysis(adversarial, apply(adversarial, new SetProperty(SLIVER_ID,
                            p("findChildIndexCallback"), imported(bad))), "probe.dart", false);
                var nullable = apply(baseline, new SetProperty(SLIVER_ID, p("findChildIndexCallback"), imported("absentIndex")));
                assertAnalysis(baseline, nullable, "probe.dart", true);
                var saved = reopen(nullable, source(nullable));
                assertAnalysis(saved, apply(saved, new ResetProperty(SLIVER_ID, p("findChildIndexCallback"))), "probe.dart", true);
                saveRuntime(saved, "48", null);
            }
        }
        assertEquals(18, runtimeCases);
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("fixed_extent_test.dart"),
                "import 'package:flutter/material.dart';\nimport 'package:flutter_test/flutter_test.dart';\n" + imports
                + "void main(){for(final entry in <(String,Widget,double,String?)>[" + entries + "]) {\n"
                + """
                  testWidgets(entry.$1,(tester) async {
                    await tester.pumpWidget(MaterialApp(home:entry.$2)); await tester.pump();
                    final sdk = tester.widget<SliverFixedExtentList>(find.byType(SliverFixedExtentList,skipOffstage:false));
                    expect(sdk.itemExtent,entry.$3);
                    if(entry.$4 != null) expect(find.text(entry.$4!),entry.$3 == 0 ? findsNothing : findsOneWidget);
                    final index = int.parse(entry.$1.substring(5));
                    if(index < 6) {
                      final delegate = sdk.delegate as SliverChildListDelegate;
                      // Zero extent may avoid mounting children, but must not discard the saved list.
                      expect(delegate.children.length,index.isEven?1:0);
                      if(index.isEven) expect((delegate.children.single as Text).data,'Fixed body');
                      expect(delegate.addAutomaticKeepAlives,index.isOdd);
                      expect(delegate.addRepaintBoundaries,index.isOdd);
                      expect(delegate.addSemanticIndexes,index.isOdd);
                    }
                    if(index >= 11 && index <= 13) {
                      final delegate = sdk.delegate as SliverChildBuilderDelegate;
                      expect(delegate.semanticIndexOffset,4);
                      expect(delegate.findChildIndexCallback!(const ValueKey<int>(2)),2);
                      expect(delegate.builder(tester.element(find.byType(CustomScrollView)),4),isNull);
                      expect(delegate.childCount,index==12?10:isNull);
                    }
                    expect(tester.takeException(),isNull);
                  });
                }}
                """);
        run(List.of(flutter.toString(), "test", "--reporter", "expanded"), "flutter-test.log");
    }
    private void saveRuntime(DesignerCommandSession session, String extent, String expectedText) throws Exception {
        String name = "case_" + runtimeCases;
        Files.write(lib.resolve(name + ".dart"), session.current().dartCandidateBytes());
        imports.append("import '../lib/").append(name).append(".dart' as c").append(runtimeCases).append(";\n");
        entries.append("('").append(name).append("',const c").append(runtimeCases).append(".Sample(),")
                .append(extent).append(",").append(expectedText == null ? "null" : "'" + expectedText + "'").append("),\n");
        runtimeCases++;
    }
    private static PropertyName p(String name) { return new PropertyName(name); }
    private static WidgetNode box(String type, Map<PropertyName,PropertyValue> properties, WidgetNode child) {
        return new WidgetNode(StableId.random(), new WidgetTypeId(type), properties,
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child)));
    }
    private static WidgetNode root(SliverFixedExtentListWidgetPropertySchema.Kind kind, boolean empty) {
        var definition = BuiltInWidgetCatalog.getDefault().find(kind.type()).orElseThrow();
        var prototype = WidgetNodePrototypeFactory.create(definition, SLIVER_ID);
        var text = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(p("data"), new PropertyValue.StringValue("Fixed body")), Map.of());
        var sliver = new WidgetNode(prototype.id(), prototype.type(), prototype.properties(),
                kind.hasChildren() ? Map.of(new SlotName("children"), new WidgetSlot.ListSlot(empty ? List.of() : List.of(text))) : Map.of());
        var viewport = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.CustomScrollView"),
                Map.of(p("primary"), new PropertyValue.BooleanValue(false)),
                Map.of(new SlotName("slivers"), new WidgetSlot.ListSlot(List.of(sliver))));
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
