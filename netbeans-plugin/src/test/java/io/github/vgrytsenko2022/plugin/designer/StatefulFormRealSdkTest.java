package io.github.vgrytsenko2022.plugin.designer;

import io.github.vgrytsenko2022.dart.DartCandidateAnalyzer;
import io.github.vgrytsenko2022.dart.DartCandidateWarningPolicy;
import io.github.vgrytsenko2022.designer.catalog.BuiltInWidgetCatalog;
import io.github.vgrytsenko2022.designer.codec.FdDecodeResult;
import io.github.vgrytsenko2022.designer.codec.FdDocumentCodec;
import io.github.vgrytsenko2022.designer.command.CreateEventHandler;
import io.github.vgrytsenko2022.designer.command.DesignerCommandSession;
import io.github.vgrytsenko2022.designer.command.DesignerCommandStatus;
import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.WidgetClassKind;
import io.github.vgrytsenko2022.designer.template.DesignerFormTemplateFactory;
import io.github.vgrytsenko2022.designer.validation.ValidationResult;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Real analyzer ownership evidence and mounted Flutter State callback execution. */
@EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
class StatefulFormRealSdkTest {
    @TempDir Path project;

    @Test
    void generatedStatefulFormAndUserSetStateHandlerPassRealEvidenceAndRuntime() throws Exception {
        Path sdk = Path.of(System.getProperty("flutter.events.sdk")).toRealPath();
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        Path flutter = sdk.resolve(windows ? "bin/flutter.bat" : "bin/flutter");
        Path dart = sdk.resolve(windows ? "bin/cache/dart-sdk/bin/dart.exe" : "bin/cache/dart-sdk/bin/dart");
        Files.writeString(project.resolve("pubspec.yaml"), """
                name: stateful_form_contract
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
        run(List.of(flutter.toString(), "pub", "get", "--offline"), "pub-get.log");
        Path file = Files.createDirectories(project.resolve("lib")).resolve("home_page.dart");
        var factory = new DesignerFormTemplateFactory();
        var template = factory.create("home_page.dart", "HomePage", WidgetClassKind.STATEFUL);
        String initial = new String(template.dartBytes(), StandardCharsets.UTF_8).replace(
                "class _HomePageState extends State<HomePage> {",
                "class _HomePageState extends State<HomePage> {\n"
                + "  int _counter = 0;\n  int get counter => _counter;\n");
        byte[] baseline = initial.getBytes(StandardCharsets.UTF_8);
        Files.write(file, baseline);
        var catalog = BuiltInWidgetCatalog.getDefault();
        var codec = new FdDocumentCodec();
        var encoded = codec.encode(template.document());
        var opened = DesignerCommandSession.open(encoded, baseline, catalog);
        assertTrue(opened.ready(), () -> opened.diagnostics().toString());
        var result = opened.session().orElseThrow().apply(new CreateEventHandler(template.document().root().id(),
                new PropertyName("onDrawerChanged"), "_drawerChanged"));
        assertEquals(DesignerCommandStatus.APPLIED, result.status(), () -> result.diagnostics().toString());
        var session = result.session();
        String created = new String(session.current().dartCandidateBytes(), StandardCharsets.UTF_8);
        assertTrue(created.indexOf("void _drawerChanged") > created.indexOf("class _HomePageState"));
        byte[] edited = created.replace("// TODO: Handle onDrawerChanged.",
                "setState(() { _counter += isOpened ? 1 : -1; });").getBytes(StandardCharsets.UTF_8);
        var restage = session.prepareSourceRestage(session.current().preparedPair().orElseThrow(), edited);
        var integrity = session.current().preparedPair().orElseThrow().dartTransition().baseline();
        var current = new FlutterDesignerDocumentState.Current((FdDecodeResult.Current) codec.decode(encoded),
                new ValidationResult(List.of()), catalog, List.of(), List.of(),
                Optional.of(integrity.source()), Optional.of(integrity));
        var analyzer = new DartCandidateAnalyzer(dart, ignored -> { });
        for (var pair : List.of(session.current().preparedPair().orElseThrow(), restage.preparedPair())) {
            var ticket = PairSaveEvidenceGate.prepareAnalysis(current, pair, project, file,
                    DartCandidateWarningPolicy.ALLOW, sdk);
            assertEquals(List.of("StatefulWidget", "State"), ticket.request().symbolProbes().stream()
                    .limit(2).map(probe -> probe.expectedSymbolName()).toList());
            var operation = analyzer.analyze(ticket.request());
            try {
                var analysis = operation.result().toCompletableFuture().get(90, TimeUnit.SECONDS);
                var accepted = PairSaveEvidenceGate.evaluateAnalysis(ticket, analysis);
                assertTrue(accepted.ready(), () -> analysis + "\n" + accepted.diagnostics());
                assertTrue(analysis.symbolEvidence().stream().allMatch(evidence -> evidence.accepted()));
            } finally {
                operation.cancel();
            }
            assertArrayEquals(baseline, Files.readAllBytes(file), "Analyzer must not write the live pair.");
        }
        // Runtime test source is deliberately installed only inside this isolated fixture.
        Files.write(file, edited);
        Path runtime = Files.createDirectories(project.resolve("test")).resolve("stateful_form_test.dart");
        Files.writeString(runtime, """
                import 'package:flutter/material.dart';
                import 'package:flutter_test/flutter_test.dart';
                import 'package:stateful_form_contract/home_page.dart';

                void main() {
                  testWidgets('user callback executes on the mounted State owner', (tester) async {
                    await tester.pumpWidget(const MaterialApp(home: HomePage()));
                    final dynamic state = tester.state(find.byType(HomePage));
                    expect(state.counter, 0);
                    final scaffold = tester.widget<Scaffold>(find.byType(Scaffold));
                    scaffold.onDrawerChanged!(true);
                    await tester.pump();
                    expect(state.counter, 1);
                    scaffold.onDrawerChanged!(false);
                    await tester.pump();
                    expect(state.counter, 0);
                    expect(tester.takeException(), isNull);
                  });
                }
                """);
        run(List.of(flutter.toString(), "test", "--no-pub", runtime.toString()), "flutter-test.log");
    }

    private void run(List<String> command, String logName) throws Exception {
        Path log = project.resolve(logName);
        Process process = new ProcessBuilder(command).directory(project.toFile())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        boolean finished = process.waitFor(180, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            process.waitFor(10, TimeUnit.SECONDS);
        }
        String output = Files.readString(log);
        assertTrue(finished, "Timed out: " + command + "\n" + output);
        assertEquals(0, process.exitValue(), command + "\n" + output);
    }
}
