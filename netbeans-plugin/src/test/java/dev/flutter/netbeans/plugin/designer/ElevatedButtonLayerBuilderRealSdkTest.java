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

/** Exact SDK layer types, style composition, runtime states/clipping and source preservation. */
@EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
class ElevatedButtonLayerBuilderRealSdkTest {
    @TempDir Path project;
    private static final StableId BUTTON_ID = StableId.random();
    private static final PropertyName BACKGROUND = new PropertyName("styleBackgroundBuilder");
    private static final PropertyName FOREGROUND = new PropertyName("styleForegroundBuilder");
    private static final String SIGNATURE = "ButtonLayerBuilder";
    private Path sdk, flutter, dart, lib;

    @Test
    void exactLayerProofsAndGeneratedButtonsPreserveChildStatesClippingAndThemeInheritance() throws Exception {
        initialize();
        Files.writeString(lib.resolve("layers.dart"), """
                import 'package:flutter/material.dart';
                final calls = <String>[];
                final backgroundStates = <Set<WidgetState>>[];
                final foregroundStates = <Set<WidgetState>>[];
                bool foregroundSawNull = false;
                Widget background(BuildContext context, Set<WidgetState> states, Widget? child) {
                  calls.add('background'); backgroundStates.add(Set.of(states));
                  return ColoredBox(key: const ValueKey('layer-background'), color: Colors.green,
                    child: child ?? const SizedBox());
                }
                Widget foreground(BuildContext context, Set<WidgetState> states, Widget? child) {
                  calls.add('foreground'); foregroundStates.add(Set.of(states));
                  foregroundSawNull = child == null;
                  return ColoredBox(key: const ValueKey('layer-foreground'), color: Colors.blue,
                    child: child ?? const SizedBox(width: 20, height: 10, key: ValueKey('empty-layer-child')));
                }
                Widget broaderContext(Object context, Set<WidgetState> states, Widget? child) => child ?? const SizedBox();
                Widget broaderStates(BuildContext context, Set<Object> states, Object? child) => const SizedBox();
                ButtonLayerBuilder get configured => background;
                ButtonLayerBuilder makeLayer() => background;
                class Layers { static Widget member(BuildContext c, Set<WidgetState> s, Widget? child) => background(c, s, child); }
                class Holder { Widget member(BuildContext c, Set<WidgetState> s, Widget? child) => background(c, s, child); }
                final holder = Holder();
                int wrongResult(BuildContext c, Set<WidgetState> s, Widget? child) => 1;
                Widget? nullableResult(BuildContext c, Set<WidgetState> s, Widget? child) => child;
                dynamic dynamicResult(BuildContext c, Set<WidgetState> s, Widget? child) => child;
                Future<Widget> asyncResult(BuildContext c, Set<WidgetState> s, Widget? child) async => const SizedBox();
                Widget wrongContext(String c, Set<WidgetState> s, Widget? child) => const SizedBox();
                Widget wrongStates(BuildContext c, Set<int> s, Widget? child) => const SizedBox();
                Widget nonnullChild(BuildContext c, Set<WidgetState> s, Widget child) => child;
                dynamic get dynamicLayer => background;
                ButtonLayerBuilder? get nullableLayer => background;
                ButtonLayerBuilder? nullableFactory() => background;
                """);
        var root = button(false);
        var baseline = open(root, "both.dart", "");
        for (PropertyValue reference : List.of(imported("background"), imported("foreground"), imported("broaderContext"),
                imported("broaderStates"), imported("configured"), imported("makeLayer", Optional.empty(), true),
                imported("Layers", Optional.of("member"), false), imported("holder", Optional.of("member"), false))) {
            assertAnalysis(baseline, apply(baseline, new SetProperty(BUTTON_ID, BACKGROUND, reference)), "both.dart", true);
        }
        var both = layers(baseline);
        assertAnalysis(baseline, both, "both.dart", true);
        String ignored = "// ignore_for_file: argument_type_not_assignable, invalid_assignment\n";
        var adversarial = reopen(baseline, ignored + source(baseline));
        for (String name : List.of("wrongResult", "nullableResult", "dynamicResult", "asyncResult", "wrongContext",
                "wrongStates", "nonnullChild", "dynamicLayer", "nullableLayer")) {
            assertAnalysis(adversarial, apply(adversarial, new SetProperty(BUTTON_ID, BACKGROUND, imported(name))), "both.dart", false);
        }
        assertAnalysis(adversarial, apply(adversarial, new SetProperty(BUTTON_ID, BACKGROUND,
                imported("nullableFactory", Optional.empty(), true))), "both.dart", false);
        for (String name : List.of("dynamicResult", "nonnullChild")) {
            assertAnalysis(adversarial, apply(adversarial, new SetProperty(BUTTON_ID, FOREGROUND, imported(name))), "both.dart", false);
        }
        for (var property : List.of(BACKGROUND, FOREGROUND)) for (PropertyValue invalid : List.of(
                new PropertyValue.NullValue(), new PropertyValue.CallbackValue("background"),
                new PropertyValue.StringValue("(c, s, child) => child"))) {
            var rejected = baseline.apply(new SetProperty(BUTTON_ID, property, invalid));
            assertFalse(rejected.changed());
            assertArrayEquals(baseline.current().fdSnapshot().copyBytes(), rejected.session().current().fdSnapshot().copyBytes());
        }
        Files.write(lib.resolve("both.dart"), both.current().dartCandidateBytes());
        Files.write(lib.resolve("omitted.dart"), open(root, "omitted.dart", "").current().dartCandidateBytes());
        var bg = apply(open(root, "background_only.dart", ""), new SetProperty(BUTTON_ID, BACKGROUND, imported("background")));
        Files.write(lib.resolve("background_only.dart"), bg.current().dartCandidateBytes());
        var fg = apply(open(root, "foreground_only.dart", ""), new SetProperty(BUTTON_ID, FOREGROUND, imported("foreground")));
        Files.write(lib.resolve("foreground_only.dart"), fg.current().dartCandidateBytes());
        var disabled = apply(layers(open(root, "disabled.dart", "")),
                new SetProperty(BUTTON_ID, new PropertyName("enabled"), new PropertyValue.BooleanValue(false)));
        Files.write(lib.resolve("disabled.dart"), disabled.current().dartCandidateBytes());
        Files.write(lib.resolve("empty.dart"), layers(open(button(true), "empty.dart", "")).current().dartCandidateBytes());
        for (String clip : List.of("none", "hardEdge")) {
            String file = "clip_" + clip.toLowerCase(java.util.Locale.ROOT) + ".dart";
            var clipped = apply(layers(open(root, file, "")), new SetProperty(BUTTON_ID,
                    new PropertyName("clipBehavior"), new PropertyValue.EnumValue("Clip", clip)));
            Files.write(lib.resolve(file), clipped.current().dartCandidateBytes());
        }
        var localBaseline = open(root, "local.dart", """
                  Widget localLayer(BuildContext context, Set<WidgetState> states, Widget? child) {
                    // User-owned Привіт.
                    return child ?? const SizedBox();
                  }
                """);
        var local = apply(localBaseline, new SetProperty(BUTTON_ID, BACKGROUND,
                new PropertyValue.DartObjectReferenceValue(Optional.empty(), "localLayer", Optional.empty(),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty())));
        assertAnalysis(localBaseline, local, "local.dart", true);
        var reset = apply(reopen(local, source(local)), new ResetProperty(BUTTON_ID, BACKGROUND));
        assertFalse(source(reset).contains("backgroundBuilder:")); assertTrue(source(reset).contains("// User-owned Привіт."));
        var undo = reset.undo(); assertTrue(undo.changed());
        assertArrayEquals(local.current().dartCandidateBytes(), undo.session().current().dartCandidateBytes());
        assertTrue(undo.session().redo().changed());

        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("layers_test.dart"), """
                import 'package:flutter/material.dart';
                import 'package:flutter/gestures.dart';
                import 'package:flutter_test/flutter_test.dart';
                import 'package:elevated_layer_contract/layers.dart' as layers;
                import 'package:elevated_layer_contract/both.dart' as both;
                import 'package:elevated_layer_contract/omitted.dart' as omitted;
                import 'package:elevated_layer_contract/background_only.dart' as bg;
                import 'package:elevated_layer_contract/foreground_only.dart' as fg;
                import 'package:elevated_layer_contract/disabled.dart' as disabled;
                import 'package:elevated_layer_contract/empty.dart' as empty;
                import 'package:elevated_layer_contract/clip_none.dart' as noClip;
                import 'package:elevated_layer_contract/clip_hardedge.dart' as hardClip;
                Finder material() => find.descendant(of: find.byType(ElevatedButton), matching: find.byType(Material));
                Future<void> mount(WidgetTester tester, Widget sample, {ButtonStyle? themeStyle}) async {
                  await tester.pumpWidget(MaterialApp(theme: ThemeData(useMaterial3: true,
                    elevatedButtonTheme: ElevatedButtonThemeData(style: themeStyle)),
                    home: Scaffold(body: Center(child: sample))));
                  await tester.pumpAndSettle();
                }
                void main() {
                  testWidgets('layers compose once with correct bounds and automatic clipping', (tester) async {
                    layers.calls.clear();
                    await mount(tester, const both.Sample());
                    expect(find.text('Button label'), findsOneWidget);
                    expect(layers.calls.take(2), ['foreground', 'background']);
                    expect(tester.widget<Material>(material()).clipBehavior, Clip.antiAlias);
                    final background = tester.getRect(find.byKey(const ValueKey('layer-background')));
                    final foreground = tester.getRect(find.byKey(const ValueKey('layer-foreground')));
                    expect(background.size, tester.getSize(material()));
                    expect(background.width, 180); expect(background.height, 64);
                    expect(background.contains(foreground.topLeft), isTrue);
                    expect(background.contains(foreground.bottomRight), isTrue);
                    expect(foreground.width, lessThan(background.width));
                    expect(tester.takeException(), isNull);
                  });
                  testWidgets('builders receive real pressed hover focus and all auxiliary SDK states', (tester) async {
                    await mount(tester, const both.Sample());
                    final gesture = await tester.startGesture(tester.getCenter(find.text('Button label')));
                    await tester.pump();
                    expect(layers.backgroundStates.last, contains(WidgetState.pressed));
                    expect(layers.foregroundStates.last, contains(WidgetState.pressed));
                    await gesture.up(); await tester.pumpAndSettle();
                    expect(layers.backgroundStates.last, isNot(contains(WidgetState.pressed)));
                    final mouse = await tester.createGesture(kind: PointerDeviceKind.mouse);
                    await mouse.addPointer(location: const Offset(1, 1));
                    await mouse.moveTo(tester.getCenter(find.text('Button label'))); await tester.pumpAndSettle();
                    expect(layers.backgroundStates.last, contains(WidgetState.hovered));
                    Focus.of(tester.element(find.text('Button label'))).requestFocus(); await tester.pumpAndSettle();
                    expect(layers.foregroundStates.last, contains(WidgetState.focused));
                    final controller = tester.widget<InkWell>(find.descendant(
                      of: find.byType(ElevatedButton), matching: find.byType(InkWell))).statesController!;
                    for (final state in [WidgetState.error, WidgetState.dragged, WidgetState.selected, WidgetState.scrolledUnder]) {
                      controller.update(state, true); await tester.pumpAndSettle();
                      expect(layers.backgroundStates.last, contains(state));
                      expect(layers.foregroundStates.last, contains(state));
                      controller.update(state, false); await tester.pumpAndSettle();
                    }
                    await mouse.removePointer();
                    expect(tester.takeException(), isNull);
                  });
                  testWidgets('disabled builders stay active and null child is passed to foreground', (tester) async {
                    await mount(tester, const disabled.Sample());
                    expect(tester.widget<ElevatedButton>(find.byType(ElevatedButton)).onPressed, isNull);
                    expect(layers.backgroundStates.last, contains(WidgetState.disabled));
                    expect(layers.foregroundStates.last, contains(WidgetState.disabled));
                    await mount(tester, const empty.Sample());
                    expect(layers.foregroundSawNull, isTrue);
                    expect(find.byKey(const ValueKey('empty-layer-child')), findsOneWidget);
                    expect(tester.takeException(), isNull);
                  });
                  testWidgets('either single builder selects automatic clip and explicit clips win', (tester) async {
                    for (final sample in [const bg.Sample(), const fg.Sample()]) {
                      await mount(tester, sample);
                      expect(tester.widget<Material>(material()).clipBehavior, Clip.antiAlias);
                      expect(find.text('Button label'), findsOneWidget);
                    }
                    await mount(tester, const noClip.Sample());
                    expect(tester.widget<Material>(material()).clipBehavior, Clip.none);
                    await mount(tester, const hardClip.Sample());
                    expect(tester.widget<Material>(material()).clipBehavior, Clip.hardEdge);
                    expect(tester.takeException(), isNull);
                  });
                  testWidgets('omission inherits theme builders and otherwise keeps framework clip default', (tester) async {
                    await mount(tester, const omitted.Sample());
                    expect(tester.widget<Material>(material()).clipBehavior, Clip.none);
                    expect(find.byKey(const ValueKey('layer-background')), findsNothing);
                    await mount(tester, const omitted.Sample(), themeStyle: const ButtonStyle(
                      backgroundBuilder: layers.background, foregroundBuilder: layers.foreground));
                    expect(find.byKey(const ValueKey('layer-background')), findsOneWidget);
                    expect(find.byKey(const ValueKey('layer-foreground')), findsOneWidget);
                    expect(tester.widget<Material>(material()).clipBehavior, Clip.antiAlias);
                    expect(find.text('Button label'), findsOneWidget);
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
                name: elevated_layer_contract
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
    private static WidgetNode button(boolean empty) {
        var child = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue("Button label")), Map.of());
        return new WidgetNode(BUTTON_ID, new WidgetTypeId("flutter.material.ElevatedButton"),
                Map.of(new PropertyName("enabled"), new PropertyValue.BooleanValue(true),
                        new PropertyName("styleFixedWidth"), number(180), new PropertyName("styleFixedHeight"), number(64),
                        new PropertyName("stylePadding"), new PropertyValue.EdgeInsetsValue(
                                java.math.BigDecimal.valueOf(12), java.math.BigDecimal.valueOf(8),
                                java.math.BigDecimal.valueOf(12), java.math.BigDecimal.valueOf(8))),
                Map.of(new SlotName("child"), empty ? WidgetSlot.SingleSlot.empty() : WidgetSlot.SingleSlot.of(child)));
    }
    private static PropertyValue number(int value) { return new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(value)); }
    private static DesignerCommandSession layers(DesignerCommandSession baseline) {
        return apply(apply(baseline, new SetProperty(BUTTON_ID, BACKGROUND, imported("background"))),
                new SetProperty(BUTTON_ID, FOREGROUND, imported("foreground")));
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
        return new PropertyValue.DartObjectReferenceValue(Optional.of("package:elevated_layer_contract/layers.dart"), name, member,
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


