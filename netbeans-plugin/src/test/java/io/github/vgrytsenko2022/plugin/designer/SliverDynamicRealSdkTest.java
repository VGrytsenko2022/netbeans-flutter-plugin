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
class SliverDynamicRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;
    private static final StableId SLIVER_ID = StableId.random();
    @Test void allConstructorsAndTypedReferencesPassRealCandidateGateAndRejectUnsafeSignatures() throws Exception {
        initialize();
        Files.writeString(lib.resolve("builders.dart"), """
                import 'package:flutter/widgets.dart';
                Widget? item(BuildContext context, int index) => index < 4 ? Text('Item $index') : null;
                Widget separator(BuildContext context, int index) => const SizedBox(height: 5);
                int? index(Key key) => key is ValueKey<int> ? key.value : null;
                ChildIndexGetter? get absentIndex => null;
                NullableIndexedWidgetBuilder get configuredItem => item;
                NullableIndexedWidgetBuilder makeItem() => item;
                SliverChildDelegate get children => SliverChildBuilderDelegate(item, childCount: 4);
                SliverChildDelegate makeChildren() => SliverChildListDelegate(const [Text('Delegate item')]);
                SliverGridDelegate get grid => const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 3);
                SliverGridDelegate makeGrid() => const SliverGridDelegateWithMaxCrossAxisExtent(maxCrossAxisExtent: 150);
                dynamic dynamicItem(BuildContext c, int i) => null;
                Widget? wrongIndex(BuildContext c, String i) => null;
                Future<Widget?> asyncItem(BuildContext c, int i) async => null;
                Widget? nullableSeparator(BuildContext c, int i) => null;
                dynamic dynamicIndex(Key key) => 0;
                double wrongResult(Key key) => 0;
                dynamic get dynamicGetter => item;
                dynamic get dynamicChildren => children;
                dynamic get dynamicGrid => grid;
                """);
        for (var kind : SliverDynamicWidgetPropertySchema.Kind.values()) {
            var baseline = open(root(kind, Map.of()), "probe.dart", "");
            var first = SliverDynamicWidgetPropertySchema.fields(kind).getFirst().name();
            var good = new LinkedHashMap<PropertyName, PropertyValue>();
            for (var field : SliverDynamicWidgetPropertySchema.fields(kind)) {
                if (field.required()) good.put(p(field.name()), imported(switch (field.name()) {
                    case "itemBuilder" -> "item"; case "separatorBuilder" -> "separator";
                    case "delegate" -> "children"; default -> "grid";
                }));
            }
            var candidate = baseline;
            for (var entry : good.entrySet()) candidate = apply(candidate, new SetProperty(SLIVER_ID, entry.getKey(), entry.getValue()));
            assertAnalysis(baseline, candidate, "probe.dart", true);
            // Candidate save/reopen retains all fields and source outside managed regions.
            var reopened = reopen(candidate, source(candidate));
            assertEquals(candidate.current().fdSnapshot(), reopened.current().fdSnapshot());
            Files.write(lib.resolve(kind.name().toLowerCase() + ".dart"), candidate.current().dartCandidateBytes());
            var adversarial = reopen(baseline, "// ignore_for_file: invalid_assignment, argument_type_not_assignable\n" + source(baseline));
            for (var field : SliverDynamicWidgetPropertySchema.fields(kind)) {
                List<String> bad = switch (field.name()) {
                    case "itemBuilder" -> List.of("dynamicItem", "wrongIndex", "asyncItem", "dynamicGetter");
                    case "separatorBuilder" -> List.of("nullableSeparator", "dynamicItem");
                    case "delegate" -> List.of("dynamicChildren");
                    case "gridDelegate" -> List.of("dynamicGrid");
                    case "findChildIndexCallback", "findItemIndexCallback" -> List.of("dynamicIndex", "wrongResult");
                    default -> List.of();
                };
                for (String name : bad) assertAnalysis(adversarial,
                        apply(adversarial, new SetProperty(SLIVER_ID, p(field.name()), imported(name))), "probe.dart", false);
                if (field.type().equals("ChildIndexGetter?")) {
                    for (String name : List.of("index", "absentIndex")) assertAnalysis(baseline,
                            apply(baseline, new SetProperty(SLIVER_ID, p(field.name()), imported(name))), "probe.dart", true);
                }
            }
            if (good.containsKey(p("itemBuilder"))) {
                for (boolean factory : List.of(false, true)) assertAnalysis(baseline, apply(baseline,
                        new SetProperty(SLIVER_ID, p("itemBuilder"),
                                imported(factory ? "makeItem" : "configuredItem", Optional.empty(), factory))), "probe.dart", true);
            } else {
                assertAnalysis(baseline, apply(baseline, new SetProperty(SLIVER_ID, p(first),
                        imported(first.equals("delegate") ? "makeChildren" : "makeGrid", Optional.empty(), true))), "probe.dart", true);
            }
            // Return one changed property to its preset: all generated helper symbols
            // must navigate to the real SDK, not merely compile as candidate text.
            var changedPreset = apply(baseline, new SetProperty(SLIVER_ID, p(first), good.get(p(first))));
            // Reopen makes the reference the saved baseline. Returning directly to
            // the original unsaved preset correctly has no pair to persist.
            changedPreset = reopen(changedPreset, source(changedPreset));
            var restoredPreset = apply(changedPreset, new SetProperty(SLIVER_ID, p(first),
                    new PropertyValue.StringValue(SliverDynamicWidgetPropertySchema.fields(kind).getFirst().preset())));
            assertAnalysis(changedPreset, restoredPreset, "probe.dart", true);
            if (good.containsKey(p("gridDelegate"))) assertAnalysis(baseline,
                    apply(baseline, new SetProperty(SLIVER_ID, p("gridDelegate"), new PropertyValue.StringValue("maxExtent"))),
                    "probe.dart", true);
            Files.write(lib.resolve("preset_" + kind.name().toLowerCase() + ".dart"), baseline.current().dartCandidateBytes());
        }
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("sliver_test.dart"), """
                import 'package:flutter/material.dart';
                import 'package:flutter_test/flutter_test.dart';
                import '../lib/list_builder.dart' as lb;
                import '../lib/list_separated.dart' as ls;
                import '../lib/list_delegate.dart' as ld;
                import '../lib/grid_builder.dart' as gb;
                import '../lib/grid_list.dart' as gl;
                import '../lib/grid_delegate.dart' as gd;
                void main() {
                  for (final entry in <String, Widget>{
                    'list builder': const lb.Sample(), 'list separated': const ls.Sample(),
                    'list delegate': const ld.Sample(), 'grid builder': const gb.Sample(),
                    'grid list': const gl.Sample(), 'grid delegate': const gd.Sample(),
                  }.entries) {
                    testWidgets(entry.key, (tester) async {
                      await tester.pumpWidget(MaterialApp(home: SizedBox(width: 300, height: 200, child: entry.value)));
                      await tester.pumpAndSettle();
                      expect(find.text('Item 0'), entry.key == 'grid list' ? findsNothing : findsOneWidget);
                      if (entry.key == 'grid list') expect(find.text('Static child'), findsOneWidget);
                      expect(tester.takeException(), isNull);
                    });
                  }
                }
                """);
        run(List.of(flutter.toString(), "test", "--reporter", "expanded"), "flutter-test.log");
    }
    private static PropertyName p(String name) { return new PropertyName(name); }
    private static WidgetNode root(SliverDynamicWidgetPropertySchema.Kind kind, Map<PropertyName, PropertyValue> properties) {
        var d = BuiltInWidgetCatalog.getDefault().find(kind.type()).orElseThrow();
        var node = WidgetNodePrototypeFactory.create(d, SLIVER_ID);
        var values = new LinkedHashMap<>(node.properties()); values.putAll(properties);
        var slots = new LinkedHashMap<>(node.slots());
        if (kind.hasChildren()) slots.put(new SlotName("children"), new WidgetSlot.ListSlot(List.of(
                new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                        Map.of(p("data"), new PropertyValue.StringValue("Static child")), Map.of()))));
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.CustomScrollView"),
                Map.of(p("primary"), new PropertyValue.BooleanValue(false)), Map.of(new SlotName("slivers"),
                        new WidgetSlot.ListSlot(List.of(new WidgetNode(node.id(), node.type(), values, slots)))));
    }
    private void initialize() throws Exception {
        sdk = Path.of(System.getProperty("flutter.events.sdk")).toRealPath();
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        flutter = sdk.resolve(windows ? "bin/flutter.bat" : "bin/flutter");
        dart = sdk.resolve(windows ? "bin/cache/dart-sdk/bin/dart.exe" : "bin/cache/dart-sdk/bin/dart");
        Files.writeString(project.resolve("pubspec.yaml"), """
                name: sliver_dynamic_contract
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
        return new PropertyValue.DartObjectReferenceValue(Optional.of("package:sliver_dynamic_contract/builders.dart"), name, member,
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

