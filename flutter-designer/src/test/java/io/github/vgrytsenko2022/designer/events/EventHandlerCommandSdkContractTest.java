package io.github.vgrytsenko2022.designer.events;

import io.github.vgrytsenko2022.designer.catalog.BuiltInWidgetCatalog;
import io.github.vgrytsenko2022.designer.catalog.WidgetCatalog;
import io.github.vgrytsenko2022.designer.catalog.WidgetNodePrototypeFactory;
import io.github.vgrytsenko2022.designer.codec.FdDocumentCodec;
import io.github.vgrytsenko2022.designer.command.CreateEventHandler;
import io.github.vgrytsenko2022.designer.command.DesignerCommand;
import io.github.vgrytsenko2022.designer.command.DesignerCommandSession;
import io.github.vgrytsenko2022.designer.command.DesignerCommandStatus;
import io.github.vgrytsenko2022.designer.command.RemoveWidget;
import io.github.vgrytsenko2022.designer.command.ResetProperty;
import io.github.vgrytsenko2022.designer.generation.DartRegionGenerator;
import io.github.vgrytsenko2022.designer.model.DartSourceDescriptor;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.ManagedRegion;
import io.github.vgrytsenko2022.designer.model.ManagedRegions;
import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.SlotName;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.WidgetClassKind;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetSlot;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

