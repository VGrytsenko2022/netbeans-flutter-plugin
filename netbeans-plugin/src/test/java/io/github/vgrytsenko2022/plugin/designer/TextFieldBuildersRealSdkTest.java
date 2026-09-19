package io.github.vgrytsenko2022.plugin.designer;

import io.github.vgrytsenko2022.dart.*;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.codec.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.validation.ValidationResult;
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

/** Exact nullable callbacks, required named arguments and actual generated TextField behavior. */
@EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
class TextFieldBuildersRealSdkTest {
    @TempDir Path project;
    private static final StableId FIELD_ID = StableId.random();
    private static final PropertyName COUNTER = new PropertyName("buildCounter");
    private static final PropertyName MENU = new PropertyName("contextMenuBuilder");
    private static final String COUNTER_TYPE = "InputCounterWidgetBuilder?";
    private static final String MENU_TYPE = "EditableTextContextMenuBuilder?";
    private Path sdk, flutter, dart, lib;

    @Test
    void exactNamedParameterAndNullableCallbackProofsPreserveUserSource() throws Exception {
        initialize(); writeBuilders();
        var baseline = open(field(Map.of()), "probe.dart", "");
        for (String name : List.of("counter", "hideCounter", "optionalCounter", "broadCounter", "configuredCounter", "absentCounter")) {
            assertAnalysis(baseline, apply(baseline, new SetProperty(FIELD_ID, COUNTER, imported(name))), "probe.dart", true);
        }
        for (String name : List.of("makeCounter", "makeAbsentCounter")) {
            assertAnalysis(baseline, apply(baseline, new SetProperty(FIELD_ID, COUNTER, imported(name, Optional.empty(), true))), "probe.dart", true);
        }
        for (String root : List.of("Builders", "holder")) {
            assertAnalysis(baseline, apply(baseline, new SetProperty(FIELD_ID, COUNTER,
                    imported(root, Optional.of("counter"), false))), "probe.dart", true);
        }
        for (String name : List.of("menu", "broadMenu", "configuredMenu", "absentMenu")) {
            assertAnalysis(baseline, apply(baseline, new SetProperty(FIELD_ID, MENU, imported(name))), "probe.dart", true);
        }
        for (String name : List.of("makeMenu", "makeAbsentMenu")) {
            assertAnalysis(baseline, apply(baseline, new SetProperty(FIELD_ID, MENU,
                    imported(name, Optional.empty(), true))), "probe.dart", true);
        }
        var both = apply(apply(baseline, new SetProperty(FIELD_ID, COUNTER, imported("counter"))),
                new SetProperty(FIELD_ID, MENU, imported("menu")));
        assertAnalysis(baseline, both, "probe.dart", true);
        var adversarial = reopen(baseline, "// ignore_for_file: argument_type_not_assignable, invalid_assignment\n" + source(baseline));
        for (String name : List.of("dynamicCounter", "wrongCounter", "asyncCounter", "wrongContextCounter",
                "wrongLengthCounter", "nonnullMaxCounter", "wrongFocusCounter", "renamedCounter",
                "missingCounter", "extraCounter", "positionalCounter", "dynamicCounterGetter")) {
            assertAnalysis(adversarial, apply(adversarial, new SetProperty(FIELD_ID, COUNTER, imported(name))), "probe.dart", false);
        }
        for (String name : List.of("dynamicMenu", "nullableResultMenu", "wrongMenu", "asyncMenu",
                "wrongStateMenu", "wrongContextMenu", "dynamicMenuGetter")) {
            assertAnalysis(adversarial, apply(adversarial, new SetProperty(FIELD_ID, MENU, imported(name))), "probe.dart", false);
        }
        var localBaseline = open(field(Map.of()), "local.dart", """
                  Widget? _localCounter(BuildContext context, {
                    required int currentLength, required int? maxLength, required bool isFocused,
                  }) {
                    // User-owned Привіт.
                    return null;
                  }
                """);
        var local = apply(localBaseline, new SetProperty(FIELD_ID, COUNTER,
                new PropertyValue.DartObjectReferenceValue(Optional.empty(), "_localCounter", Optional.empty(),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty())));
        assertAnalysis(localBaseline, local, "local.dart", true);
        var reset = apply(reopen(local, source(local)), new ResetProperty(FIELD_ID, COUNTER));
        assertFalse(source(reset).contains("buildCounter:")); assertTrue(source(reset).contains("// User-owned Привіт."));
        assertArrayEquals(local.current().dartCandidateBytes(), reset.undo().session().current().dartCandidateBytes());
        for (var property : List.of(COUNTER, MENU)) for (PropertyValue invalid : List.of(
                new PropertyValue.CallbackValue("callback"), new PropertyValue.StringValue("(c) => null"),
                new PropertyValue.DartExpressionValue("(c) => null"))) {
            var rejected = baseline.apply(new SetProperty(FIELD_ID, property, invalid));
            assertFalse(rejected.changed());
            assertArrayEquals(baseline.current().fdBytes(), rejected.session().current().fdBytes());
        }
    }

