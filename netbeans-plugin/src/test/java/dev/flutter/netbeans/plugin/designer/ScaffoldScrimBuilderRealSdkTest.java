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

/** Exact generated Scaffold code and SDK behavior, including anonymous-function proof. */
@EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
class ScaffoldScrimBuilderRealSdkTest {
    @TempDir Path project;
    private static final WidgetTypeId TYPE = new WidgetTypeId("flutter.material.Scaffold");
    private static final PropertyName BUILDER = new PropertyName("bottomSheetScrimBuilder");
    private static final String SIGNATURE = "Widget? Function(BuildContext, Animation<double>)";
    private Path sdk, flutter, dart, lib;

    @Test
    void anonymousFunctionProofPreservesDefaultsNullableResultsReferencesAndRuntimeAnimation() throws Exception {
        initialize();
        Files.writeString(lib.resolve("builders.dart"), """
                import 'package:flutter/material.dart';
                typedef Scrim = Widget? Function(BuildContext, Animation<double>);
                final contexts = <BuildContext>[];
                final animations = <Animation<double>>[];
                bool hide = false;
                Widget? custom(BuildContext context, Animation<double> animation) {
                  contexts.add(context); animations.add(animation);
                  if (hide) return null;
                  return AnimatedBuilder(animation: animation, builder: (context, child) =>
                    Opacity(key: const ValueKey('custom-scrim'), opacity: animation.value,
                      child: const IgnorePointer(child: ColoredBox(color: Colors.green))));
                }
                Widget concrete(BuildContext context, Animation<double> animation) => const SizedBox();
                Null noScrim(BuildContext context, Animation<double> animation) => null;
                Widget? broader(Object context, Animation<num> animation) => const SizedBox();
                Scrim get configured => custom;
                Scrim makeBuilder() => custom;
                class Builders { static Widget? member(BuildContext c, Animation<double> a) => custom(c, a); }
                class Holder { Widget? member(BuildContext c, Animation<double> a) => custom(c, a); }
                final holder = Holder();
                int wrongResult(BuildContext context, Animation<double> animation) => 1;
                Object? broadResult(BuildContext context, Animation<double> animation) => const SizedBox();
                dynamic dynamicResult(BuildContext context, Animation<double> animation) => const SizedBox();
                Future<Widget?> asyncResult(BuildContext context, Animation<double> animation) async => null;
                Widget? wrongContext(String context, Animation<double> animation) => null;
                Widget? wrongAnimation(BuildContext context, Animation<int> animation) => null;
                dynamic get dynamicBuilder => custom;
                Scrim? get nullableBuilder => custom;
                Widget? Function(BuildContext, Animation<double>)? nullableFactory() => custom;
                """);
        var root = scaffold();
        var baseline = open(root, "custom.dart", "");
        for (PropertyValue reference : List.of(imported("custom"), imported("concrete"), imported("noScrim"),
                imported("broader"), imported("configured"), imported("makeBuilder", Optional.empty(), true),
                imported("Builders", Optional.of("member"), false), imported("holder", Optional.of("member"), false))) {
            assertAnalysis(baseline, apply(baseline, new SetProperty(root.id(), BUILDER, reference)), "custom.dart", true);
        }
        var custom = apply(baseline, new SetProperty(root.id(), BUILDER, imported("custom")));
        Files.write(lib.resolve("custom.dart"), custom.current().dartCandidateBytes());
        Files.write(lib.resolve("default_scrim.dart"), open(root, "default_scrim.dart", "").current().dartCandidateBytes());
        var noneBaseline = open(root, "no_scrim.dart", "");
        Files.write(lib.resolve("no_scrim.dart"), apply(noneBaseline,
                new SetProperty(root.id(), BUILDER, imported("noScrim"))).current().dartCandidateBytes());

        String ignored = "// ignore_for_file: argument_type_not_assignable, invalid_assignment\n";
        var adversarial = reopen(baseline, ignored + source(baseline));
        for (String name : List.of("wrongResult", "broadResult", "dynamicResult", "asyncResult", "wrongContext",
                "wrongAnimation", "dynamicBuilder", "nullableBuilder")) {
            assertAnalysis(adversarial, apply(adversarial, new SetProperty(root.id(), BUILDER, imported(name))), "custom.dart", false);
        }
        assertAnalysis(adversarial, apply(adversarial, new SetProperty(root.id(), BUILDER,
                imported("nullableFactory", Optional.empty(), true))), "custom.dart", false);
        var rejectNull = baseline.apply(new SetProperty(root.id(), BUILDER, new PropertyValue.NullValue()));
        assertFalse(rejectNull.changed());
        assertArrayEquals(baseline.current().fdSnapshot().copyBytes(), rejectNull.session().current().fdSnapshot().copyBytes());
        Files.write(lib.resolve("custom.dart"), custom.current().dartCandidateBytes());

        // Existing user-owned function remains intact through editing, reset and history.
        var localBaseline = open(root, "local.dart", """
                  Widget? localScrim(BuildContext context, Animation<double> animation) {
                    // User-owned Привіт.
                    return null;
                  }
                """);
        var local = apply(localBaseline, new SetProperty(root.id(), BUILDER,
                new PropertyValue.DartObjectReferenceValue(Optional.empty(), "localScrim", Optional.empty(),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty())));
        assertAnalysis(localBaseline, local, "local.dart", true);
        var reopened = reopen(local, source(local));
        var reset = apply(reopened, new ResetProperty(root.id(), BUILDER));
        assertFalse(source(reset).contains("bottomSheetScrimBuilder:"));
        assertTrue(source(reset).contains("// User-owned Привіт."));
        var undo = reset.undo();
        assertTrue(undo.changed());
        assertArrayEquals(local.current().dartCandidateBytes(), undo.session().current().dartCandidateBytes());
        assertTrue(undo.session().redo().changed());

        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("scrim_test.dart"), """
                import 'package:flutter/material.dart';
                import 'package:flutter_test/flutter_test.dart';
                import 'package:scrim_contract/builders.dart' as builders;
                import 'package:scrim_contract/custom.dart' as custom;
                import 'package:scrim_contract/default_scrim.dart' as defaults;
                import 'package:scrim_contract/no_scrim.dart' as none;
                Finder scrimBarriers() => find.descendant(of: find.byType(Scaffold), matching: find.byType(ModalBarrier));
                void main() {
                  testWidgets('omission preserves non-dismissable animated SDK barrier', (tester) async {
                    await tester.pumpWidget(const MaterialApp(home: defaults.Sample()));
                    final state = tester.state<ScaffoldState>(find.byType(Scaffold));
                    for (final value in [0.0, 0.5, 1.0]) {
                      state.showBodyScrim(true, value); await tester.pump();
                      final barrier = tester.widget<ModalBarrier>(scrimBarriers());
                      expect(barrier.dismissible, isFalse);
                      final expected = value == 1.0 ? 0.6 : value == 0.5 ? 0.15 : 0.1;
                      expect(barrier.color!.a, closeTo(expected, 0.005));
                    }
                    state.showBodyScrim(false, 0); await tester.pump();
                    expect(scrimBarriers(), findsNothing);
                    expect(tester.takeException(), isNull);
                  });
                  testWidgets('generated project builder receives real context and animated coverage', (tester) async {
                    builders.hide = false; builders.contexts.clear(); builders.animations.clear();
                    await tester.pumpWidget(const MaterialApp(home: custom.Sample()));
                    final state = tester.state<ScaffoldState>(find.byType(Scaffold));
                    expect(builders.contexts, isEmpty);
                    for (final value in [0.0, 0.5, 1.0]) {
                      state.showBodyScrim(true, value); await tester.pump();
                      expect(builders.contexts.last.mounted, isTrue);
                      expect(builders.animations.last.value, value);
                      expect(tester.widget<Opacity>(find.byKey(const ValueKey('custom-scrim'))).opacity, value);
                      expect(scrimBarriers(), findsNothing);
                    }
                    state.showBodyScrim(false, 0); await tester.pump();
                    expect(find.byKey(const ValueKey('custom-scrim')), findsNothing);
                    builders.hide = true;
                    state.showBodyScrim(true, 1); await tester.pump();
                    expect(find.byKey(const ValueKey('custom-scrim')), findsNothing);
                    expect(scrimBarriers(), findsNothing);
                    expect(tester.takeException(), isNull);
                  });
                  testWidgets('returning null suppresses scrim without a null callback', (tester) async {
                    await tester.pumpWidget(const MaterialApp(home: none.Sample()));
                    tester.state<ScaffoldState>(find.byType(Scaffold)).showBodyScrim(true, 1);
                    await tester.pump();
                    expect(scrimBarriers(), findsNothing);
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
                name: scrim_contract
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
        var child = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue("body")), Map.of());
        return new WidgetNode(StableId.random(), TYPE, Map.of(), Map.of(new SlotName("body"), WidgetSlot.SingleSlot.of(child)));
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
        return new PropertyValue.DartObjectReferenceValue(Optional.of("package:scrim_contract/builders.dart"), name, member,
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
