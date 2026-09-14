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

/** Generated MenuAnchor builder, State, pair-save and pinned native overlay behavior. */
@EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
class MenuAnchorStateRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;


    @Test
    void generatedBuilderAllStateConsumersSaveReopenAndNativeMenuBehavior() throws Exception {
        initialize();
        var gate = WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault()
                .find(new WidgetTypeId("flutter.material.Checkbox")).orElseThrow(), StableId.random());
        var gateValues = new LinkedHashMap<>(gate.properties());
        gateValues.put(p("value"), new PropertyValue.BooleanValue(true));
        gate = new WidgetNode(gate.id(), gate.type(), gateValues, gate.slots());
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        for (String name : flags()) values.put(p(name), new PropertyValue.BooleanValue(false));
        values.put(p("onOpen"), reference("_opened"));
        values.put(p("onClose"), reference("_closed"));
        values.put(p("onAnimationStatusChanged"), reference("_statusChanged"));
        values.put(p("alignmentOffset"), new PropertyValue.OffsetValue(java.math.BigDecimal.valueOf(3), java.math.BigDecimal.valueOf(4)));
        values.put(p("reservedPadding"), new PropertyValue.EdgeInsetsDirectionalValue(java.math.BigDecimal.valueOf(5),
                java.math.BigDecimal.valueOf(6), java.math.BigDecimal.valueOf(7), java.math.BigDecimal.valueOf(8)));
        values.put(p("stylePadding"), new PropertyValue.EdgeInsetsDirectionalValue(java.math.BigDecimal.valueOf(7),
                java.math.BigDecimal.valueOf(8), java.math.BigDecimal.valueOf(9), java.math.BigDecimal.valueOf(10)));
        values.put(p("stylePressedBackgroundColor"), new PropertyValue.ColorValue(0xffff0000L));
        values.put(p("styleSideWidth"), new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(2)));
        values.put(p("styleVisualDensityHorizontal"), new PropertyValue.DoubleValue(java.math.BigDecimal.ONE));
        values.put(p("styleMinimumWidth"), new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(160)));
        values.put(p("styleFixedHeight"), new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(120)));
        var anchor = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.material.MenuAnchor"), values,
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text("Open menu")),
                       new SlotName("menuChildren"), new WidgetSlot.ListSlot(List.of(menu("Action", "_clicked", true),
                               menu("Persistent", "_persistentClicked", false)))));
        String members = """
                  int pressedEvents = 0; // User-owned Привіт.
                  int persistentEvents = 0;
                  int openEvents = 0;
                  int closeEvents = 0;
                  int builderCalls = 0;
                  MenuController? lastController;
                  BuildContext? lastBuilderContext;
                  final List<AnimationStatus> statuses = [];
                  void _clicked() { pressedEvents += 1; }
                  void _persistentClicked() { persistentEvents += 1; }
                  void _opened() { openEvents += 1; }
                  void _closed() { closeEvents += 1; }
                  void _statusChanged(AnimationStatus value) { statuses.add(value); }
                """;
        String file = "menu_anchor.dart";
        var session = open(column(List.of(gate, anchor)), file, members);
        session = apply(session, new CreateStateBinding(gate.id(), "_options", "_optionsChanged"));
        for (String name : flags()) session = apply(session, new BindPropertyToState(anchor.id(), p(name), field("_options")));
        session = reopen(session, source(session));
        var baseline = session;
        session = apply(session, new CreateMenuAnchorBuilder(anchor.id(), "_buildMenu"));
        assertTrue(source(session).contains("builder: _buildMenu"), source(session));
        assertTrue(source(session).contains("controller.isOpen"), source(session));
        assertTrue(source(session).contains("controller.open()"), source(session));
        assertTrue(source(session).contains("controller.close()"), source(session));
        assertFalse(source(session).contains("final MenuController"), "Template must not invent an owned controller field");
        assertAnalysis(baseline, session, file, true);
        assertArrayEquals(baseline.current().dartCandidateBytes(), session.undo().session().current().dartCandidateBytes());
        assertArrayEquals(session.current().dartCandidateBytes(), session.undo().session().redo().session().current().dartCandidateBytes());
        for (DesignerCommand invalid : List.<DesignerCommand>of(
                new CreateMenuAnchorBuilder(anchor.id(), "_duplicateBuilder"),
                new CreateMenuAnchorBuilder(gate.id(), "_wrongWidget"),
                new CreateEventHandler(anchor.id(), p("builder"), "_fakeEvent"),
                new BindPropertyToState(anchor.id(), p("anchorTapClosesMenu"), field("_options")),
                new CreateStateBinding(anchor.id(), "_invented", "_inventedChanged"))) assertRejected(session, invalid);

        // The explicitly created method is ordinary user-owned code, never a regenerated template.
        String marker = "return TextButton(";
        assertEquals(1, source(session).split(java.util.regex.Pattern.quote(marker), -1).length - 1);
        String userEdit = source(session).replace(marker, """
                // Keep customized menu builder Привіт.
                    builderCalls += 1;
                    lastController = controller;
                    lastBuilderContext = context;
                    return TextButton(""");
        session = reopen(session, userEdit);
        baseline = session;
        session = apply(session, new RenameStateField(gate.id(), "_options", "_menuFlags"));
        for (String name : flags()) assertTrue(source(session).contains(name + ": _menuFlags"), source(session));
        assertTrue(source(session).contains("Keep customized menu builder Привіт."));
        assertTrue(source(session).contains("builderCalls += 1;"));
        assertAnalysis(baseline, session, file, true);
        Files.write(lib.resolve(file), session.current().dartCandidateBytes());
        var restored = reopen(session, Files.readString(lib.resolve(file)));
        assertArrayEquals(session.current().dartCandidateBytes(), restored.current().dartCandidateBytes());
        var detached = apply(restored, new RemovePropertyStateBinding(anchor.id(), p("animated")));
        assertTrue(source(detached).contains("animated: false"));
        assertArrayEquals(restored.current().dartCandidateBytes(), detached.undo().session().current().dartCandidateBytes());
        assertAnalysis(restored, detached, file, true);
        var reset = apply(restored, new ResetProperty(anchor.id(), p("builder")));
        assertFalse(source(reset).contains("builder: _buildMenu"));
        assertTrue(source(reset).contains("Keep customized menu builder Привіт."));
        assertRejected(reset, new CreateMenuAnchorBuilder(anchor.id(), "_buildMenu"));
        assertArrayEquals(restored.current().dartCandidateBytes(), reset.undo().session().current().dartCandidateBytes());
        assertAnalysis(restored, reset, file, true);

        Files.write(lib.resolve(file), restored.current().dartCandidateBytes());
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("menu_anchor_test.dart"), runtimeTests());
        run(List.of(dart.toString(), "analyze", "--no-fatal-warnings", lib.toString()), "analyze.log");
        run(List.of(flutter.toString(), "test", "--no-pub", "--reporter", "expanded"), "flutter-test.log");
        assertTrue(Files.readString(project.resolve("flutter-test.log")).contains("All tests passed!"));
    }
    private static List<String> flags() { return List.of("consumeOutsideTap", "crossAxisUnconstrained", "useRootOverlay", "animated"); }
    private static void assertRejected(DesignerCommandSession session, DesignerCommand command) {
        var rejected = session.apply(command);
        assertFalse(rejected.changed(), command + ": " + rejected.diagnostics());
        assertArrayEquals(session.current().fdSnapshot().copyBytes(), rejected.session().current().fdSnapshot().copyBytes());
        assertArrayEquals(session.current().dartCandidateBytes(), rejected.session().current().dartCandidateBytes());
    }
    private static WidgetNode menu(String label, String handler, boolean close) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.material.MenuItemButton"),
                Map.of(p("enabled"), new PropertyValue.BooleanValue(true), p("closeOnActivate"),
                        new PropertyValue.BooleanValue(close), p("onPressed"), reference(handler)),
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
                import 'package:flutter/services.dart';
                import 'package:flutter_test/flutter_test.dart';
                import 'package:menu_anchor_contract/menu_anchor.dart';

                MenuAnchor anchor(WidgetTester tester) => tester.widget<MenuAnchor>(find.byType(MenuAnchor).first);
                Future<dynamic> mount(WidgetTester tester) async {
                  await tester.pumpWidget(MaterialApp(theme: ThemeData(
                    menuTheme: const MenuThemeData(style: MenuStyle(
                      backgroundColor: WidgetStatePropertyAll(Colors.blue),
                      side: WidgetStatePropertyAll(BorderSide(color: Colors.purple, width: 5)),
                      visualDensity: VisualDensity(horizontal: -2, vertical: -3),
                      minimumSize: WidgetStatePropertyAll(Size(40, 30)),
                      maximumSize: WidgetStatePropertyAll(Size(120, 240)),
                    )),
                    menuButtonTheme: const MenuButtonThemeData(style: ButtonStyle(backgroundColor: WidgetStatePropertyAll(Colors.orange))),
                  ), home: const Scaffold(body: Sample())));
                  await tester.pumpAndSettle();
                  return tester.state(find.byType(Sample));
                }
                Future<void> options(WidgetTester tester, bool value) async {
                  tester.widget<Checkbox>(find.byType(Checkbox)).onChanged!(value); await tester.pumpAndSettle();
                }
                Future<void> openMenu(WidgetTester tester) async {
                  await tester.tap(find.text('Open menu')); await tester.pumpAndSettle();
                }
                Future<void> cleanup(WidgetTester tester) async {
                  await tester.pumpWidget(const SizedBox()); await tester.pumpAndSettle(); expect(tester.takeException(), isNull);
                }
                void main() {
                  testWidgets('complete constructor and builder receive native context controller and optional child', (tester) async {
                    final dynamic state = await mount(tester);
                    final value = anchor(tester);
                    expect(value.controller, isNull); expect(value.childFocusNode, isNull); expect(value.layerLink, isNull);
                    expect(value.builder, isNotNull); expect(value.child, isA<Text>()); expect(value.menuChildren, hasLength(2));
                    expect(value.onOpen, isNotNull); expect(value.onClose, isNotNull); expect(value.onAnimationStatusChanged, isNotNull);
                    expect(value.alignmentOffset, const Offset(3, 4));
                    expect(value.reservedPadding, const EdgeInsetsDirectional.fromSTEB(5, 6, 7, 8));
                    expect(value.clipBehavior, Clip.hardEdge);
                    expect(value.consumeOutsideTap, isTrue); expect(value.crossAxisUnconstrained, isTrue);
                    expect(value.useRootOverlay, isTrue); expect(value.animated, isTrue);
                    expect(state.builderCalls, greaterThan(0)); expect(state.lastController, isA<MenuController>());
                    expect(state.lastBuilderContext, isNot(same(state.context)));
                    expect(MenuController.maybeOf(state.lastBuilderContext!), same(state.lastController));
                    expect(find.text('Action'), findsNothing); await cleanup(tester);
                  });
                  testWidgets('explicit user-owned builder opens real menu and menu item activation controls closing', (tester) async {
                    final dynamic state = await mount(tester); await openMenu(tester);
                    expect(state.lastController.isOpen, isTrue); expect(state.openEvents, 1);
                    expect(find.text('Action'), findsOneWidget);
                    await tester.tap(find.text('Persistent')); await tester.pumpAndSettle();
                    expect(state.persistentEvents, 1); expect(state.lastController.isOpen, isTrue);
                    await tester.tap(find.text('Action')); await tester.pumpAndSettle();
                    expect(state.pressedEvents, 1); expect(state.lastController.isOpen, isFalse); expect(state.closeEvents, 1);
                    expect(find.text('Action'), findsNothing); await cleanup(tester);
                  });
                  testWidgets('all four State consumers rebuild without replacing the native anchor State', (tester) async {
                    final dynamic state = await mount(tester);
                    final original = tester.state(find.byType(MenuAnchor).first); final controller = state.lastController;
                    await options(tester, false);
                    final value = anchor(tester);
                    expect(value.consumeOutsideTap, isFalse); expect(value.crossAxisUnconstrained, isFalse);
                    expect(value.useRootOverlay, isFalse); expect(value.animated, isFalse);
                    expect(tester.state(find.byType(MenuAnchor).first), same(original)); expect(state.lastController, same(controller));
                    await openMenu(tester); expect(controller.isOpen, isTrue);
                    controller.close(); await tester.pumpAndSettle(); expect(controller.isOpen, isFalse);
                    await options(tester, true); expect(anchor(tester).animated, isTrue);
                    expect(tester.state(find.byType(MenuAnchor).first), same(original)); await cleanup(tester);
                  });
                  testWidgets('native panel uses empty-state value fallback and local density constructor defaults', (tester) async {
                    await mount(tester);
                    final style = anchor(tester).style!;
                    expect(style.backgroundColor!.resolve({}), isNull);
                    expect(style.backgroundColor!.resolve({WidgetState.pressed}), const Color(0xffff0000));
                    expect(style.padding!.resolve({}), const EdgeInsetsDirectional.fromSTEB(7, 8, 9, 10));
                    expect(style.side!.resolve({})!.width, 2);
                    expect(style.side!.resolve({})!.color, Colors.purple);
                    expect(style.visualDensity, const VisualDensity(horizontal: 1, vertical: 0));
                    expect(style.minimumSize!.resolve({}), const Size(160, 30));
                    // Shared sparse editing raises the inherited conflicting maximum.
                    expect(style.maximumSize!.resolve({}), const Size(160, 240));
                    expect(style.fixedSize!.resolve({}), const Size(double.infinity, 120));
                    await openMenu(tester);
                    final materials = tester.widgetList<Material>(find.ancestor(of: find.text('Action'), matching: find.byType(Material)));
                    expect(materials.any((value) => value.color == Colors.blue), isTrue);
                    expect(materials.any((value) => value.color == const Color(0xffff0000)), isFalse);
                    await cleanup(tester);
                  });
                  testWidgets('animation Events preserve native closing lifetime and instant status branches', (tester) async {
                    final dynamic state = await mount(tester); await openMenu(tester);
                    expect(state.statuses, containsAllInOrder([AnimationStatus.forward, AnimationStatus.completed]));
                    state.lastController.close();
                    expect(state.lastController.isOpen, isTrue); expect(state.closeEvents, 0);
                    await tester.pumpAndSettle();
                    expect(state.statuses, containsAllInOrder([AnimationStatus.reverse, AnimationStatus.dismissed]));
                    expect(state.closeEvents, 1); expect(state.lastController.isOpen, isFalse);
                    await options(tester, false); state.statuses.clear();
                    await openMenu(tester); state.lastController.close(); await tester.pumpAndSettle();
                    expect(state.statuses, [AnimationStatus.completed, AnimationStatus.dismissed]);
                    await cleanup(tester);
                  });
                  testWidgets('outside tap consume flag controls propagation while both paths dismiss', (tester) async {
                    final dynamic state = await mount(tester); await openMenu(tester);
                    await tester.tap(find.byType(Checkbox)); await tester.pumpAndSettle();
                    expect(state.lastController.isOpen, isFalse);
                    expect(tester.widget<Checkbox>(find.byType(Checkbox)).value, isTrue);
                    await options(tester, false); await openMenu(tester);
                    await tester.tap(find.byType(Checkbox)); await tester.pumpAndSettle();
                    expect(state.lastController.isOpen, isFalse);
                    expect(tester.widget<Checkbox>(find.byType(Checkbox)).value, isTrue);
                    await cleanup(tester);
                  });
                  testWidgets('focused Escape closes open native menu without invoking project menu item callbacks', (tester) async {
                    final dynamic state = await mount(tester); await openMenu(tester);
                    final item = find.ancestor(of: find.text('Action'), matching: find.byType(MenuItemButton));
                    tester.widget<TextButton>(find.descendant(of: item, matching: find.byType(TextButton)).first).focusNode!.requestFocus();
                    await tester.pump();
                    await tester.sendKeyEvent(LogicalKeyboardKey.escape); await tester.pumpAndSettle();
                    expect(state.lastController.isOpen, isFalse); expect(state.closeEvents, 1);
                    expect(state.pressedEvents, 0); expect(state.persistentEvents, 0); await cleanup(tester);
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
                name: menu_anchor_contract
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