    @Test
    void generatedFieldsRespectCounterSemanticsGraphemesAndMenuNullVersusOmission() throws Exception {
        initialize(); writeBuilders();
        save("counter.dart", Map.of(COUNTER, imported("counter")));
        save("bounded.dart", Map.of(COUNTER, imported("counter"), p("maxLength"), integer(2),
                p("maxLengthEnforcement"), new PropertyValue.EnumValue("MaxLengthEnforcement", "none")));
        save("unlimited.dart", Map.of(COUNTER, imported("counter"), p("maxLength"), integer(-1)));
        save("hide.dart", Map.of(COUNTER, imported("hideCounter"), p("maxLength"), integer(5)));
        save("counter_default.dart", Map.of(p("maxLength"), integer(5)));
        save("counter_null.dart", Map.of(COUNTER, new PropertyValue.NullValue(), p("maxLength"), integer(5)));
        save("counter_nullable.dart", Map.of(COUNTER, imported("makeAbsentCounter", Optional.empty(), true), p("maxLength"), integer(5)));
        save("menu.dart", Map.of(MENU, imported("menu")));
        save("menu_default.dart", Map.of());
        save("menu_null.dart", Map.of(MENU, new PropertyValue.NullValue()));
        save("menu_nullable.dart", Map.of(MENU, imported("absentMenu")));
        save("readonly.dart", Map.of(MENU, imported("menu"), p("readOnly"), new PropertyValue.BooleanValue(true)));
        save("disabled.dart", Map.of(COUNTER, imported("counter"), MENU, imported("menu"), p("enabled"), new PropertyValue.BooleanValue(false)));
        var reset = apply(open(field(Map.of(MENU, new PropertyValue.NullValue())), "menu_reset.dart", ""), new ResetProperty(FIELD_ID, MENU));
        Files.write(lib.resolve("menu_reset.dart"), reset.current().dartCandidateBytes());
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("builders_test.dart"), """
                import 'package:flutter/material.dart';
                import 'package:flutter_test/flutter_test.dart';
                import 'package:text_field_builders_contract/builders.dart' as builders;
                import 'package:text_field_builders_contract/counter.dart' as counter;
                import 'package:text_field_builders_contract/bounded.dart' as bounded;
                import 'package:text_field_builders_contract/unlimited.dart' as unlimited;
                import 'package:text_field_builders_contract/hide.dart' as hidden;
                import 'package:text_field_builders_contract/counter_default.dart' as defaultCounter;
                import 'package:text_field_builders_contract/counter_null.dart' as nullCounter;
                import 'package:text_field_builders_contract/counter_nullable.dart' as nullableCounter;
                import 'package:text_field_builders_contract/menu.dart' as menu;
                import 'package:text_field_builders_contract/menu_default.dart' as defaultMenu;
                import 'package:text_field_builders_contract/menu_null.dart' as nullMenu;
                import 'package:text_field_builders_contract/menu_nullable.dart' as nullableMenu;
                import 'package:text_field_builders_contract/menu_reset.dart' as resetMenu;
                import 'package:text_field_builders_contract/readonly.dart' as readonly;
                import 'package:text_field_builders_contract/disabled.dart' as disabled;
                Future<void> mount(WidgetTester tester, Widget sample) async {
                  await tester.pumpWidget(MaterialApp(theme: ThemeData(platform: TargetPlatform.android),
                    home: Scaffold(body: Center(child: SizedBox(width: 300, child: sample)))));
                  await tester.pumpAndSettle();
                }
                Future<EditableTextState> select(WidgetTester tester) async {
                  final state = tester.state<EditableTextState>(find.byType(EditableText));
                  state.widget.controller.text = 'selected words';
                  await tester.tap(find.byType(TextField)); await tester.pumpAndSettle();
                  state.selectAll(SelectionChangedCause.longPress);
                  await tester.pumpAndSettle();
                  state.showToolbar(); await tester.pumpAndSettle();
                  return state;
                }
                void main() {
                  testWidgets('counter receives grapheme length nullable max and real focus with semantics', (tester) async {
                    await mount(tester, const counter.Sample());
                    expect(builders.length, 0); expect(builders.maximum, isNull); expect(builders.focused, isFalse);
                    await tester.enterText(find.byType(TextField), '👨‍👩‍👧‍👦a🇺🇦'); await tester.pumpAndSettle();
                    expect(builders.length, 3); expect(builders.maximum, isNull); expect(builders.focused, isTrue);
                    expect(find.byKey(const ValueKey('custom-counter')), findsOneWidget);
                    final labels = tester.widget<Text>(find.byKey(const ValueKey('custom-counter')));
                    expect(labels.semanticsLabel, 'Custom character count');
                    final wrappers = tester.widgetList<Semantics>(find.ancestor(
                      of: find.byKey(const ValueKey('custom-counter')), matching: find.byType(Semantics)));
                    expect(wrappers.any((s) => s.container && s.properties.liveRegion == true), isTrue);
                    FocusManager.instance.primaryFocus!.unfocus(); await tester.pumpAndSettle();
                    expect(builders.focused, isFalse); expect(tester.takeException(), isNull);
                  });
                  testWidgets('positive and noMaxLength sentinel reach required nullable maxLength', (tester) async {
                    await mount(tester, const bounded.Sample());
                    await tester.enterText(find.byType(TextField), 'abcd'); await tester.pumpAndSettle();
                    expect(builders.length, 4); expect(builders.maximum, 2);
                    await mount(tester, const unlimited.Sample());
                    await tester.enterText(find.byType(TextField), 'abcd'); await tester.pumpAndSettle();
                    expect(builders.length, 4); expect(builders.maximum, TextField.noMaxLength);
                    expect(tester.takeException(), isNull);
                  });
                  testWidgets('null result hides counter and its generated Semantics', (tester) async {
                    await mount(tester, const hidden.Sample());
                    final decoration = tester.widget<InputDecorator>(find.byType(InputDecorator)).decoration;
                    expect(decoration.counter, isNull); expect(decoration.counterText, isNull);
                    expect(find.text('0/5'), findsNothing); expect(tester.takeException(), isNull);
                  });
                  testWidgets('omitted explicit null and nullable factory keep default counter', (tester) async {
                    for (final sample in [const defaultCounter.Sample(), const nullCounter.Sample(), const nullableCounter.Sample()]) {
                      await mount(tester, sample);
                      expect(tester.widget<TextField>(find.byType(TextField)).buildCounter, isNull);
                      expect(find.text('0/5'), findsOneWidget);
                    }
                    expect(tester.takeException(), isNull);
                  });
                  testWidgets('custom menu receives actual EditableTextState and can invoke user action', (tester) async {
                    builders.menuActions = 0; builders.menuState = null;
                    await mount(tester, const menu.Sample());
                    final state = await select(tester);
                    expect(identical(builders.menuState, state), isTrue);
                    expect(find.byKey(const ValueKey('custom-menu')), findsOneWidget);
                    expect(state.textEditingValue.selection.textInside(state.textEditingValue.text), 'selected words');
                    await tester.tap(find.byKey(const ValueKey('menu-action'))); await tester.pumpAndSettle();
                    expect(builders.menuActions, 1); expect(find.byKey(const ValueKey('custom-menu')), findsNothing);
                    expect(tester.takeException(), isNull);
                  });
                  testWidgets('menu omission and reset use platform default but null disables it', (tester) async {
                    for (final sample in [const defaultMenu.Sample(), const resetMenu.Sample()]) {
                      await mount(tester, sample);
                      expect(tester.widget<TextField>(find.byType(TextField)).contextMenuBuilder, isNotNull);
                      await select(tester);
                      expect(find.byType(AdaptiveTextSelectionToolbar), findsOneWidget);
                    }
                    for (final sample in [const nullMenu.Sample(), const nullableMenu.Sample()]) {
                      await mount(tester, sample);
                      expect(tester.widget<TextField>(find.byType(TextField)).contextMenuBuilder, isNull);
                      await select(tester);
                      expect(find.byType(AdaptiveTextSelectionToolbar), findsNothing);
                      expect(find.byKey(const ValueKey('custom-menu')), findsNothing);
                    }
                    expect(tester.takeException(), isNull);
                  });
                  testWidgets('readOnly selection menu stays useful and disabled counter preserves field behavior', (tester) async {
                    await mount(tester, const readonly.Sample());
                    final state = await select(tester);
                    expect(state.widget.readOnly, isTrue);
                    expect(find.byKey(const ValueKey('custom-menu')), findsOneWidget);
                    await mount(tester, const disabled.Sample());
                    expect(tester.widget<TextField>(find.byType(TextField)).enabled, isFalse);
                    expect(builders.focused, isFalse);
                    expect(find.byKey(const ValueKey('custom-counter')), findsOneWidget);
                    expect(find.byKey(const ValueKey('custom-menu')), findsNothing);
                    expect(tester.takeException(), isNull);
                  });
                }
                """);
        run(List.of(flutter.toString(), "test", "--reporter", "expanded"), "flutter-test.log");
    }

    private void writeBuilders() throws Exception {
        Files.writeString(lib.resolve("builders.dart"), """
                import 'package:flutter/material.dart';
                int length = -1; int? maximum; bool focused = false;
                EditableTextState? menuState; int menuActions = 0;
                Widget? counter(BuildContext context, {required int currentLength, required int? maxLength, required bool isFocused}) {
                  length = currentLength; maximum = maxLength; focused = isFocused;
                  return Text('$currentLength/$maxLength', key: const ValueKey('custom-counter'), semanticsLabel: 'Custom character count');
                }
                Widget? hideCounter(BuildContext c, {required int currentLength, required int? maxLength, required bool isFocused}) => null;
                Widget? optionalCounter(BuildContext c, {int currentLength = 0, int? maxLength, bool isFocused = false}) => null;
                Widget? broadCounter(Object c, {required num currentLength, required Object? maxLength, required Object isFocused}) => null;
                InputCounterWidgetBuilder? get configuredCounter => counter;
                InputCounterWidgetBuilder? get absentCounter => null;
                InputCounterWidgetBuilder? makeCounter() => counter;
                InputCounterWidgetBuilder? makeAbsentCounter() => null;
                class Builders {
                  static Widget? counter(BuildContext c, {required int currentLength, required int? maxLength, required bool isFocused}) => null;
                }
                class Holder {
                  Widget? counter(BuildContext c, {required int currentLength, required int? maxLength, required bool isFocused}) => null;
                }
                final holder = Holder();
                dynamic dynamicCounter(BuildContext c, {required int currentLength, required int? maxLength, required bool isFocused}) => null;
                int wrongCounter(BuildContext c, {required int currentLength, required int? maxLength, required bool isFocused}) => 1;
                Future<Widget?> asyncCounter(BuildContext c, {required int currentLength, required int? maxLength, required bool isFocused}) async => null;
                Widget? wrongContextCounter(String c, {required int currentLength, required int? maxLength, required bool isFocused}) => null;
                Widget? wrongLengthCounter(BuildContext c, {required String currentLength, required int? maxLength, required bool isFocused}) => null;
                Widget? nonnullMaxCounter(BuildContext c, {required int currentLength, required int maxLength, required bool isFocused}) => null;
                Widget? wrongFocusCounter(BuildContext c, {required int currentLength, required int? maxLength, required String isFocused}) => null;
                Widget? renamedCounter(BuildContext c, {required int currentLength, required int? maxLength, required bool focused}) => null;
                Widget? missingCounter(BuildContext c, {required int currentLength, required bool isFocused}) => null;
                Widget? extraCounter(BuildContext c, {required int currentLength, required int? maxLength, required bool isFocused, required int extra}) => null;
                Widget? positionalCounter(BuildContext c, int currentLength, int? maxLength, bool isFocused) => null;
                dynamic get dynamicCounterGetter => counter;
                Widget menu(BuildContext context, EditableTextState state) {
                  menuState = state;
                  return Material(key: const ValueKey('custom-menu'), child: TextButton(
                    key: const ValueKey('menu-action'), onPressed: () { menuActions++; state.hideToolbar(); }, child: const Text('Custom action')));
                }
                Widget broadMenu(Object context, Object state) => const SizedBox();
                EditableTextContextMenuBuilder? get configuredMenu => menu;
                EditableTextContextMenuBuilder? get absentMenu => null;
                EditableTextContextMenuBuilder? makeMenu() => menu;
                EditableTextContextMenuBuilder? makeAbsentMenu() => null;
                dynamic dynamicMenu(BuildContext context, EditableTextState state) => const SizedBox();
                Widget? nullableResultMenu(BuildContext context, EditableTextState state) => null;
                int wrongMenu(BuildContext context, EditableTextState state) => 1;
                Future<Widget> asyncMenu(BuildContext context, EditableTextState state) async => const SizedBox();
                Widget wrongStateMenu(BuildContext context, String state) => const SizedBox();
                Widget wrongContextMenu(String context, EditableTextState state) => const SizedBox();
                dynamic get dynamicMenuGetter => menu;
                """);
    }
    private void save(String file, Map<PropertyName, PropertyValue> properties) throws Exception {
        Files.write(lib.resolve(file), open(field(properties), file, "").current().dartCandidateBytes());
    }
    private static WidgetNode field(Map<PropertyName, PropertyValue> properties) {
        return new WidgetNode(FIELD_ID, TextFieldWidgetPropertySchema.TEXT_FIELD_TYPE, properties, Map.of());
    }
    private static PropertyName p(String name) { return new PropertyName(name); }
    private static PropertyValue integer(int value) { return new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(value)); }
    private void initialize() throws Exception {
        sdk = Path.of(System.getProperty("flutter.events.sdk")).toRealPath();
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        flutter = sdk.resolve(windows ? "bin/flutter.bat" : "bin/flutter");
        dart = sdk.resolve(windows ? "bin/cache/dart-sdk/bin/dart.exe" : "bin/cache/dart-sdk/bin/dart");
        Files.writeString(project.resolve("pubspec.yaml"), """
                name: text_field_builders_contract
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
                .filter(type -> type.expectedDartType().equals(COUNTER_TYPE) || type.expectedDartType().equals(MENU_TYPE)).isPresent()), ticket.request().toString());
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
        return new PropertyValue.DartObjectReferenceValue(Optional.of("package:text_field_builders_contract/builders.dart"), name, member,
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



