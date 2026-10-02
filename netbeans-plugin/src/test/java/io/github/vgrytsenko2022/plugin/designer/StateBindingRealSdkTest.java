package io.github.vgrytsenko2022.plugin.designer;

import io.github.vgrytsenko2022.dart.DartCandidateAnalyzer;
import io.github.vgrytsenko2022.dart.DartCandidateWarningPolicy;
import io.github.vgrytsenko2022.designer.catalog.BuiltInWidgetCatalog;
import io.github.vgrytsenko2022.designer.catalog.WidgetNodePrototypeFactory;
import io.github.vgrytsenko2022.designer.codec.FdDecodeResult;
import io.github.vgrytsenko2022.designer.codec.FdDocumentCodec;
import io.github.vgrytsenko2022.designer.command.CreateStateBinding;
import io.github.vgrytsenko2022.designer.command.RemoveStateBinding;
import io.github.vgrytsenko2022.designer.command.RemoveWidget;
import io.github.vgrytsenko2022.designer.command.RenameStateField;
import io.github.vgrytsenko2022.designer.command.DesignerCommandSession;
import io.github.vgrytsenko2022.designer.command.DesignerCommandStatus;
import io.github.vgrytsenko2022.designer.generation.DartRegionGenerator;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.validation.ValidationResult;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** End-to-end analyzer evidence and real Flutter controlled-value rebuilds. */
@EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
class StateBindingRealSdkTest {
    @TempDir Path project;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void allControlledFamiliesUpdateRuntimeValuesAndKeepDetachedSourceValid(boolean renameField) throws Exception {
        Path sdk = Path.of(System.getProperty("flutter.events.sdk")).toRealPath();
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        Path flutter = sdk.resolve(windows ? "bin/flutter.bat" : "bin/flutter");
        Path dart = sdk.resolve(windows ? "bin/cache/dart-sdk/bin/dart.exe" : "bin/cache/dart-sdk/bin/dart");
        Files.writeString(project.resolve("pubspec.yaml"), """
                name: state_binding_contract
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
        Files.writeString(lib.resolve("choice.dart"), "enum Choice { first, second }\n");
        run(List.of(flutter.toString(), "pub", "get", "--offline"), "pub-get.log");
        List<Case> cases = new ArrayList<>(List.of(
                new Case("Checkbox", false, "", false), new Case("Checkbox", true, "", false),
                new Case("CheckboxListTile", false, "", false), new Case("CheckboxListTile", true, "", false),
                new Case("Switch", false, "", false), new Case("Slider", false, "", false),
                new Case("RangeSlider", false, "", false),
                new Case("Checkbox", false, "", true), new Case("CheckboxListTile", false, "", true),
                new Case("Switch", false, "", true), new Case("Slider", false, "", true)));
        for (String type : List.of("String", "int", "double", "num", "bool", "Object", "Choice")) {
            cases.add(new Case("RadioGroup", false, type, false));
            cases.add(new Case("RadioGroup", true, type, false));
        }
        var analyzer = new DartCandidateAnalyzer(dart, ignored -> { });
        var catalog = BuiltInWidgetCatalog.getDefault();
        var codec = new FdDocumentCodec();
        StringBuilder imports = new StringBuilder("import 'package:flutter/material.dart';\n"
                + "import 'package:flutter_test/flutter_test.dart';\n"
                + "import 'package:state_binding_contract/choice.dart';\n");
        StringBuilder runtime = new StringBuilder("void main() {\n");
        for (int index = 0; index < cases.size(); index++) {
            Case entry = cases.get(index);
            String fileName = "case_" + index + ".dart";
            DesignerCommandSession initial = open(entry, fileName);
            Path file = lib.resolve(fileName);
            byte[] baseline = initial.current().dartCandidateBytes();
            Files.write(file, baseline);
            StableId target = ((WidgetSlot.ListSlot) initial.current().document().root().slots()
                    .get(new SlotName("children"))).children().getFirst().id();
            var created = initial.apply(new CreateStateBinding(target, "_value", "_valueChanged"));
            assertEquals(DesignerCommandStatus.APPLIED, created.status(), () -> entry + ": " + created.diagnostics());
            var session = created.session();
            if (entry.radioType.equals("Choice")) {
                var siblings = ((WidgetSlot.ListSlot) session.current().document().root().slots().get(new SlotName("children"))).children();
                var sourceBinding = siblings.getFirst().stateBinding().orElseThrow();
                var consumer = session.apply(new io.github.vgrytsenko2022.designer.command.BindPropertyToState(siblings.getLast().id(),
                        new PropertyName("data"), new StatePropertyBinding(sourceBinding.fieldName(), sourceBinding.type(),
                                sourceBinding.referenceType(), StatePropertyBinding.Transform.TO_STRING)));
                assertEquals(DesignerCommandStatus.APPLIED, consumer.status(), consumer.diagnostics().toString());
                session = consumer.session();
            }
            if (renameField) {
                var renamed = session.apply(new RenameStateField(target, "_value", "_selection"));
                assertEquals(DesignerCommandStatus.APPLIED, renamed.status(), renamed.diagnostics().toString());
                session = renamed.session();
                assertTrue(new String(session.current().dartCandidateBytes(), StandardCharsets.UTF_8)
                        .contains("void _valueChanged("), "Field rename retains the event method name");
            }
            var pair = session.current().preparedPair().orElseThrow();
            var current = new FlutterDesignerDocumentState.Current((FdDecodeResult.Current) codec.decode(initial.current().fdSnapshot()),
                    new ValidationResult(List.of()), catalog, List.of(), List.of(),
                    Optional.of(pair.dartTransition().baseline().source()), Optional.of(pair.dartTransition().baseline()));
            var ticket = PairSaveEvidenceGate.prepareAnalysis(current, pair, project, file, DartCandidateWarningPolicy.ALLOW, sdk);
            var fieldProbes = ticket.request().symbolProbes().stream()
                    .filter(probe -> probe.expectedSymbolName().equals(renameField ? "_selection" : "_value")).toList();
            assertEquals(entry.radioType.equals("Choice") ? 2 : 1, fieldProbes.size());
            assertTrue(fieldProbes.stream().allMatch(probe -> probe.expectedTargetKind().equals(Optional.of("FIELD"))));
            assertTrue(fieldProbes.stream().allMatch(probe -> probe.staticTypeProbe().isPresent()));
            var operation = analyzer.analyze(ticket.request());
            try {
                var analysis = operation.result().toCompletableFuture().get(90, TimeUnit.SECONDS);
                var accepted = PairSaveEvidenceGate.evaluateAnalysis(ticket, analysis);
                assertTrue(accepted.ready(), () -> entry + "\n" + analysis + "\n" + accepted.diagnostics());
            } finally { operation.cancel(); }
            assertArrayEquals(baseline, Files.readAllBytes(file), "Analyzer must never write a live source");
            Files.write(file, session.current().dartCandidateBytes());
            var removed = session.apply(new RemoveStateBinding(target));
            assertEquals(DesignerCommandStatus.APPLIED, removed.status(), () -> removed.diagnostics().toString());
            Path detached = lib.resolve("detached_" + index + ".dart");
            Files.write(detached, removed.session().current().dartCandidateBytes());
            assertTrue(Files.readString(detached).contains("_valueChanged"));
            var widgetRemoved = removed.session().apply(new RemoveWidget(target));
            assertEquals(DesignerCommandStatus.APPLIED, widgetRemoved.status(), () -> widgetRemoved.diagnostics().toString());
            Files.write(lib.resolve("removed_" + index + ".dart"), widgetRemoved.session().current().dartCandidateBytes());
            imports.append("import 'package:state_binding_contract/").append(fileName).append("' as case").append(index).append(";\n");
            runtime.append("  testWidgets('controlled binding ").append(index).append("', (tester) async {\n")
                    .append("    await tester.pumpWidget(MaterialApp(")
                    .append(entry.adaptive ? "theme: ThemeData(platform: TargetPlatform.iOS), " : "")
                    .append("home: Scaffold(body: case").append(index).append(".Sample())));\n")
                    .append(entry.runtimeAssertions()).append("    expect(tester.takeException(), isNull);\n  });\n");
        }
        runtime.append("}\n");
        Path test = Files.createDirectories(project.resolve("test")).resolve("state_binding_test.dart");
        Files.writeString(test, imports.toString() + runtime);
        run(List.of(dart.toString(), "analyze", "--no-fatal-warnings", lib.toString()), "analyze-detached.log");
        run(List.of(flutter.toString(), "test", "--no-pub", test.toString()), "flutter-test.log");
    }

    private static DesignerCommandSession open(Case entry, String fileName) throws Exception {
        var catalog = BuiltInWidgetCatalog.getDefault();
        var kind = new WidgetTypeId((entry.widget.equals("RadioGroup") ? "flutter.widgets." : "flutter.material.") + entry.widget);
        WidgetNode target = WidgetNodePrototypeFactory.create(catalog.find(kind).orElseThrow(), StableId.random());
        var properties = new LinkedHashMap<>(target.properties());
        var slots = new LinkedHashMap<>(target.slots());
        if (entry.tristate && !entry.widget.equals("RadioGroup")) {
            properties.put(new PropertyName("tristate"), new PropertyValue.BooleanValue(true));
            properties.put(new PropertyName("value"), new PropertyValue.NullValue());
        }
        if (entry.adaptive) properties.put(new PropertyName("variant"), new PropertyValue.StringValue("adaptive"));
        if (entry.widget.equals("RadioGroup")) {
            PropertyValue type = entry.radioType.equals("Choice") ? new PropertyValue.DartObjectReferenceValue(
                    Optional.of("package:state_binding_contract/choice.dart"), "Choice", Optional.empty(),
                    PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()) : new PropertyValue.StringValue(entry.radioType);
            properties.put(new PropertyName("valueType"), type);
            properties.put(new PropertyName("nullableValueType"), new PropertyValue.BooleanValue(entry.tristate));
            slots.put(new SlotName("child"), WidgetSlot.SingleSlot.of(WidgetNodePrototypeFactory.create(
                    catalog.find(new WidgetTypeId("flutter.widgets.SizedBox")).orElseThrow(), StableId.random())));
        }
        target = new WidgetNode(target.id(), target.type(), properties, slots);
        List<WidgetNode> children = entry.radioType.equals("Choice") ? List.of(target, WidgetNodePrototypeFactory.create(
                catalog.find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow(), StableId.random())) : List.of(target);
        WidgetNode root = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(children)));
        return openRoot(root, fileName, "");
    }

    static DesignerCommandSession openRoot(WidgetNode root, String fileName, String userMembers) throws Exception {
        var catalog = BuiltInWidgetCatalog.getDefault();
        StableId id = StableId.random();
        var provisional = new DesignerDocument(id, descriptor(fileName, "0".repeat(64), "0".repeat(64)), root);
        var generation = new DartRegionGenerator().generate(provisional, catalog);
        assertTrue(generation.successful(), () -> generation.diagnostics().toString());
        var generated = generation.generated().orElseThrow();
        var document = new DesignerDocument(id, descriptor(fileName, generated.imports().normalizedSha256(),
                generated.build().normalizedSha256()), root);
        byte[] source = ("// <netbeans-flutter-designer region=\"imports\">\n" + generated.imports().payload()
                + "// </netbeans-flutter-designer>\nclass Sample extends StatefulWidget {\n"
                + "  const Sample({super.key});\n  @override\n  State<Sample> createState() => Storage();\n}\n"
                + "class Storage extends State<Sample> {\n" + userMembers + "\n  // <netbeans-flutter-designer region=\"build\">\n"
                + generated.build().payload() + "  // </netbeans-flutter-designer>\n}\n").getBytes(StandardCharsets.UTF_8);
        var result = DesignerCommandSession.open(new FdDocumentCodec().encode(document), source, catalog);
        assertTrue(result.ready(), () -> result.diagnostics().toString());
        return result.session().orElseThrow();
    }

    private static DartSourceDescriptor descriptor(String fileName, String imports, String build) {
        return new DartSourceDescriptor(fileName, "Sample", WidgetClassKind.STATEFUL, Optional.of(DartRegionGenerator.PROFILE_ID),
                new ManagedRegions(new ManagedRegion(imports), new ManagedRegion(build)));
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

    private record Case(String widget, boolean tristate, String radioType, boolean adaptive) {
        String runtimeAssertions() {
            String type = widget.equals("RadioGroup") ? "RadioGroup<" + radioType + (tristate ? "?" : "") + ">" : widget;
            String finder = widget.equals("CheckboxListTile") ? "find.byType(CheckboxListTile)" : "find.byType(" + type + ")";
            String value = switch (widget) {
                case "Slider" -> "0.25";
                case "RangeSlider" -> "const RangeValues(0.2, 0.8)";
                case "RadioGroup" -> switch (radioType) {
                    case "int" -> "7";
                    case "double", "num" -> "0.5";
                    case "bool" -> "true";
                    case "Choice" -> "Choice.second";
                    default -> "'selected'";
                };
                default -> "true";
            };
            String property = widget.equals("RadioGroup") ? "groupValue" : widget.equals("RangeSlider") ? "values" : "value";
            String assertion = widget.equals("RangeSlider")
                    ? "    expect(updated.values.start, 0.2);\n    expect(updated.values.end, 0.8);\n"
                    : "    expect(updated." + property + ", " + value + ");\n";
            String code = "    final first = tester.widget<" + type + ">(" + finder + ");\n"
                    + "    first.onChanged" + (widget.equals("RadioGroup") ? "" : "!") + "(" + value + ");\n"
                    + "    await tester.pump();\n    final updated = tester.widget<" + type + ">(" + finder + ");\n" + assertion;
            if (tristate || widget.equals("RadioGroup")) {
                code += "    updated.onChanged" + (widget.equals("RadioGroup") ? "" : "!") + "(null);\n"
                        + "    await tester.pump();\n    expect(tester.widget<" + type + ">(" + finder + ")." + property + ", isNull);\n";
            }
            return code;
        }
    }
}
