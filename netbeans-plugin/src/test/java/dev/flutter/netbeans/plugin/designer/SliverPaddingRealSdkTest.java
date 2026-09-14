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
class SliverPaddingRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;
    private static final StableId SLIVER_ID = StableId.random();
    @Test void completeConstructorAndGeometryReferencesPassCandidateGateAndRunAfterReopen() throws Exception {
        initialize();
        Files.writeString(lib.resolve("builders.dart"), """
                import 'package:flutter/widgets.dart';
                const EdgeInsetsGeometry physical = EdgeInsets.all(8);
                EdgeInsetsGeometry get directional => const EdgeInsetsDirectional.fromSTEB(12, 4, 20, 6);
                EdgeInsetsGeometry mixed() => physical.add(directional);
                class Insets {
                  static EdgeInsetsGeometry get configured => directional;
                  static EdgeInsetsGeometry create() => mixed();
                }
                dynamic get unsafe => physical;
                EdgeInsetsGeometry? get nullable => physical;
                String get wrong => 'not insets';
                Future<EdgeInsetsGeometry> asynchronous() async => physical;
                """);
        var baseline = open(root(false), "probe.dart", "");
        var zero = java.math.BigDecimal.ZERO;
        var physical = new PropertyValue.EdgeInsetsValue(java.math.BigDecimal.valueOf(8), zero, zero, zero);
        var directional = new PropertyValue.EdgeInsetsDirectionalValue(java.math.BigDecimal.valueOf(12),
                java.math.BigDecimal.valueOf(4), java.math.BigDecimal.valueOf(20), java.math.BigDecimal.valueOf(6));
        var examples = new LinkedHashMap<String, PropertyValue>();
        examples.put("physical", physical);
        examples.put("directional", directional);
        examples.put("reference", imported("mixed", Optional.empty(), true));
        examples.put("empty", physical);
        for (var entry : examples.entrySet()) {
            var base = entry.getKey().equals("empty") ? open(root(true), "probe.dart", "") : baseline;
            var candidate = apply(base, new SetProperty(SLIVER_ID, p("padding"), entry.getValue()));
            assertAnalysis(base, candidate, "probe.dart", true);
            var reopened = reopen(candidate, source(candidate));
            assertEquals(candidate.current().fdSnapshot(), reopened.current().fdSnapshot());
            assertTrue(source(reopened).contains("SliverPadding("));
            Files.write(lib.resolve(entry.getKey() + ".dart"), reopened.current().dartCandidateBytes());
        }
        for (PropertyValue value : List.of(imported("physical"), imported("directional"),
                imported("Insets", Optional.of("configured"), false),
                imported("Insets", Optional.of("create"), true))) {
            assertAnalysis(baseline, apply(baseline, new SetProperty(SLIVER_ID, p("padding"), value)), "probe.dart", true);
        }
        var local = open(root(false), "probe.dart",
                "EdgeInsetsGeometry get _insets => const EdgeInsetsDirectional.all(5);\n"
                + "EdgeInsetsGeometry _createInsets() => _insets;");
        for (boolean factory : List.of(false, true)) {
            var value = new PropertyValue.DartObjectReferenceValue(Optional.empty(),
                    factory ? "_createInsets" : "_insets", Optional.empty(),
                    factory ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION
                            : PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                    factory ? Optional.of(false) : Optional.empty());
            assertAnalysis(local, apply(local, new SetProperty(SLIVER_ID, p("padding"), value)), "probe.dart", true);
        }
        var adversarial = reopen(baseline,
                "// ignore_for_file: argument_type_not_assignable, invalid_assignment\n" + source(baseline));
        for (String name : List.of("unsafe", "nullable", "wrong", "asynchronous")) {
            assertAnalysis(adversarial, apply(adversarial, new SetProperty(SLIVER_ID, p("padding"),
                    imported(name, Optional.empty(), name.equals("asynchronous")))), "probe.dart", false);
        }
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("padding_test.dart"), """
                import 'package:flutter/material.dart';
                import 'package:flutter_test/flutter_test.dart';
                import '../lib/physical.dart' as physical;
                import '../lib/directional.dart' as directional;
                import '../lib/reference.dart' as reference;
                import '../lib/empty.dart' as empty;
                void main() {
                  for (final rtl in [false, true]) {
                    for (final entry in <String, Widget>{
                      'physical': const physical.Sample(), 'directional': const directional.Sample(),
                      'reference': const reference.Sample(), 'empty': const empty.Sample(),
                    }.entries) {
                      testWidgets('${entry.key} rtl=$rtl', (tester) async {
                        await tester.pumpWidget(MaterialApp(home: Directionality(
                          textDirection: rtl ? TextDirection.rtl : TextDirection.ltr,
                          child: SizedBox(width: 300, height: 200, child: entry.value))));
                        await tester.pumpAndSettle();
                        final values = tester.widgetList<SliverPadding>(find.byType(SliverPadding, skipOffstage: false)).toList();
                        expect(values, hasLength(entry.key == 'empty' ? 1 : 2));
                        final insets = values.first.padding.resolve(rtl ? TextDirection.rtl : TextDirection.ltr);
                        expect(insets.left, entry.key == 'directional' ? (rtl ? 20 : 12)
                            : entry.key == 'reference' ? (rtl ? 28 : 20) : 8);
                        expect(find.text('Nested child'), entry.key == 'empty' ? findsNothing : findsOneWidget);
                        expect(tester.takeException(), isNull);
                      });
                    }
                  }
                }
                """);
        run(List.of(flutter.toString(), "test", "--reporter", "expanded"), "flutter-test.log");
    }
    private static PropertyName p(String name) { return new PropertyName(name); }
    private static WidgetNode root(boolean empty) {
        var d = BuiltInWidgetCatalog.getDefault().find(SliverPaddingWidgetPropertySchema.TYPE).orElseThrow();
        var outer = WidgetNodePrototypeFactory.create(d, SLIVER_ID);
        if (!empty) {
            var text = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                    Map.of(p("data"), new PropertyValue.StringValue("Nested child")), Map.of());
            var adapter = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.SliverToBoxAdapter"),
                    Map.of(), Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text)));
            var nested = WidgetNodePrototypeFactory.create(d, StableId.random());
            nested = new WidgetNode(nested.id(), nested.type(), nested.properties(),
                    Map.of(new SlotName("sliver"), WidgetSlot.SingleSlot.of(adapter)));
            outer = new WidgetNode(outer.id(), outer.type(), outer.properties(),
                    Map.of(new SlotName("sliver"), WidgetSlot.SingleSlot.of(nested)));
        }
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.CustomScrollView"),
                Map.of(p("primary"), new PropertyValue.BooleanValue(false)), Map.of(new SlotName("slivers"),
                        new WidgetSlot.ListSlot(List.of(outer))));
    }
    private void initialize() throws Exception {
        sdk = Path.of(System.getProperty("flutter.events.sdk")).toRealPath();
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        flutter = sdk.resolve(windows ? "bin/flutter.bat" : "bin/flutter");
        dart = sdk.resolve(windows ? "bin/cache/dart-sdk/bin/dart.exe" : "bin/cache/dart-sdk/bin/dart");
        Files.writeString(project.resolve("pubspec.yaml"), """
                name: sliver_padding_contract
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
        return new PropertyValue.DartObjectReferenceValue(Optional.of("package:sliver_padding_contract/builders.dart"), name, member,
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