/** Real SDK proof of complete generated command candidates, not isolated stubs. */
@EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
class EventHandlerCommandSdkContractTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final StableId TARGET = StableId.parse("798b3dfa-9dd7-488c-934f-ae4e37242884");
    @TempDir Path project;

    @Test
    void eventCommandsRemainValidAfterDisconnectAndWidgetRemoval() throws Exception {
        Path sdk = Path.of(System.getProperty("flutter.events.sdk")).toAbsolutePath();
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        Path flutter = sdk.resolve(windows ? "bin/flutter.bat" : "bin/flutter");
        Path dart = sdk.resolve(windows ? "bin/cache/dart-sdk/bin/dart.exe" : "bin/cache/dart-sdk/bin/dart");
        assertTrue(Files.isRegularFile(flutter), "Missing Flutter SDK: " + flutter);
        assertTrue(Files.isRegularFile(dart), "Missing Dart SDK: " + dart);
        Files.writeString(project.resolve("pubspec.yaml"), """
                name: designer_event_command_contract
                publish_to: none
                environment:
                  sdk: '>=3.11.0 <4.0.0'
                dependencies:
                  flutter:
                    sdk: flutter
                """);
        List<Case> cases = List.of(
                new Case("Scaffold", "onDrawerChanged", false),
                new Case("TextField", "onTapOutside", false),
                new Case("TextField", "onAppPrivateCommand", false),
                new Case("RangeSlider", "onChanged", false),
                new Case("RefreshIndicator", "onRefresh", false),
                new Case("RefreshIndicator", "onStatusChange", true),
                new Case("Checkbox", "onChanged", false),
                new Case("Switch", "onChanged", false));
        Path sources = Files.createDirectory(project.resolve("lib"));
        int index = 0;
        for (Case testCase : cases) {
            DesignerCommandSession initial = open(testCase);
            DesignerCommandSession created = apply(initial,
                    new CreateEventHandler(TARGET, new PropertyName(testCase.event), "_handler"));
            DesignerCommandSession disconnected = apply(created,
                    new ResetProperty(TARGET, new PropertyName(testCase.event)));
            DesignerCommandSession removed = apply(disconnected, new RemoveWidget(TARGET));
            String stem = "case_" + index++ + "_" + testCase.widget.toLowerCase()
                    + "_" + testCase.event.toLowerCase();
            save(sources.resolve(stem + "_initial.dart"), initial);
            save(sources.resolve(stem + "_created.dart"), created);
            save(sources.resolve(stem + "_disconnected.dart"), disconnected);
            save(sources.resolve(stem + "_removed.dart"), removed);
            var retainedMethod = DartEventHandlerSource.methods(
                    removed.current().dartCandidateBytes(), "Sample").stream()
                    .filter(method -> method.name().equals("_handler")).findFirst();
            assertTrue(retainedMethod.isPresent(), testCase + " removed user method");
            assertFalse(retainedMethod.orElseThrow().generated());
            assertFalse(source(removed).contains(testCase.event + ": _handler"));
            assertTrue(source(created).contains(testCase.event + ": _handler"), testCase.toString());
            var descriptor = WidgetEventCatalog.find(
                    new WidgetTypeId("flutter.material." + testCase.widget),
                    new PropertyName(testCase.event)).orElseThrow();
            for (String uri : descriptor.signature().importUris()) {
                assertTrue(source(removed).contains("import '" + uri + "';"),
                        testCase + " lost retained handler import " + uri);
            }
            DesignerCommandSession reopened = DesignerCommandSession.open(
                    removed.current().fdSnapshot(), removed.current().dartCandidateBytes(), CATALOG)
                    .session().orElseThrow();
            assertArrayEquals(removed.current().dartCandidateBytes(), reopened.current().dartCandidateBytes());
        }
        List<String> get = new ArrayList<>();
        if (windows) get.addAll(List.of("cmd.exe", "/d", "/c"));
        get.addAll(List.of(flutter.toString(), "pub", "get", "--offline"));
        run(get, "pub-get.log");
        // Production's ALLOW warning policy rejects errors but admits warnings.
        // Keep all diagnostics visible: duplicate imports must not be hidden.
        String analysis = run(List.of(dart.toString(), "analyze", "--no-fatal-warnings",
                sources.toString()), "analyze.log");
        assertFalse(analysis.lines().anyMatch(line -> line.stripLeading().startsWith("error -")), analysis);
        long duplicateImports = analysis.lines().filter(line -> line.contains("duplicate_import")).count();
        System.out.println("Event command SDK contract: " + (cases.size() * 4)
                + " complete generated candidates; duplicate_import diagnostics=" + duplicateImports
                + " (retained signature imports, production warning policy ALLOW).");
        if (!analysis.contains("No issues found")) System.out.println(analysis);
    }

    private static DesignerCommandSession open(Case testCase) throws Exception {
        var definition = CATALOG.find(new WidgetTypeId("flutter.material." + testCase.widget)).orElseThrow();
        Map<PropertyName, PropertyValue> creation = testCase.noSpinner
                ? Map.of(new PropertyName("variant"), new PropertyValue.StringValue("noSpinner")) : Map.of();
        WidgetNode target = WidgetNodePrototypeFactory.create(definition, TARGET, creation);
        if (testCase.widget.equals("RefreshIndicator")) {
            var slots = new LinkedHashMap<>(target.slots());
            slots.put(new SlotName("child"), new WidgetSlot.SingleSlot(Optional.of(
                    WidgetNodePrototypeFactory.create(CATALOG.find(
                            new WidgetTypeId("flutter.widgets.ListView")).orElseThrow(), StableId.random()))));
            target = new WidgetNode(target.id(), target.type(), target.properties(), slots);
        }
        WidgetNode root = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Column"),
                Map.of(), Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(target))));
        StableId documentId = StableId.random();
        var provisional = new DesignerDocument(documentId, descriptor("0".repeat(64), "0".repeat(64)), root);
        var generation = new DartRegionGenerator().generate(provisional, CATALOG);
        assertTrue(generation.successful(), testCase + ": " + generation.diagnostics());
        var generated = generation.generated().orElseThrow();
        var document = new DesignerDocument(documentId, descriptor(generated.imports().normalizedSha256(),
                generated.build().normalizedSha256()), root);
        byte[] source = ("// <netbeans-flutter-designer region=\"imports\">\n" + generated.imports().payload()
                + "// </netbeans-flutter-designer>\n\nclass Sample extends StatelessWidget {\n"
                + "  // <netbeans-flutter-designer region=\"build\">\n" + generated.build().payload()
                + "  // </netbeans-flutter-designer>\n}\n").getBytes(StandardCharsets.UTF_8);
        var result = DesignerCommandSession.open(new FdDocumentCodec().encode(document), source, CATALOG);
        assertTrue(result.ready(), testCase + ": " + result.diagnostics());
        return result.session().orElseThrow();
    }

    private static DartSourceDescriptor descriptor(String imports, String build) {
        return new DartSourceDescriptor("sample.dart", "Sample", WidgetClassKind.STATELESS,
                Optional.of(DartRegionGenerator.PROFILE_ID),
                new ManagedRegions(new ManagedRegion(imports), new ManagedRegion(build)));
    }

    private static DesignerCommandSession apply(DesignerCommandSession session, DesignerCommand command) {
        var result = session.apply(command);
        assertEquals(DesignerCommandStatus.APPLIED, result.status(), result.diagnostics().toString());
        return result.session();
    }

    private static String source(DesignerCommandSession session) {
        return new String(session.current().dartCandidateBytes(), StandardCharsets.UTF_8);
    }

    private static void save(Path file, DesignerCommandSession session) throws IOException {
        Files.write(file, session.current().dartCandidateBytes());
    }

    private String run(List<String> command, String logName) throws IOException, InterruptedException {
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
        return output;
    }

    private record Case(String widget, String event, boolean noSpinner) { }
}
