package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.dart.*;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.ValidationResult;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Generated AppBar predicates: exact proof, compatibility, nested scroll and source preservation. */
@EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
class AppBarNotificationPredicateRealSdkTest {
    @TempDir Path project;
    private static final StableId APP_BAR_ID = StableId.random();
    private static final PropertyName PREDICATE = new PropertyName("notificationPredicate");
    private static final String SIGNATURE = "ScrollNotificationPredicate";
    private Path sdk, flutter, dart, lib;

    @Test
    void predicatesPreservePresetsProveExactBoolResultsAndFilterNestedScrollWithoutConsumption() throws Exception {
        initialize();
        Files.writeString(lib.resolve("predicates.dart"), """
                import 'package:flutter/material.dart';
                final seen = <ScrollNotification>[];
                int selectedDepth = 1;
                bool custom(ScrollNotification n) { seen.add(n); return n.depth == selectedDepth; }
                bool acceptAll(ScrollNotification n) => true;
                bool broaderObject(Object n) => true;
                bool broaderNotification(Notification n) => true;
                ScrollNotificationPredicate get configured => custom;
                ScrollNotificationPredicate makePredicate() => custom;
                class Predicates { static bool member(ScrollNotification n) => custom(n); }
                class Holder { bool member(ScrollNotification n) => custom(n); }
                final holder = Holder();
                int wrongResult(ScrollNotification n) => 1;
                bool? nullableResult(ScrollNotification n) => true;
                Object? broadResult(ScrollNotification n) => true;
                dynamic dynamicResult(ScrollNotification n) => true;
                Future<bool> asyncResult(ScrollNotification n) async => true;
                bool narrowParameter(ScrollUpdateNotification n) => true;
                dynamic get dynamicPredicate => custom;
                ScrollNotificationPredicate? get nullablePredicate => custom;
                ScrollNotificationPredicate? nullableFactory() => custom;
                """);
        var root = scaffold();
        var baseline = open(root, "custom.dart", "");
        for (PropertyValue reference : List.of(imported("custom"), imported("acceptAll"), imported("broaderObject"),
                imported("broaderNotification"), imported("configured"), imported("makePredicate", Optional.empty(), true),
                imported("Predicates", Optional.of("member"), false), imported("holder", Optional.of("member"), false))) {
            assertAnalysis(baseline, apply(baseline, new SetProperty(APP_BAR_ID, PREDICATE, reference)), "custom.dart", true);
        }
        var custom = apply(baseline, new SetProperty(APP_BAR_ID, PREDICATE, imported("custom")));
        Files.write(lib.resolve("custom.dart"), custom.current().dartCandidateBytes());
        Files.write(lib.resolve("omitted.dart"), open(root, "omitted.dart", "").current().dartCandidateBytes());
        for (String preset : List.of("default", "depthZero", "all")) {
            String file = "preset_" + preset.toLowerCase(java.util.Locale.ROOT) + ".dart";
            var presetBaseline = open(root, file, "");
            Files.write(lib.resolve(file), apply(presetBaseline,
                    new SetProperty(APP_BAR_ID, PREDICATE, new PropertyValue.StringValue(preset))).current().dartCandidateBytes());
        }
        String ignored = "// ignore_for_file: argument_type_not_assignable, invalid_assignment\n";
        var adversarial = reopen(baseline, ignored + source(baseline));
        for (String name : List.of("wrongResult", "nullableResult", "broadResult", "dynamicResult", "asyncResult",
                "narrowParameter", "dynamicPredicate", "nullablePredicate")) {
            assertAnalysis(adversarial, apply(adversarial, new SetProperty(APP_BAR_ID, PREDICATE, imported(name))), "custom.dart", false);
        }
        assertAnalysis(adversarial, apply(adversarial, new SetProperty(APP_BAR_ID, PREDICATE,
                imported("nullableFactory", Optional.empty(), true))), "custom.dart", false);
        for (PropertyValue invalid : List.of(new PropertyValue.NullValue(), new PropertyValue.CallbackValue("custom"),
                new PropertyValue.StringValue("(notification) => true"))) {
            var rejected = baseline.apply(new SetProperty(APP_BAR_ID, PREDICATE, invalid));
            assertFalse(rejected.changed());
            assertArrayEquals(baseline.current().fdSnapshot().copyBytes(), rejected.session().current().fdSnapshot().copyBytes());
        }
        Files.write(lib.resolve("custom.dart"), custom.current().dartCandidateBytes());

        var localBaseline = open(root, "local.dart", """
                  bool localPredicate(ScrollNotification notification) {
                    // User-owned Привіт.
                    return notification.depth == 1;
                  }
                """);
        var local = apply(localBaseline, new SetProperty(APP_BAR_ID, PREDICATE,
                new PropertyValue.DartObjectReferenceValue(Optional.empty(), "localPredicate", Optional.empty(),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty())));
        assertAnalysis(localBaseline, local, "local.dart", true);
        var reset = apply(reopen(local, source(local)), new ResetProperty(APP_BAR_ID, PREDICATE));
        assertFalse(source(reset).contains("notificationPredicate:"));
        assertTrue(source(reset).contains("// User-owned Привіт."));
        var undo = reset.undo(); assertTrue(undo.changed());
        assertArrayEquals(local.current().dartCandidateBytes(), undo.session().current().dartCandidateBytes());
        assertTrue(undo.session().redo().changed());

        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("predicate_test.dart"), """
                import 'package:flutter/material.dart';
                import 'package:flutter_test/flutter_test.dart';
                import 'package:appbar_predicate_contract/predicates.dart' as predicates;
                import 'package:appbar_predicate_contract/custom.dart' as custom;
                import 'package:appbar_predicate_contract/omitted.dart' as omitted;
                import 'package:appbar_predicate_contract/preset_default.dart' as defaults;
                import 'package:appbar_predicate_contract/preset_depthzero.dart' as depthZero;
                import 'package:appbar_predicate_contract/preset_all.dart' as all;
                double elevation(WidgetTester tester) => tester.widget<Material>(
                  find.descendant(of: find.byType(AppBar), matching: find.byType(Material)).first).elevation;
                Future<void> scroll(WidgetTester tester, String marker, double pixels) async {
                  // Sliver rows can be unmounted after scrolling. The two Scrollable states
                  // themselves remain mounted: outer first, nested second in this fixture.
                  final scrollables = find.descendant(of: find.byType(Scaffold), matching: find.byType(Scrollable));
                  expect(scrollables, findsNWidgets(2));
                  tester.state<ScrollableState>(scrollables.at(marker == 'inner marker' ? 1 : 0)).position.jumpTo(pixels);
                  await tester.pumpAndSettle();
                }
                void main() {
                  testWidgets('omission and legacy depth-zero presets ignore nested scroll but accept outer updates', (tester) async {
                    for (final sample in [const omitted.Sample(), const defaults.Sample(), const depthZero.Sample()]) {
                      await tester.pumpWidget(MaterialApp(theme: ThemeData(useMaterial3: true), home: sample));
                      await tester.pumpAndSettle();
                      expect(elevation(tester), 1);
                      await scroll(tester, 'inner marker', 100);
                      expect(elevation(tester), 1);
                      await scroll(tester, 'outer marker', 100);
                      expect(elevation(tester), 9);
                      await scroll(tester, 'outer marker', 0);
                      expect(elevation(tester), 1);
                      expect(tester.takeException(), isNull);
                    }
                  });
                  testWidgets('all legacy preset accepts actual nested notifications', (tester) async {
                    await tester.pumpWidget(MaterialApp(theme: ThemeData(useMaterial3: true), home: const all.Sample()));
                    await tester.pumpAndSettle();
                    await scroll(tester, 'inner marker', 100);
                    expect(elevation(tester), 9);
                    await scroll(tester, 'inner marker', 0);
                    expect(elevation(tester), 1);
                    expect(tester.takeException(), isNull);
                  });
                  testWidgets('custom predicate filters elevation and never consumes notifications', (tester) async {
                    predicates.seen.clear(); predicates.selectedDepth = 1;
                    final observed = <ScrollNotification>[];
                    await tester.pumpWidget(MaterialApp(theme: ThemeData(useMaterial3: true), home:
                      NotificationListener<ScrollNotification>(onNotification: (n) { observed.add(n); return false; },
                        child: const custom.Sample())));
                    await tester.pumpAndSettle();
                    await scroll(tester, 'outer marker', 100);
                    expect(elevation(tester), 1);
                    expect(predicates.seen.any((n) => n.depth == 0), isTrue);
                    expect(observed.whereType<ScrollUpdateNotification>(), isNotEmpty);
                    // A notification's depth can grow as it continues bubbling; capture in the predicate too.
                    final calls = predicates.seen.length;
                    final propagated = observed.length;
                    await scroll(tester, 'inner marker', 100);
                    expect(predicates.seen.length, greaterThan(calls));
                    expect(observed.length, greaterThan(propagated));
                    expect(elevation(tester), 9);
                    await scroll(tester, 'inner marker', 0);
                    expect(elevation(tester), 1);
                    expect(tester.takeException(), isNull);
                  });
                }
                """);
        run(List.of(flutter.toString(), "test", "--reporter", "expanded"), "flutter-test.log");
    }

    private void initialize() throws Exception {
        sdk = Path.of(System.getProperty("flutter.events.sdk")).toRealPath();
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        flutter = sdk.resolve(windows ? "bin/flutter.bat" : "bin/flutter");
        dart = sdk.resolve(windows ? "bin/cache/dart-sdk/bin/dart.exe" : "bin/cache/dart-sdk/bin/dart");
        Files.writeString(project.resolve("pubspec.yaml"), """
                name: appbar_predicate_contract
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
        assertTrue(ticket.request().symbolProbes().stream().anyMatch(probe -> probe.staticTypeProbe()
                .filter(type -> type.expectedDartType().equals(SIGNATURE)).isPresent()), ticket.request().toString());
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
    private static WidgetNode scaffold() {
        var appBar = new WidgetNode(APP_BAR_ID, new WidgetTypeId("flutter.material.AppBar"),
                Map.of(new PropertyName("elevation"), number(1), new PropertyName("scrolledUnderElevation"), number(9)),
                Map.of(new SlotName("title"), WidgetSlot.SingleSlot.of(text("Toolbar"))));
        var inner = list(text("inner marker"), box(900, null));
        var outer = list(text("outer marker"), box(220, inner), box(1300, null));
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.material.Scaffold"), Map.of(),
                Map.of(new SlotName("appBar"), WidgetSlot.SingleSlot.of(appBar),
                        new SlotName("body"), WidgetSlot.SingleSlot.of(outer)));
    }
    private static WidgetNode text(String value) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue(value)), Map.of());
    }
    private static WidgetNode list(WidgetNode... children) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.ListView"),
                Map.of(new PropertyName("primary"), new PropertyValue.BooleanValue(false)),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(children))));
    }
    private static WidgetNode box(int height, WidgetNode child) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.SizedBox"),
                Map.of(new PropertyName("height"), number(height)),
                child == null ? Map.of() : Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child)));
    }
    private static PropertyValue number(int value) { return new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(value)); }
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
        return new PropertyValue.DartObjectReferenceValue(Optional.of("package:appbar_predicate_contract/predicates.dart"), name, member,
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
