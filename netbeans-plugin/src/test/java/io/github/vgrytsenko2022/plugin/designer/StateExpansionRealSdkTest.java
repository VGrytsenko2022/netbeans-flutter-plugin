package io.github.vgrytsenko2022.plugin.designer;

import io.github.vgrytsenko2022.dart.DartCandidateAnalyzer;
import io.github.vgrytsenko2022.dart.DartCandidateWarningPolicy;
import io.github.vgrytsenko2022.designer.catalog.BuiltInWidgetCatalog;
import io.github.vgrytsenko2022.designer.catalog.WidgetNodePrototypeFactory;
import io.github.vgrytsenko2022.designer.codec.FdDecodeResult;
import io.github.vgrytsenko2022.designer.codec.FdDocumentCodec;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.validation.ValidationResult;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Real analyzer, user input, shared selection, controller notifications and disposal. */
@EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
class StateExpansionRealSdkTest {
    @TempDir Path project;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void textToggleSelectionAndDependentPropertiesWorkTogetherAndDisposeOwnedResources(boolean renameFields) throws Exception {
        Path sdk = Path.of(System.getProperty("flutter.events.sdk")).toRealPath();
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        Path flutter = sdk.resolve(windows ? "bin/flutter.bat" : "bin/flutter");
        Path dart = sdk.resolve(windows ? "bin/cache/dart-sdk/bin/dart.exe" : "bin/cache/dart-sdk/bin/dart");
        Files.writeString(project.resolve("pubspec.yaml"), """
                name: state_expansion_contract
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
        Path lib = Files.createDirectories(project.resolve("lib"));
        run(List.of(flutter.toString(), "pub", "get", "--offline"), "pub-get.log");
        WidgetNode textField = node("flutter.material.TextField");
        WidgetNode mirror = text("preview");
        WidgetNode toggle = child(with(node("flutter.material.IconButton"), "isSelected", bool(false)),
                "icon", node("flutter.widgets.Icon"));
        WidgetNode switchNode = node("flutter.material.Switch");
        WidgetNode first = child(node("flutter.material.ListTile"), "title", text("Choice A"));
        WidgetNode second = child(node("flutter.material.ListTile"), "title", text("Choice B"));
        WidgetNode visible = child(node("flutter.widgets.Visibility"), "child", text("controlled visibility"));
        WidgetNode inverse = child(node("flutter.widgets.Offstage"), "child", text("inverse visibility"));
        WidgetNode selected = child(node("flutter.widgets.Visibility"), "child", text("choice two visible"));
        WidgetNode pageLabel = text("page preview");
        WidgetNode opacity = child(node("flutter.widgets.Opacity"), "child", text("opacity label"));
        WidgetNode linear = node("flutter.material.LinearProgressIndicator");
        WidgetNode circular = node("flutter.material.CircularProgressIndicator");
        WidgetNode refresh = node("flutter.material.RefreshProgressIndicator");
        WidgetNode stack = children(node("flutter.widgets.IndexedStack"), List.of(text("panel zero"), text("panel one"), text("panel two")));
        WidgetNode root = children(node("flutter.widgets.Column"), List.of(textField, mirror, toggle,
                switchNode, first, second, visible, inverse, selected, pageLabel, opacity, linear, circular, refresh, stack));
        var initial = StateBindingRealSdkTest.openRoot(root, "sample.dart", """
                  int _choice = 0;
                  double _fraction = 0.5;
                  void setFraction(double value) { setState(() { _fraction = value; }); }
                  bool initHook = false;
                  bool disposeHook = false;
                  @override
                  void initState() {
                    super.initState();
                    initHook = true; // preserve my initialization
                  }
                  @override
                  void dispose() {
                    disposeHook = true; // preserve my cleanup
                    super.dispose();
                  }
                """);
        var session = apply(initial, new CreateStateBinding(textField.id(), "_text", "_textChanged",
                StateBinding.Action.CHANGE, Optional.empty(), "initial", Optional.empty()));
        session = apply(session, new CreateStateBinding(toggle.id(), "_enabled", "_toggle",
                StateBinding.Action.TOGGLE, Optional.empty(), "", Optional.empty()));
        session = apply(session, new CreateStateBinding(switchNode.id(), "_enabled", "_switchChanged",
                StateBinding.Action.CHANGE, Optional.empty(), "", Optional.of(field("_enabled", StateBinding.Type.BOOL))));
        session = apply(session, new CreateStateBinding(first.id(), "_choice", "_chooseFirst",
                StateBinding.Action.SELECT, Optional.of(integer(1)), "", Optional.of(field("_choice", StateBinding.Type.INT))));
        session = apply(session, new CreateStateBinding(second.id(), "_choice", "_chooseSecond",
                StateBinding.Action.SELECT, Optional.of(integer(2)), "", Optional.of(field("_choice", StateBinding.Type.INT))));
        session = apply(session, new BindPropertyToState(mirror.id(), new PropertyName("data"),
                binding("_text", StateBinding.Type.TEXT_CONTROLLER, StatePropertyBinding.Transform.TEXT)));
        session = apply(session, new BindPropertyToState(visible.id(), new PropertyName("visible"), field("_enabled", StateBinding.Type.BOOL)));
        session = apply(session, new BindPropertyToState(inverse.id(), new PropertyName("offstage"),
                binding("_enabled", StateBinding.Type.BOOL, StatePropertyBinding.Transform.NOT)));
        session = apply(session, new BindPropertyToState(selected.id(), new PropertyName("visible"),
                new StatePropertyBinding("_choice", StateBinding.Type.INT, Optional.empty(),
                        StatePropertyBinding.Transform.EQUALS, Optional.of(integer(2)))));
        session = apply(session, new BindPropertyToState(pageLabel.id(), new PropertyName("data"),
                binding("_choice", StateBinding.Type.INT, StatePropertyBinding.Transform.TO_STRING)));
        for (WidgetNode output : List.of(opacity, linear, circular, refresh)) {
            session = apply(session, new BindPropertyToState(output.id(),
                    new PropertyName(output == opacity ? "opacity" : "value"),
                    binding("_fraction", StateBinding.Type.DOUBLE, StatePropertyBinding.Transform.CLAMP)));
        }
        session = apply(session, new BindPropertyToState(stack.id(), new PropertyName("index"),
                binding("_choice", StateBinding.Type.INT, StatePropertyBinding.Transform.CLAMP)));
        if (renameFields) {
            session = apply(session, new RenameStateField(textField.id(), "_text", "_query"));
            session = apply(session, new RenameStateField(visible.id(), "_enabled", "_selected"));
            session = apply(session, new RenameStateField(first.id(), "_choice", "_page"));
            session = apply(session, new RenameStateField(linear.id(), "_fraction", "_ratio"));
            String renamed = new String(session.current().dartCandidateBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(renamed.contains("_query.addListener(_queryStateListener);"), renamed);
            assertTrue(renamed.contains("_query.removeListener(_queryStateListener);"), renamed);
            assertTrue(renamed.contains("_query.dispose();"), renamed);
            assertTrue(renamed.contains("void _textChanged(String value)"), "Field rename must not rename the event handler");
            assertTrue(renamed.contains("void setFraction(double value) { setState(() { _ratio = value; }); }"), renamed);
            assertTrue(renamed.contains("// preserve my initialization"));
            assertTrue(renamed.contains("// preserve my cleanup"));
        }
        Path source = lib.resolve("sample.dart");
        Files.write(source, initial.current().dartCandidateBytes());
        var pair = session.current().preparedPair().orElseThrow();
        var codec = new FdDocumentCodec();
        var current = new FlutterDesignerDocumentState.Current((FdDecodeResult.Current) codec.decode(initial.current().fdSnapshot()),
                new ValidationResult(List.of()), BuiltInWidgetCatalog.getDefault(), List.of(), List.of(),
                Optional.of(pair.dartTransition().baseline().source()), Optional.of(pair.dartTransition().baseline()));
        var ticket = PairSaveEvidenceGate.prepareAnalysis(current, pair, project, source, DartCandidateWarningPolicy.ALLOW, sdk);
        var controllerProbes = ticket.request().symbolProbes().stream()
                .filter(probe -> probe.expectedSymbolName().equals(renameFields ? "_query" : "_text")).toList();
        assertEquals(2, controllerProbes.size(), "Control and text consumer both require exact field evidence");
        assertTrue(controllerProbes.stream().allMatch(probe -> probe.expectedTargetKind().equals(Optional.of("FIELD"))));
        var operation = new DartCandidateAnalyzer(dart, ignored -> { }).analyze(ticket.request());
        try {
            var analysis = operation.result().toCompletableFuture().get(90, TimeUnit.SECONDS);
            var accepted = PairSaveEvidenceGate.evaluateAnalysis(ticket, analysis);
            assertTrue(accepted.ready(), () -> analysis + "\n" + accepted.diagnostics());
        } finally { operation.cancel(); }
        assertArrayEquals(initial.current().dartCandidateBytes(), Files.readAllBytes(source));
        Files.write(source, session.current().dartCandidateBytes());
        var reopened = DesignerCommandSession.open(session.current().fdSnapshot(), Files.readAllBytes(source), BuiltInWidgetCatalog.getDefault());
        assertTrue(reopened.ready(), reopened.diagnostics().toString());
        var detached = apply(session, new RemovePropertyStateBinding(mirror.id(), new PropertyName("data")));
        detached = apply(detached, new RemoveStateBinding(textField.id()));
        detached = apply(detached, new RemoveWidget(textField.id()));
        Files.write(lib.resolve("detached.dart"), detached.current().dartCandidateBytes());
        var emptyStack = apply(session, new ClearSlotChildren(stack.id(), new SlotName("children"),
                ((WidgetSlot.ListSlot) stack.slots().get(new SlotName("children"))).children().stream().map(WidgetNode::id).toList()));
        Files.write(lib.resolve("empty_stack.dart"), emptyStack.current().dartCandidateBytes());
        Path test = Files.createDirectories(project.resolve("test")).resolve("state_test.dart");
        Files.writeString(test, """
                import 'package:flutter/material.dart';
                import 'package:flutter_test/flutter_test.dart';
                import 'package:state_expansion_contract/sample.dart' as page;
                void main() {
                  testWidgets('controller, shared control state, selection and cleanup', (tester) async {
                    await tester.pumpWidget(MaterialApp(home: Scaffold(body: page.Sample())));
                    final dynamic state = tester.state(find.byType(page.Sample));
                    expect(state.initHook, isTrue);
                    final controller = tester.widget<TextField>(find.byType(TextField)).controller!;
                    expect(controller.text, 'initial');
                    expect(tester.widgetList<Text>(find.byType(Text)).any((w) => w.data == 'initial'), isTrue);
                    await tester.enterText(find.byType(TextField), 'typed');
                    await tester.pump();
                    expect(tester.widgetList<Text>(find.byType(Text)).any((w) => w.data == 'typed'), isTrue);
                    controller.text = 'programmatic';
                    await tester.pump();
                    expect(tester.widgetList<Text>(find.byType(Text)).any((w) => w.data == 'programmatic'), isTrue);
                    expect(find.text('controlled visibility'), findsNothing);
                    tester.widget<IconButton>(find.byType(IconButton)).onPressed!();
                    await tester.pump();
                    expect(tester.widget<IconButton>(find.byType(IconButton)).isSelected, isTrue);
                    expect(tester.widget<Switch>(find.byType(Switch)).value, isTrue);
                    expect(find.text('controlled visibility'), findsOneWidget);
                    expect(find.text('inverse visibility'), findsOneWidget);
                    tester.widget<Switch>(find.byType(Switch)).onChanged!(false);
                    await tester.pump();
                    expect(tester.widget<IconButton>(find.byType(IconButton)).isSelected, isFalse);
                    tester.widget<ListTile>(find.widgetWithText(ListTile, 'Choice A')).onTap!();
                    await tester.pump();
                    expect(tester.widget<ListTile>(find.widgetWithText(ListTile, 'Choice A')).selected, isTrue);
                    expect(tester.widget<ListTile>(find.widgetWithText(ListTile, 'Choice B')).selected, isFalse);
                    expect(find.text('1'), findsOneWidget);
                    tester.widget<ListTile>(find.widgetWithText(ListTile, 'Choice B')).onTap!();
                    await tester.pump();
                    expect(tester.widget<ListTile>(find.widgetWithText(ListTile, 'Choice A')).selected, isFalse);
                    expect(tester.widget<ListTile>(find.widgetWithText(ListTile, 'Choice B')).selected, isTrue);
                    expect(find.text('2'), findsOneWidget);
                    expect(find.text('choice two visible'), findsOneWidget);
                    expect(tester.widget<IndexedStack>(find.byType(IndexedStack)).index, 2);
                    state.setFraction(3.0);
                    await tester.pump();
                    expect(tester.widget<Opacity>(find.byType(Opacity).first).opacity, 1.0);
                    expect(tester.widget<LinearProgressIndicator>(find.byType(LinearProgressIndicator)).value, 1.0);
                    expect(tester.widget<CircularProgressIndicator>(find.byType(CircularProgressIndicator)).value, 1.0);
                    expect(tester.widget<RefreshProgressIndicator>(find.byType(RefreshProgressIndicator)).value, 1.0);
                    state.setFraction(-2.0);
                    await tester.pump();
                    expect(tester.widget<LinearProgressIndicator>(find.byType(LinearProgressIndicator)).value, 0.0);
                    state.setFraction(double.nan);
                    await tester.pump();
                    expect(tester.widget<LinearProgressIndicator>(find.byType(LinearProgressIndicator)).value, 1.0);
                    await tester.pumpWidget(const SizedBox.shrink());
                    expect(state.disposeHook, isTrue);
                    expect(() => controller.addListener(() {}), throwsFlutterError);
                    expect(tester.takeException(), isNull);
                  });
                }
                """);
        run(List.of(dart.toString(), "analyze", "--no-fatal-warnings", lib.toString()), "analyze.log");
        run(List.of(flutter.toString(), "test", "--no-pub", test.toString()), "flutter-test.log");
    }

    private static DesignerCommandSession apply(DesignerCommandSession source, DesignerCommand command) {
        var result = source.apply(command);
        assertEquals(DesignerCommandStatus.APPLIED, result.status(), command + ": " + result.diagnostics());
        return result.session();
    }
    private static StatePropertyBinding field(String name, StateBinding.Type type) {
        return binding(name, type, StatePropertyBinding.Transform.DIRECT);
    }
    private static StatePropertyBinding binding(String name, StateBinding.Type type, StatePropertyBinding.Transform transform) {
        return new StatePropertyBinding(name, type, Optional.empty(), transform, Optional.empty());
    }
    private static WidgetNode node(String type) {
        return WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId(type)).orElseThrow(), StableId.random());
    }
    private static WidgetNode text(String value) { return with(node("flutter.widgets.Text"), "data", new PropertyValue.StringValue(value)); }
    private static PropertyValue.BooleanValue bool(boolean value) { return new PropertyValue.BooleanValue(value); }
    private static PropertyValue.IntegerValue integer(int value) { return new PropertyValue.IntegerValue(BigInteger.valueOf(value)); }
    private static WidgetNode with(WidgetNode node, String name, PropertyValue value) {
        var properties = new LinkedHashMap<>(node.properties()); properties.put(new PropertyName(name), value);
        return new WidgetNode(node.id(), node.type(), properties, node.slots(), node.extensions(), node.stateBinding(), node.propertyBindings());
    }
    private static WidgetNode child(WidgetNode node, String slot, WidgetNode child) {
        var slots = new LinkedHashMap<>(node.slots()); slots.put(new SlotName(slot), WidgetSlot.SingleSlot.of(child));
        return new WidgetNode(node.id(), node.type(), node.properties(), slots, node.extensions(), node.stateBinding(), node.propertyBindings());
    }
    private static WidgetNode children(WidgetNode node, List<WidgetNode> children) {
        return new WidgetNode(node.id(), node.type(), node.properties(), Map.of(new SlotName("children"), new WidgetSlot.ListSlot(children)));
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
