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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** One analyzer ticket and one mounted Flutter fixture cover all reviewed ListTile State actions. */
@EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
class ListTileStateBindingRealSdkTest {
    @TempDir Path project;

    @Test void toggleSelectAndCompatibleReusedFieldsRebuildMountedTilesWithExactValues() throws Exception {
        Path sdk = Path.of(System.getProperty("flutter.events.sdk")).toRealPath();
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        Path flutter = sdk.resolve(windows ? "bin/flutter.bat" : "bin/flutter");
        Path dart = sdk.resolve(windows ? "bin/cache/dart-sdk/bin/dart.exe" : "bin/cache/dart-sdk/bin/dart");
        Files.writeString(project.resolve("pubspec.yaml"), """
                name: list_tile_state_contract
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

        List<Case> cases = cases();
        var catalog = BuiltInWidgetCatalog.getDefault();
        List<WidgetNode> tiles = new ArrayList<>();
        List<StableId> titles = new ArrayList<>();
        StringBuilder members = new StringBuilder();
        for (int i = 0; i < cases.size(); i++) {
            Case entry = cases.get(i);
            WidgetNode title = WidgetNodePrototypeFactory.create(catalog.find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow(),
                    StableId.random(), Map.of(new PropertyName("data"), new PropertyValue.StringValue("Preview " + i)));
            titles.add(title.id());
            WidgetNode tile = WidgetNodePrototypeFactory.create(catalog.find(new WidgetTypeId("flutter.material.ListTile")).orElseThrow(),
                    StableId.random(), Map.of(new PropertyName("selected"), new PropertyValue.BooleanValue(entry.initiallySelected())));
            var slots = new LinkedHashMap<>(tile.slots());
            slots.put(new SlotName("title"), WidgetSlot.SingleSlot.of(title));
            tiles.add(new WidgetNode(tile.id(), tile.type(), tile.properties(), slots));
            if (entry.reusedType().isPresent()) members.append("  ").append(entry.dartType()).append(" _choice")
                    .append(i).append(" = ").append(entry.initializer()).append(";\n");
        }
        WidgetNode column = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(tiles)));
        WidgetNode root = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.SingleChildScrollView"), Map.of(),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(column)));
        var initial = StateBindingRealSdkTest.openRoot(root, "sample.dart", members.toString());
        var session = initial;
        for (int i = 0; i < cases.size(); i++) {
            Case entry = cases.get(i);
            String fieldName = "_choice" + i;
            var reused = entry.reusedType().map(type -> new StatePropertyBinding(fieldName, type, Optional.empty(),
                    StatePropertyBinding.Transform.DIRECT));
            session = applied(session, new CreateStateBinding(tiles.get(i).id(), fieldName, "_tap" + i,
                    entry.action(), entry.selectedValue(), "", reused));
            StateBinding binding = EventWidgetLookup.find(session.current().document().root(), tiles.get(i).id()).stateBinding().orElseThrow();
            session = applied(session, new BindPropertyToState(titles.get(i), new PropertyName("data"),
                    new StatePropertyBinding(fieldName, binding.type(), binding.referenceType(), StatePropertyBinding.Transform.TO_STRING)));
        }

        Path source = lib.resolve("sample.dart");
        byte[] baseline = initial.current().dartCandidateBytes();
        Files.write(source, baseline);
        var pair = session.current().preparedPair().orElseThrow();
        var current = new FlutterDesignerDocumentState.Current((FdDecodeResult.Current) new FdDocumentCodec().decode(initial.current().fdSnapshot()),
                new ValidationResult(List.of()), catalog, List.of(), List.of(),
                Optional.of(pair.dartTransition().baseline().source()), Optional.of(pair.dartTransition().baseline()));
        var ticket = PairSaveEvidenceGate.prepareAnalysis(current, pair, project, source, DartCandidateWarningPolicy.ALLOW, sdk);
        for (int i = 0; i < cases.size(); i++) {
            String fieldName = "_choice" + i;
            var probes = ticket.request().symbolProbes().stream().filter(probe -> probe.expectedSymbolName().equals(fieldName)).toList();
            assertFalse(probes.isEmpty(), fieldName);
            assertTrue(probes.stream().allMatch(probe -> probe.expectedTargetKind().equals(Optional.of("FIELD"))
                    && probe.staticTypeProbe().isPresent()), fieldName);
        }
        var operation = new DartCandidateAnalyzer(dart, ignored -> {}).analyze(ticket.request());
        try {
            var analysis = operation.result().toCompletableFuture().get(90, TimeUnit.SECONDS);
            var evidence = PairSaveEvidenceGate.evaluateAnalysis(ticket, analysis);
            assertTrue(evidence.ready(), () -> analysis + "\n" + evidence.diagnostics());
        } finally { operation.cancel(); }
        assertArrayEquals(baseline, Files.readAllBytes(source));
        Files.write(source, session.current().dartCandidateBytes());

        StringBuilder test = new StringBuilder("import 'package:flutter/material.dart';\n"
                + "import 'package:flutter_test/flutter_test.dart';\n"
                + "import 'package:list_tile_state_contract/sample.dart';\n"
                + "void main() {\n  testWidgets('all ListTile State actions', (tester) async {\n"
                + "    await tester.pumpWidget(const MaterialApp(home: Scaffold(body: Sample())));\n"
                + "    expect(find.byType(ListTile), findsNWidgets(" + cases.size() + "));\n");
        for (int i = 0; i < cases.size(); i++) {
            Case entry = cases.get(i);
            test.append("    { // ").append(i).append(' ').append(entry.action()).append('\n')
                    .append("      final finder = find.byType(ListTile).at(").append(i).append(");\n")
                    .append("      final before = tester.widget<ListTile>(finder);\n")
                    .append("      expect(before.selected, ").append(entry.initiallySelected()).append(");\n")
                    .append("      expect((before.title! as Text).data, ").append(quoted(entry.initialText())).append(");\n")
                    .append("      before.onTap!();\n      await tester.pump();\n")
                    .append("      final after = tester.widget<ListTile>(finder);\n")
                    .append("      expect(identical(before, after), isFalse);\n")
                    .append("      expect(after.selected, ").append(entry.action() == StateBinding.Action.TOGGLE ? !entry.initiallySelected() : true).append(");\n")
                    .append("      expect((after.title! as Text).data, ").append(quoted(entry.updatedText())).append(");\n");
            if (entry.action() == StateBinding.Action.TOGGLE) test.append("      after.onTap!();\n      await tester.pump();\n")
                    .append("      expect(tester.widget<ListTile>(finder).selected, ").append(entry.initiallySelected()).append(");\n");
            test.append("      expect(tester.takeException(), isNull);\n    }\n");
        }
        test.append("    await tester.pumpWidget(const SizedBox.shrink());\n    expect(tester.takeException(), isNull);\n  });\n}\n");
        Path testFile = Files.createDirectories(project.resolve("test")).resolve("list_tile_test.dart");
        Files.writeString(testFile, test);
        run(List.of(dart.toString(), "analyze", "--no-fatal-warnings", lib.toString()), "analyze.log");
        run(List.of(flutter.toString(), "test", "--no-pub", testFile.toString()), "flutter-test.log");
    }

    private static List<Case> cases() {
        List<Case> cases = new ArrayList<>();
        for (boolean selected : List.of(false, true)) {
            cases.add(new Case(StateBinding.Action.TOGGLE, selected, Optional.empty(), Optional.empty(), "", "",
                    Boolean.toString(selected), Boolean.toString(!selected)));
            for (var value : List.of(new Scalar(new PropertyValue.BooleanValue(true), "true"),
                    new Scalar(new PropertyValue.StringValue("selected '$value'"), "selected '$value'"),
                    new Scalar(new PropertyValue.IntegerValue(BigInteger.valueOf(7)), "7"),
                    new Scalar(new PropertyValue.DoubleValue(new BigDecimal("0.5")), "0.5"))) {
                cases.add(new Case(StateBinding.Action.SELECT, selected, Optional.of(value.value()), Optional.empty(), "", "",
                        selected ? value.text() : "null", value.text()));
            }
        }
        cases.add(reuse(StateBinding.Type.BOOL, "bool", "false", "false", new PropertyValue.BooleanValue(true), "true"));
        cases.add(reuse(StateBinding.Type.STRING, "String", "'before'", "before", new PropertyValue.StringValue("after"), "after"));
        cases.add(reuse(StateBinding.Type.INT, "int", "0", "0", new PropertyValue.IntegerValue(BigInteger.valueOf(7)), "7"));
        cases.add(reuse(StateBinding.Type.DOUBLE, "double", "0.0", "0.0", new PropertyValue.DoubleValue(new BigDecimal("0.5")), "0.5"));
        cases.add(reuse(StateBinding.Type.NUM, "num", "0", "0", new PropertyValue.DoubleValue(new BigDecimal("0.5")), "0.5"));
        cases.add(reuse(StateBinding.Type.NULLABLE_BOOL, "bool?", "null", "null", new PropertyValue.BooleanValue(true), "true"));
        cases.add(reuse(StateBinding.Type.NULLABLE_STRING, "String?", "null", "null", new PropertyValue.StringValue("after"), "after"));
        cases.add(reuse(StateBinding.Type.NULLABLE_INT, "int?", "null", "null", new PropertyValue.IntegerValue(BigInteger.valueOf(7)), "7"));
        cases.add(reuse(StateBinding.Type.NULLABLE_DOUBLE, "double?", "null", "null", new PropertyValue.DoubleValue(new BigDecimal("0.5")), "0.5"));
        cases.add(reuse(StateBinding.Type.NULLABLE_NUM, "num?", "null", "null", new PropertyValue.IntegerValue(BigInteger.valueOf(7)), "7"));
        cases.add(reuse(StateBinding.Type.NULLABLE_OBJECT, "Object?", "null", "null", new PropertyValue.StringValue("after"), "after"));
        return List.copyOf(cases);
    }
    private static Case reuse(StateBinding.Type type, String dartType, String initializer, String initialText,
            PropertyValue selected, String updatedText) {
        return new Case(StateBinding.Action.SELECT, false, Optional.of(selected), Optional.of(type), dartType, initializer, initialText, updatedText);
    }
    private record Scalar(PropertyValue value, String text) {}
    private record Case(StateBinding.Action action, boolean initiallySelected, Optional<PropertyValue> selectedValue,
            Optional<StateBinding.Type> reusedType, String dartType, String initializer, String initialText, String updatedText) {}
    private static String quoted(String text) { return "'" + text.replace("\\", "\\\\").replace("'", "\\'").replace("$", "\\$") + "'"; }
    private static DesignerCommandSession applied(DesignerCommandSession session, DesignerCommand command) {
        var result = session.apply(command);
        assertEquals(DesignerCommandStatus.APPLIED, result.status(), result.diagnostics().toString());
        return result.session();
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
    private static class EventWidgetLookup {
        static WidgetNode find(WidgetNode node, StableId id) {
            if (node.id().equals(id)) return node;
            for (WidgetSlot slot : node.slots().values()) {
                List<WidgetNode> children = slot instanceof WidgetSlot.ListSlot list ? list.children()
                        : ((WidgetSlot.SingleSlot) slot).child().stream().toList();
                for (WidgetNode child : children) {
                    WidgetNode found = find(child, id);
                    if (found != null) return found;
                }
            }
            return null;
        }
    }
}
