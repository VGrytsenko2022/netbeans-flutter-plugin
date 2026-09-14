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

/** Generated MenuItemButton State, source preservation and pinned native interaction behavior. */
@EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
class MenuItemButtonStateRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;

    @Test
    void generatedMenuAllStateConsumersSaveReopenAndNativeBehavior() throws Exception {
        initialize();
        var gate = WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault()
                .find(new WidgetTypeId("flutter.material.Checkbox")).orElseThrow(), StableId.random());
        var gateValues = new LinkedHashMap<>(gate.properties());
        gateValues.put(p("value"), new PropertyValue.BooleanValue(true));
        gate = new WidgetNode(gate.id(), gate.type(), gateValues, gate.slots());
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        values.put(p("enabled"), new PropertyValue.BooleanValue(true));
        for (String name : List.of("autofocus", "requestFocusOnHover", "closeOnActivate")) values.put(p(name), new PropertyValue.BooleanValue(false));
        values.put(p("onPressed"), reference("_clicked"));
        values.put(p("onHover"), reference("_hovered"));
        values.put(p("onFocusChange"), reference("_focused"));
        values.put(p("semanticsLabel"), new PropertyValue.StringValue("Primary action"));
        values.put(p("shortcutTrigger"), new PropertyValue.EnumValue("LogicalKeyboardKey", "keyK"));
        values.put(p("shortcutControl"), new PropertyValue.BooleanValue(true));
        values.put(p("shortcutNumLock"), new PropertyValue.EnumValue("LockState", "ignored"));
        values.put(p("shortcutIncludeRepeats"), new PropertyValue.BooleanValue(false));
        values.put(p("stylePressedForegroundColor"), new PropertyValue.ColorValue(0xffff0000L));
        var primary = menu(values, "Action", true);
        var persistent = menu(Map.of(p("enabled"), new PropertyValue.BooleanValue(true),
                p("closeOnActivate"), new PropertyValue.BooleanValue(false),
                p("onPressed"), reference("_persistentClicked"),
                p("shortcutCharacter"), new PropertyValue.StringValue("p"),
                p("shortcutControl"), new PropertyValue.BooleanValue(true)), "Persistent", false);
        String members = """
                  int pressedEvents = 0; // User-owned Привіт.
                  int persistentEvents = 0;
                  int hoverEvents = 0;
                  int focusEvents = 0;
                  void _clicked() { pressedEvents += 1; }
                  void _persistentClicked() { persistentEvents += 1; }
                  void _hovered(bool value) { hoverEvents += 1; }
                  void _focused(bool value) { focusEvents += 1; }
                  final MenuSerializableShortcut _shortcut = const CharacterActivator('x');
                """;
        String file = "menu_item.dart";
        var session = open(column(List.of(gate, primary, persistent)), file, members);
        session = apply(session, new CreateStateBinding(gate.id(), "_options", "_optionsChanged"));
        for (String name : List.of("enabled", "autofocus", "requestFocusOnHover", "closeOnActivate")) {
            session = apply(session, new BindPropertyToState(primary.id(), p(name), field("_options")));
        }
        session = reopen(session, source(session));
        var baseline = session;
        session = apply(session, new RenameStateField(gate.id(), "_options", "_menuFlags"));
        for (String name : List.of("autofocus", "requestFocusOnHover", "closeOnActivate")) assertTrue(source(session).contains(name + ": _menuFlags"));
        assertTrue(source(session).contains("_menuFlags ? _clicked : null"), source(session));
        assertFalse(source(session).contains("onLongPress:"));
        for (DesignerCommand invalid : List.<DesignerCommand>of(
                new CreateStateBinding(primary.id(), "_invented", "_inventedChanged"),
                new CreateEventHandler(primary.id(), p("onLongPress"), "_inventedEvent"),
                new BindPropertyToState(primary.id(), p("shortcutControl"), field("_menuFlags")),
                new SetProperty(primary.id(), p("shortcut"), reference("_shortcut")),
                new SetProperty(primary.id(), p("clipBehavior"), new PropertyValue.NullValue()),
                new ResetProperty(primary.id(), p("enabled")))) {
            var rejected = session.apply(invalid);
            assertFalse(rejected.changed(), invalid + ": " + rejected.diagnostics());
            assertArrayEquals(session.current().fdSnapshot().copyBytes(), rejected.session().current().fdSnapshot().copyBytes());
            assertArrayEquals(session.current().dartCandidateBytes(), rejected.session().current().dartCandidateBytes());
        }
        assertAnalysis(baseline, session, file, true);
        Files.write(lib.resolve(file), session.current().dartCandidateBytes());
        var restored = reopen(session, Files.readString(lib.resolve(file)));
        assertArrayEquals(session.current().dartCandidateBytes(), restored.current().dartCandidateBytes());
        var detached = apply(restored, new RemovePropertyStateBinding(primary.id(), p("enabled")));
        assertTrue(source(detached).contains("onPressed: _clicked"), source(detached));
        assertArrayEquals(restored.current().dartCandidateBytes(), detached.undo().session().current().dartCandidateBytes());
        assertAnalysis(restored, detached, file, true);
        assertTrue(source(restored).contains("User-owned Привіт."));
        Files.write(lib.resolve(file), restored.current().dartCandidateBytes());
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("menu_item_test.dart"), runtimeTests());
        run(List.of(dart.toString(), "analyze", "--no-fatal-warnings", lib.toString()), "analyze.log");
        run(List.of(flutter.toString(), "test", "--no-pub", "--reporter", "expanded"), "flutter-test.log");
        assertTrue(Files.readString(project.resolve("flutter-test.log")).contains("All tests passed!"));
    }
    private static WidgetNode menu(Map<PropertyName, PropertyValue> values, String label, boolean icons) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.material.MenuItemButton"), values,
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text(label)),
                       new SlotName("leadingIcon"), icons ? WidgetSlot.SingleSlot.of(text("Leading")) : WidgetSlot.SingleSlot.empty(),
                       new SlotName("trailingIcon"), icons ? WidgetSlot.SingleSlot.of(text("Trailing")) : WidgetSlot.SingleSlot.empty()));
    }
    private static WidgetNode column(List<WidgetNode> children) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(children)));
    }
    private static PropertyValue.DartObjectReferenceValue reference(String name) {
        return new PropertyValue.DartObjectReferenceValue(Optional.empty(), name, Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }
    private static String runtimeTests() {
        return """
                import 'package:flutter/material.dart';
                import 'package:flutter/services.dart';
                import 'package:flutter/gestures.dart';
                import 'package:flutter_test/flutter_test.dart';
                import 'package:menu_item_contract/menu_item.dart';

                Finder item(String label) => find.ancestor(of: find.text(label), matching: find.byType(MenuItemButton)).first;
                MenuItemButton menu(WidgetTester tester) => tester.widget<MenuItemButton>(item('Action'));
                TextButton inner(WidgetTester tester) => tester.widget<TextButton>(find.descendant(of: item('Action'), matching: find.byType(TextButton)).first);
                Future<dynamic> mount(WidgetTester tester) async {
                  await tester.pumpWidget(MaterialApp(theme: ThemeData(
                    menuButtonTheme: const MenuButtonThemeData(style: ButtonStyle(foregroundColor: WidgetStatePropertyAll(Colors.blue))),
                    textButtonTheme: const TextButtonThemeData(style: ButtonStyle(foregroundColor: WidgetStatePropertyAll(Colors.green))),
                  ), home: const Scaffold(body: Sample())));
                  await tester.pumpAndSettle();
                  return tester.state(find.byType(Sample));
                }
                Future<void> options(WidgetTester tester, bool value) async {
                  tester.widget<Checkbox>(find.byType(Checkbox)).onChanged!(value);
                  await tester.pumpAndSettle();
                }
                Future<void> cleanup(WidgetTester tester) async {
                  await tester.pumpWidget(const SizedBox()); await tester.pumpAndSettle();
                  expect(tester.takeException(), isNull);
                }
                void main() {
                  testWidgets('complete native constructor and both shortcut variants', (tester) async {
                    await mount(tester);
                    final value = menu(tester);
                    expect(value.onPressed, isNotNull); expect(value.autofocus, isTrue);
                    expect(value.requestFocusOnHover, isTrue); expect(value.closeOnActivate, isTrue);
                    expect(value.clipBehavior, Clip.none); expect(value.overflowAxis, Axis.horizontal);
                    expect(value.leadingIcon, isNotNull); expect(value.trailingIcon, isNotNull);
                    final shortcut = value.shortcut! as SingleActivator;
                    expect(shortcut.trigger, LogicalKeyboardKey.keyK); expect(shortcut.control, isTrue);
                    expect(shortcut.shift, isFalse); expect(shortcut.alt, isFalse); expect(shortcut.meta, isFalse);
                    expect(shortcut.numLock, LockState.ignored); expect(shortcut.includeRepeats, isFalse);
                    final other = tester.widget<MenuItemButton>(item('Persistent'));
                    expect(other.shortcut, isA<CharacterActivator>());
                    expect((other.shortcut! as CharacterActivator).character, 'p');
                    expect(other.closeOnActivate, isFalse);
                    await cleanup(tester);
                  });
                  testWidgets('sparse style resolution matches the pinned native menu merge', (tester) async {
                    await mount(tester);
                    expect(inner(tester).style!.foregroundColor!.resolve({}), isNull);
                    final generatedColor = DefaultTextStyle.of(tester.element(find.text('Action'))).style.color;
                    expect(inner(tester).style!.foregroundColor!.resolve({WidgetState.pressed}), const Color(0xffff0000));
                    await tester.pumpWidget(MaterialApp(theme: ThemeData(
                      menuButtonTheme: const MenuButtonThemeData(style: ButtonStyle(foregroundColor: WidgetStatePropertyAll(Colors.blue))),
                      textButtonTheme: const TextButtonThemeData(style: ButtonStyle(foregroundColor: WidgetStatePropertyAll(Colors.green))),
                    ), home: Scaffold(body: Column(children: [
                      MenuItemButton(onPressed: () {}, style: ButtonStyle(foregroundColor: WidgetStateProperty.resolveWith(
                        (states) => states.contains(WidgetState.pressed) ? const Color(0xffff0000) : null,
                      )), child: const Text('Native sparse')),
                      MenuItemButton(onPressed: () {}, child: const Text('Native default')),
                    ]))));
                    await tester.pumpAndSettle();
                    expect(generatedColor, DefaultTextStyle.of(tester.element(find.text('Native sparse'))).style.color);
                    expect(generatedColor, Colors.green); // SDK merges property objects, not each resolved state.
                    expect(DefaultTextStyle.of(tester.element(find.text('Native default'))).style.color, Colors.blue);
                    await cleanup(tester);
                  });
                  testWidgets('four live State consumers preserve the MenuItemButton State', (tester) async {
                    final dynamic state = await mount(tester);
                    final original = tester.state(item('Action'));
                    await options(tester, false);
                    final value = menu(tester);
                    expect(value.onPressed, isNull); expect(value.autofocus, isFalse);
                    expect(value.requestFocusOnHover, isFalse); expect(value.closeOnActivate, isFalse);
                    expect(tester.state(item('Action')), same(original));
                    await tester.tap(find.text('Action')); await tester.pump();
                    expect(state.pressedEvents, 0);
                    await options(tester, true);
                    expect(menu(tester).onPressed, isNotNull); expect(tester.state(item('Action')), same(original));
                    await cleanup(tester);
                  });
                  testWidgets('native press is deferred until the post-frame callback', (tester) async {
                    final dynamic state = await mount(tester);
                    await tester.tap(find.text('Action'));
                    expect(state.pressedEvents, 0);
                    await tester.pump();
                    expect(state.pressedEvents, 1);
                    await cleanup(tester);
                  });
                  testWidgets('display shortcuts are not registered while focused Enter activates', (tester) async {
                    final dynamic state = await mount(tester);
                    await tester.sendKeyDownEvent(LogicalKeyboardKey.controlLeft);
                    await tester.sendKeyEvent(LogicalKeyboardKey.keyK);
                    await tester.sendKeyEvent(LogicalKeyboardKey.keyP);
                    await tester.sendKeyUpEvent(LogicalKeyboardKey.controlLeft);
                    await tester.pump();
                    expect(state.pressedEvents, 0); expect(state.persistentEvents, 0);
                    inner(tester).focusNode!.requestFocus(); await tester.pump();
                    await tester.sendKeyEvent(LogicalKeyboardKey.enter); await tester.pump();
                    expect(state.pressedEvents, 1);
                    await cleanup(tester);
                  });
                  testWidgets('hover callbacks retain native disabled behavior and semantic label overrides children', (tester) async {
                    final semantics = tester.ensureSemantics();
                    final dynamic state = await mount(tester);
                    expect(find.bySemanticsLabel('Primary action'), findsOneWidget);
                    final focusBefore = state.focusEvents;
                    inner(tester).focusNode!.unfocus(); await tester.pump();
                    final mouse = await tester.createGesture(kind: PointerDeviceKind.mouse);
                    await mouse.addPointer(location: const Offset(790, 590));
                    await mouse.moveTo(tester.getCenter(find.text('Action')));
                    await tester.pump();
                    expect(inner(tester).focusNode!.hasFocus, isTrue);
                    expect(state.hoverEvents, greaterThan(0));
                    expect(state.focusEvents, greaterThan(focusBefore));
                    await mouse.moveTo(const Offset(790, 590)); await tester.pump();
                    await options(tester, false);
                    final before = state.hoverEvents;
                    await mouse.moveTo(tester.getCenter(find.text('Action'))); await tester.pump();
                    expect(state.hoverEvents, greaterThan(before));
                    expect(state.pressedEvents, 0);
                    await mouse.removePointer(); await cleanup(tester); semantics.dispose();
                  });
                  testWidgets('real MenuAnchor honors both closeOnActivate branches and callbacks', (tester) async {
                    final controller = MenuController();
                    await tester.pumpWidget(MaterialApp(home: Scaffold(body: MenuAnchor(
                      controller: controller, menuChildren: const [Sample()],
                      builder: (context, controller, child) => const Text('Anchor'),
                    ))));
                    controller.open(); await tester.pumpAndSettle();
                    final dynamic state = tester.state(find.byType(Sample));
                    await tester.tap(find.text('Persistent')); await tester.pumpAndSettle();
                    expect(controller.isOpen, isTrue); expect(state.persistentEvents, 1);
                    await tester.tap(find.text('Action')); await tester.pumpAndSettle();
                    expect(controller.isOpen, isFalse); expect(state.pressedEvents, 1);
                    await cleanup(tester);
                  });
                }
                """;
    }

    private static WidgetNode text(String value) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(p("data"), new PropertyValue.StringValue(value)), Map.of());
    }
    private static StatePropertyBinding field(String name) {
        return new StatePropertyBinding(name, StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.DIRECT, Optional.empty());
    }
    private static PropertyName p(String name) { return new PropertyName(name); }
    private void initialize() throws Exception {
        sdk = Path.of(System.getProperty("flutter.events.sdk")).toRealPath();
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        flutter = sdk.resolve(windows ? "bin/flutter.bat" : "bin/flutter");
        dart = sdk.resolve(windows ? "bin/cache/dart-sdk/bin/dart.exe" : "bin/cache/dart-sdk/bin/dart");
        Files.writeString(project.resolve("pubspec.yaml"), """
                name: menu_item_contract
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
                .isPresent()), ticket.request().toString());
        var operation = new DartCandidateAnalyzer(dart, ignored -> { }).analyze(ticket.request());
        try {
            var result = operation.result().toCompletableFuture().get(90, TimeUnit.SECONDS);
            var accepted = PairSaveEvidenceGate.evaluateAnalysis(ticket, result);
            assertEquals(pass, accepted.ready(), () -> result.status() + "\n" + result.diagnostics()
                    + "\n" + result.symbolEvidence().stream().filter(evidence -> !evidence.accepted()).toList()
                    + "\n" + accepted.diagnostics());
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
