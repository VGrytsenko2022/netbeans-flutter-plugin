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

/** Generated SubmenuButton Events, State, both styles and native nested-menu behavior. */
@EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
class SubmenuButtonStateRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;



    @Test
    void generatedEventsStateNestedMenusAndIndependentStylesSurviveSaveReopen() throws Exception {
        initialize();
        var gate = WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault()
                .find(new WidgetTypeId("flutter.material.Checkbox")).orElseThrow(), StableId.random());
        var gateValues = new LinkedHashMap<>(gate.properties()); gateValues.put(p("value"), bool(true));
        gate = new WidgetNode(gate.id(), gate.type(), gateValues, gate.slots());
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        for (String name : flags()) values.put(p(name), bool(false));
        values.put(p("onHover"), reference("_hovered")); values.put(p("onFocusChange"), reference("_focused"));
        values.put(p("onOpen"), reference("_opened")); values.put(p("onClose"), reference("_closed"));
        values.put(p("onAnimationStatusChanged"), reference("_statusChanged"));
        values.put(p("controller"), reference("_menuController")); values.put(p("focusNode"), reference("_buttonFocus"));
        values.put(p("statesController"), reference("_paddingStates")); values.put(p("hoverOpenDelayUs"), integer(100001));
        values.put(p("alignmentOffset"), new PropertyValue.OffsetValue(java.math.BigDecimal.valueOf(3), java.math.BigDecimal.valueOf(4)));
        values.put(p("stylePressedForegroundColor"), new PropertyValue.ColorValue(0xffff0000L));
        values.put(p("styleBackgroundBuilder"), reference("_background"));
        values.put(p("menuStyleHoveredPadding"), new PropertyValue.EdgeInsetsDirectionalValue(java.math.BigDecimal.valueOf(7),
                java.math.BigDecimal.valueOf(10), java.math.BigDecimal.valueOf(9), java.math.BigDecimal.valueOf(12)));
        values.put(p("menuStyleSideWidth"), number(2));
        values.put(p("menuStyleVisualDensityHorizontal"), number(1));
        values.put(p("submenuIconDefault"), reference("_defaultArrow"));
        values.put(p("submenuIconHovered"), new PropertyValue.NullValue());
        values.put(p("submenuIconFocused"), reference("_focusedArrow"));
        values.put(p("submenuIconDisabled"), PropertyValue.IconDataValue.none());
        var nested = submenu("Nested", Map.of(), List.of(item("Deep", "_deepClicked", true)), false);
        var children = List.of(item("Action", "_clicked", true), item("Persistent", "_persistentClicked", false), nested);
        var primary = submenu("Tools", values, children, true);
        var empty = submenu(null, Map.of(), List.of(), false);
        String members = """
                  int pressedEvents = 0; // User-owned Привіт.
                  int persistentEvents = 0;
                  int deepEvents = 0;
                  int openEvents = 0;
                  int closeEvents = 0;
                  final List<bool> hoverEvents = [];
                  final List<bool> focusEvents = [];
                  final List<AnimationStatus> statuses = [];
                  final MenuController _menuController = MenuController();
                  final FocusNode _buttonFocus = FocusNode();
                  final WidgetStatesController _paddingStates = WidgetStatesController({WidgetState.hovered});
                  MenuController get currentMenu => _menuController;
                  FocusNode get buttonFocus => _buttonFocus;
                  WidgetStatesController get paddingStates => _paddingStates;
                  static const Widget _defaultArrow = Text('Default arrow');
                  static const Widget _focusedArrow = Text('Focused arrow');
                  void refreshForTest() { setState(() {}); }
                  void _clicked() { pressedEvents += 1; }
                  void _persistentClicked() { persistentEvents += 1; }
                  void _deepClicked() { deepEvents += 1; }
                  void _opened() { openEvents += 1; }
                  void _closed() { closeEvents += 1; }
                  void _hovered(bool value) { hoverEvents.add(value); }
                  void _focused(bool value) { focusEvents.add(value); }
                  void _statusChanged(AnimationStatus value) { statuses.add(value); }
                  Widget _background(BuildContext context, Set<WidgetState> states, Widget? child) =>
                    ColoredBox(key: const ValueKey('Button background'), color: Colors.yellow, child: child ?? const SizedBox.shrink());
                  @override
                  void dispose() {
                    _buttonFocus.dispose();
                    _paddingStates.dispose();
                    super.dispose();
                  }
                """;
        String file = "submenu.dart";
        var session = open(column(List.of(gate, primary, empty)), file, members);
        session = apply(session, new CreateStateBinding(gate.id(), "_options", "_optionsChanged"));
        for (String name : flags()) session = apply(session, new BindPropertyToState(primary.id(), p(name), field("_options")));
        session = reopen(session, source(session)); var baseline = session;
        session = apply(session, new RenameEventHandler(primary.id(), p("onOpen"), "_menuOpened"));
        session = apply(session, new RenameStateField(gate.id(), "_options", "_submenuFlags"));
        assertTrue(source(session).contains("onOpen: _menuOpened"), source(session));
        for (String name : flags()) assertTrue(source(session).contains(name + ": _submenuFlags"), source(session));
        assertTrue(source(session).contains("User-owned Привіт."));
        assertFalse(source(session).contains("builder: _buildMenu"));
        assertAnalysis(baseline, session, file, true);
        Files.write(lib.resolve(file), session.current().dartCandidateBytes());
        var restored = reopen(session, Files.readString(lib.resolve(file)));
        assertArrayEquals(session.current().dartCandidateBytes(), restored.current().dartCandidateBytes());
        for (DesignerCommand invalid : List.<DesignerCommand>of(
                new CreateEventHandler(primary.id(), p("onPressed"), "_invented"),
                new CreateMenuAnchorBuilder(primary.id(), "_inventedBuilder"),
                new CreateStateBinding(primary.id(), "_inventedState", "_inventedChanged"),
                new BindPropertyToState(primary.id(), p("statesController"), field("_submenuFlags")),
                new SetProperty(primary.id(), p("style"), reference("_wholeButton")),
                new SetProperty(primary.id(), p("menuStyle"), reference("_wholeMenu")),
                new SetProperty(primary.id(), p("submenuIcon"), reference("_wholeIcon")),
                new SetProperty(primary.id(), p("hoverOpenDelayUs"), new PropertyValue.NullValue()))) assertRejected(restored, invalid);
        var detached = apply(restored, new RemovePropertyStateBinding(primary.id(), p("animated")));
        assertTrue(source(detached).contains("animated: false"));
        assertArrayEquals(restored.current().dartCandidateBytes(), detached.undo().session().current().dartCandidateBytes());
        assertAnalysis(restored, detached, file, true);
        var disconnected = apply(restored, new ResetProperty(primary.id(), p("onHover")));
        assertFalse(source(disconnected).contains("onHover: _hovered"));
        assertTrue(source(disconnected).contains("void _hovered(bool value)"));
        assertAnalysis(restored, disconnected, file, true);
        var cleared = apply(restored, new ClearSlotChildren(primary.id(), new SlotName("menuChildren"), children.stream().map(WidgetNode::id).toList()));
        assertTrue(source(cleared).contains("child: null"));
        assertArrayEquals(restored.current().dartCandidateBytes(), cleared.undo().session().current().dartCandidateBytes());
        assertAnalysis(restored, cleared, file, true);
        Files.write(lib.resolve(file), restored.current().dartCandidateBytes());
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("submenu_test.dart"), runtimeTests());
        run(List.of(dart.toString(), "analyze", "--no-fatal-warnings", lib.toString()), "analyze.log");
        run(List.of(flutter.toString(), "test", "--no-pub", "--reporter", "expanded"), "flutter-test.log");
        assertTrue(Files.readString(project.resolve("flutter-test.log")).contains("All tests passed!"));
    }
    private static List<String> flags() { return List.of("useRootOverlay", "animated"); }
    private static PropertyValue bool(boolean value) { return new PropertyValue.BooleanValue(value); }
    private static PropertyValue number(long value) { return new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(value)); }
    private static PropertyValue integer(long value) { return new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(value)); }
    private static void assertRejected(DesignerCommandSession session, DesignerCommand command) {
        var rejected = session.apply(command); assertFalse(rejected.changed(), command + ": " + rejected.diagnostics());
        assertArrayEquals(session.current().fdSnapshot().copyBytes(), rejected.session().current().fdSnapshot().copyBytes());
        assertArrayEquals(session.current().dartCandidateBytes(), rejected.session().current().dartCandidateBytes());
    }
    private static WidgetNode submenu(String label, Map<PropertyName, PropertyValue> values, List<WidgetNode> items, boolean icons) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.material.SubmenuButton"), values,
                Map.of(new SlotName("child"), label == null ? WidgetSlot.SingleSlot.empty() : WidgetSlot.SingleSlot.of(text(label)),
                       new SlotName("leadingIcon"), icons ? WidgetSlot.SingleSlot.of(text("Leading")) : WidgetSlot.SingleSlot.empty(),
                       new SlotName("trailingIcon"), icons ? WidgetSlot.SingleSlot.of(text("Trailing")) : WidgetSlot.SingleSlot.empty(),
                       new SlotName("menuChildren"), new WidgetSlot.ListSlot(items)));
    }
    private static WidgetNode item(String label, String callback, boolean close) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.material.MenuItemButton"),
                Map.of(p("enabled"), bool(true), p("onPressed"), reference(callback), p("closeOnActivate"), bool(close)),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text(label)),
                       new SlotName("leadingIcon"), WidgetSlot.SingleSlot.empty(), new SlotName("trailingIcon"), WidgetSlot.SingleSlot.empty()));
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
                import 'package:flutter/gestures.dart';
                import 'package:flutter/services.dart';
                import 'package:flutter_test/flutter_test.dart';
                import 'package:submenu_contract/submenu.dart';

                Finder button(String label) => find.ancestor(of: find.text(label), matching: find.byType(SubmenuButton)).first;
                SubmenuButton value(WidgetTester tester) => tester.widget<SubmenuButton>(button('Tools'));
                TextButton inner(WidgetTester tester, String label) => tester.widget<TextButton>(
                  find.descendant(of: button(label), matching: find.byType(TextButton)).first);
                Future<dynamic> mount(WidgetTester tester) async {
                  await tester.pumpWidget(MaterialApp(theme: ThemeData(
                    menuTheme: const MenuThemeData(
                      style: MenuStyle(backgroundColor: WidgetStatePropertyAll(Colors.blue),
                        padding: WidgetStatePropertyAll(EdgeInsets.all(6)),
                        side: WidgetStatePropertyAll(BorderSide(color: Colors.purple, width: 5))),
                      submenuIcon: WidgetStatePropertyAll(Text('Theme arrow')),
                    ),
                    menuButtonTheme: const MenuButtonThemeData(style: ButtonStyle(foregroundColor: WidgetStatePropertyAll(Colors.blue))),
                    textButtonTheme: const TextButtonThemeData(style: ButtonStyle(foregroundColor: WidgetStatePropertyAll(Colors.green))),
                  ), home: const Scaffold(body: Sample())));
                  await tester.pumpAndSettle();
                  return tester.state(find.byType(Sample));
                }
                Future<void> openMenu(WidgetTester tester) async {
                  await tester.tap(find.text('Tools')); await tester.pumpAndSettle();
                }
                Future<void> options(WidgetTester tester, bool flag) async {
                  tester.widget<Checkbox>(find.byType(Checkbox)).onChanged!(flag); await tester.pumpAndSettle();
                }
                Future<void> cleanup(WidgetTester tester) async {
                  await tester.pumpWidget(const SizedBox()); await tester.pumpAndSettle(); expect(tester.takeException(), isNull);
                }
                void main() {
                  testWidgets('complete native constructor has built-in opener and nullable required child', (tester) async {
                    final dynamic state = await mount(tester); final widget = value(tester);
                    expect(widget.controller, same(state.currentMenu)); expect(widget.focusNode, same(state.buttonFocus));
                    expect(widget.statesController, same(state.paddingStates)); expect(widget.menuChildren, hasLength(3));
                    expect(widget.child, isNotNull); expect(widget.leadingIcon, isNotNull); expect(widget.trailingIcon, isNotNull);
                    expect(widget.hoverOpenDelay, const Duration(microseconds: 100001)); expect(widget.alignmentOffset, const Offset(3, 4));
                    expect(widget.clipBehavior, Clip.hardEdge); expect(widget.useRootOverlay, isTrue); expect(widget.animated, isTrue);
                    expect(widget.onHover, isNotNull); expect(widget.onFocusChange, isNotNull);
                    expect(widget.onOpen, isNotNull); expect(widget.onClose, isNotNull); expect(widget.onAnimationStatusChanged, isNotNull);
                    final empty = tester.widgetList<SubmenuButton>(find.byType(SubmenuButton)).last;
                    expect(empty.child, isNull); expect(empty.menuChildren, isEmpty);
                    final emptyButton = tester.widgetList<TextButton>(find.byType(TextButton)).last;
                    expect(emptyButton.onPressed, isNull); expect(inner(tester, 'Tools').onPressed, isNotNull);
                    expect(find.text('Default arrow'), findsNothing); // Standalone native button hides submenu decoration.
                    await cleanup(tester);
                  });
                  testWidgets('native clicks open close and honor persistent and deferred item callbacks', (tester) async {
                    final dynamic state = await mount(tester); await openMenu(tester);
                    expect(state.currentMenu.isOpen, isTrue); expect(state.openEvents, 1);
                    await tester.tap(find.text('Persistent')); await tester.pumpAndSettle();
                    expect(state.persistentEvents, 1); expect(state.currentMenu.isOpen, isTrue);
                    await tester.tap(find.text('Action')); await tester.pumpAndSettle();
                    expect(state.pressedEvents, 1); expect(state.currentMenu.isOpen, isFalse); expect(state.closeEvents, 1);
                    await cleanup(tester);
                  });
                  testWidgets('both styles stay independent and sparse menu padding safely fills missing state', (tester) async {
                    final dynamic state = await mount(tester); final widget = value(tester);
                    expect(widget.style!.foregroundColor!.resolve({}), isNull);
                    expect(widget.style!.foregroundColor!.resolve({WidgetState.pressed}), const Color(0xffff0000));
                    expect(widget.menuStyle!.padding!.resolve({}), const EdgeInsets.all(6));
                    expect(widget.menuStyle!.padding!.resolve({WidgetState.hovered}), const EdgeInsetsDirectional.fromSTEB(7, 10, 9, 12));
                    expect(widget.menuStyle!.side!.resolve({})!.color, Colors.purple);
                    expect(widget.menuStyle!.side!.resolve({})!.width, 2);
                    expect(widget.menuStyle!.visualDensity, const VisualDensity(horizontal: 1, vertical: 0));
                    final nativeAnchor = tester.widget<MenuAnchor>(find.descendant(of: button('Tools'), matching: find.byType(MenuAnchor)).first);
                    expect(nativeAnchor.alignmentOffset, const Offset(3, -6)); // statesController is used for prepass padding.
                    expect(inner(tester, 'Tools').statesController, isNull); // SDK does not forward it to TextButton.
                    state.paddingStates.update(WidgetState.hovered, false); state.refreshForTest(); await tester.pumpAndSettle();
                    final updated = tester.widget<MenuAnchor>(find.descendant(of: button('Tools'), matching: find.byType(MenuAnchor)).first);
                    expect(updated.alignmentOffset, const Offset(3, -2));
                    await openMenu(tester);
                    expect(tester.widgetList<Material>(find.ancestor(of: find.text('Action'), matching: find.byType(Material)))
                      .any((material) => material.color == Colors.blue), isTrue);
                    await cleanup(tester);
                  });
                  testWidgets('menu clipping and button layer-builder clipping are separate native branches', (tester) async {
                    await mount(tester); final buttonFinder = find.descendant(of: button('Tools'), matching: find.byType(TextButton)).first;
                    expect(value(tester).clipBehavior, Clip.hardEdge); expect(inner(tester, 'Tools').clipBehavior, isNull);
                    expect(find.byKey(const ValueKey('Button background')), findsOneWidget);
                    expect(tester.widgetList<Material>(find.descendant(of: buttonFinder, matching: find.byType(Material)))
                      .any((material) => material.clipBehavior == Clip.antiAlias), isTrue);
                    await cleanup(tester);
                  });
                  testWidgets('icon buckets distinguish null native fallback from an empty Icon widget', (tester) async {
                    await mount(tester); final icons = value(tester).submenuIcon!;
                    expect((icons.resolve({})! as Text).data, 'Default arrow');
                    expect((icons.resolve({WidgetState.focused})! as Text).data, 'Focused arrow');
                    expect(icons.resolve({WidgetState.hovered}), isNull);
                    expect(icons.resolve({WidgetState.hovered, WidgetState.focused}), isNull);
                    expect((icons.resolve({WidgetState.disabled})! as Icon).icon, isNull);
                    expect((icons.resolve({WidgetState.disabled, WidgetState.hovered})! as Icon).icon, isNull);
                    await cleanup(tester);
                  });
                  testWidgets('two State consumers preserve native button State and user-owned controllers', (tester) async {
                    final dynamic state = await mount(tester); final original = tester.state(button('Tools'));
                    final controller = state.currentMenu; final focus = state.buttonFocus;
                    await options(tester, false);
                    expect(value(tester).useRootOverlay, isFalse); expect(value(tester).animated, isFalse);
                    expect(tester.state(button('Tools')), same(original)); expect(value(tester).controller, same(controller));
                    expect(value(tester).focusNode, same(focus)); await openMenu(tester);
                    expect(controller.isOpen, isTrue); controller.close(); await tester.pumpAndSettle();
                    await options(tester, true); expect(value(tester).animated, isTrue); await cleanup(tester);
                  });
                  testWidgets('hover delay preserves microseconds and emits native hover and focus Events', (tester) async {
                    final dynamic state = await mount(tester);
                    final mouse = await tester.createGesture(kind: PointerDeviceKind.mouse);
                    await mouse.addPointer(location: const Offset(790, 590));
                    final point = tester.getCenter(find.text('Tools'));
                    await mouse.moveTo(point); await mouse.moveTo(point + const Offset(1, 0)); await tester.pump();
                    await tester.pump(const Duration(microseconds: 50000)); expect(state.currentMenu.isOpen, isFalse);
                    await tester.pump(const Duration(microseconds: 50001)); await tester.pumpAndSettle();
                    expect(state.currentMenu.isOpen, isTrue); expect(state.hoverEvents, contains(true)); expect(state.focusEvents, contains(true));
                    await mouse.moveTo(const Offset(790, 590)); await tester.pump(); expect(state.hoverEvents, contains(false));
                    await mouse.removePointer(); await cleanup(tester);
                  });
                  testWidgets('native nested submenu focus opens deep children and activation closes root', (tester) async {
                    final dynamic state = await mount(tester);
                    // Flutter 3.44.8 keeps the animated root controller open in
                    // this nested-overlay fixture after activation. Exercise the
                    // deterministic SDK close contract here; animation lifetime
                    // is covered by the following test.
                    await options(tester, false); await openMenu(tester);
                    inner(tester, 'Nested').focusNode!.requestFocus(); await tester.pumpAndSettle();
                    expect(find.text('Deep'), findsOneWidget);
                    await tester.sendKeyEvent(LogicalKeyboardKey.arrowRight); await tester.pump();
                    await tester.sendKeyEvent(LogicalKeyboardKey.enter); await tester.pumpAndSettle();
                    expect(state.deepEvents, 1); expect(state.currentMenu.isOpen, isFalse); await cleanup(tester);
                  });
                  testWidgets('animation statuses retain native closing lifetime and instant mode', (tester) async {
                    final dynamic state = await mount(tester); await openMenu(tester);
                    expect(state.statuses, containsAllInOrder([AnimationStatus.forward, AnimationStatus.completed]));
                    state.currentMenu.close(); expect(state.currentMenu.isOpen, isTrue); expect(state.closeEvents, 0);
                    await tester.pumpAndSettle(); expect(state.statuses, containsAllInOrder([AnimationStatus.reverse, AnimationStatus.dismissed]));
                    expect(state.currentMenu.isOpen, isFalse); await options(tester, false); state.statuses.clear();
                    await openMenu(tester); state.currentMenu.close(); await tester.pumpAndSettle();
                    expect(state.statuses, [AnimationStatus.completed, AnimationStatus.dismissed]); await cleanup(tester);
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
                name: submenu_contract
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
