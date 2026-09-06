package dev.flutter.netbeans.plugin.designer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import dev.flutter.netbeans.dart.*;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.generation.*;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.pair.*;
import dev.flutter.netbeans.designer.source.*;
import dev.flutter.netbeans.designer.transition.*;
import dev.flutter.netbeans.plugin.designer.properties.IconButtonPropertyContractTest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Full IconButton generation, same-file SDK proof and exact FD reopen without widening safety budgets. */
class IconButtonCandidateRealSdkTest {
    @TempDir Path workspace;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final StableId DOCUMENT_ID = StableId.parse("75000000-0000-4000-8000-000000000001");
    private static final WidgetTypeId ICON_BUTTON = new WidgetTypeId("flutter.material.IconButton");

    @Test
    void allFourDenseConstructorsRetainEveryStyleOccurrenceWithinUnchangedSafetyBudgets() throws Exception {
        var cases = new ArrayList<Candidate>();
        int index = 0;
        for (String variant : List.of("standard", "filled", "filledTonal", "outlined")) {
            var values = IconButtonPropertyContractTest.full(variant, index == 1 || index == 2, index == 2, index == 2);
            values.put(p("enabled"), new PropertyValue.BooleanValue(true));
            // Repeated item IDs across independent state-list paths must remain distinct probes.
            var item = StableId.parse("75000000-0000-4000-8000-000000000002");
            values.replaceAll((name, value) -> switch (value) {
                case PropertyValue.ShadowListValue ignored -> new PropertyValue.ShadowListValue(List.of(
                        new PropertyValue.ShadowListValue.Shadow(item, new ColorSource.Literal(0xff123456L),
                                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE)));
                case PropertyValue.FontFeatureListValue ignored -> new PropertyValue.FontFeatureListValue(List.of(
                        new PropertyValue.FontFeatureListValue.FontFeature(item, "kern", 1)));
                case PropertyValue.FontVariationListValue ignored -> new PropertyValue.FontVariationListValue(List.of(
                        new PropertyValue.FontVariationListValue.FontVariation(item, "wght", BigDecimal.valueOf(500))));
                default -> value;
            });
            assertTrue(values.size() <= 512, variant + ": " + values.size());
            cases.add(new Candidate(variant, button(variant, values), true));
            index++;
        }
        validate(cases);
    }

    @Test
    void wholeStyleAndReferenceFactoriesRetainAllFourConstructorsAndNullableSelection() throws Exception {
        var cases = new ArrayList<Candidate>();
        int index = 0;
        var selected = List.<PropertyValue>of(new PropertyValue.NullValue(),
                new PropertyValue.BooleanValue(false), new PropertyValue.BooleanValue(true), new PropertyValue.NullValue());
        for (String variant : List.of("standard", "filled", "filledTonal", "outlined")) {
            var values = Map.<PropertyName, PropertyValue>of(p("style"), reference("_style"),
                    p("isSelected"), selected.get(index++), p("focusNode"), factory("_makeFocusNode"),
                    p("statesController"), factory("_makeStatesController"), p("mouseCursor"), reference("_mouseCursor"),
                    p("onPressed"), reference("_onPressed"), p("onHover"), reference("_onHover"),
                    p("onLongPress"), reference("_onLongPress"), p("enabled"), new PropertyValue.BooleanValue(true));
            cases.add(new Candidate("whole-" + variant, button(variant, values), true));
        }
        validate(cases);
    }

    @Test
    void strictWholeStyleWitnessRejectsDynamicAndNullableReferences() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (String style : List.of("_style", "_dynamicStyle", "_nullableStyle")) {
            cases.add(new Candidate(style, button("outlined", Map.of(p("style"), reference(style))), style.equals("_style")));
        }
        validate(cases);
    }

    @Test
    void sparseCompoundDefaultsDirectAxesAndInactiveCallbacksUseTheRealIconButtonContract() throws Exception {
        var buttons = new ArrayList<WidgetNode>();
        for (String variant : List.of("standard", "filled", "filledTonal", "outlined")) {
            var values = new LinkedHashMap<PropertyName, PropertyValue>();
            values.put(p("constraints"), new PropertyValue.BoxConstraintsValue(
                    BigDecimal.valueOf(36), Optional.of(BigDecimal.valueOf(80)),
                    BigDecimal.valueOf(38), Optional.of(BigDecimal.valueOf(84))));
            values.put(p("visualDensityHorizontal"), new PropertyValue.DoubleValue(BigDecimal.ONE));
            values.put(p("visualDensityVertical"), new PropertyValue.DoubleValue(BigDecimal.valueOf(-1)));
            values.put(p("styleMinimumWidth"), new PropertyValue.DoubleValue(BigDecimal.valueOf(48)));
            values.put(p("styleVisualDensityHorizontal"), new PropertyValue.DoubleValue(BigDecimal.valueOf(-2)));
            values.put(p("stylePressedSideWidth"), new PropertyValue.DoubleValue(BigDecimal.valueOf(2)));
            values.put(p("enabled"), new PropertyValue.BooleanValue(false));
            // Disabled handlers are retained in FD but must cause no import, evaluation or proof.
            values.put(p("onPressed"), externalReference("package:not_installed/disabled.dart", "disabledPressed"));
            values.put(p("onLongPress"), externalReference("package:not_installed/disabled.dart", "disabledLongPress"));
            buttons.add(button(variant, values));
            buttons.add(button(variant, Map.of(p("isSelected"), new PropertyValue.NullValue())));
        }
        var root = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(buttons)));
        validate(List.of(new Candidate("sparse-compounds-and-disabled", root, true)));
    }

    private record Candidate(String name, WidgetNode root, boolean accepted) { }

    private void validate(List<Candidate> candidates) throws Exception {
        Path executable = configured("dart.executable"), sdk = configured("flutter.sdk"), cache = configured("pub.cache");
        Path project = Files.createDirectories(workspace.resolve("button_probe"));
        Path lib = Files.createDirectories(project.resolve("lib"));
        configure(project, sdk, cache);
        Path file = lib.resolve("home_page.dart");
        var codec = new FdDocumentCodec();
        var generator = new DartRegionGenerator();
        var catalog = BuiltInWidgetCatalog.getDefault();
        var seed = descriptor("", "");
        var seedNode = text("Seed");
        var baselineGenerated = generator.generate(document(seed, seedNode), catalog).generated().orElseThrow();
        var baselineDescriptor = descriptor(baselineGenerated.imports().payload(), baselineGenerated.build().payload());
        var baselineDocument = document(baselineDescriptor, seedNode);
        byte[] baselineBytes = source(baselineGenerated);
        Files.write(file, baselineBytes);
        byte[] pubspec = Files.readAllBytes(project.resolve("pubspec.yaml"));
        byte[] packageConfig = Files.readAllBytes(project.resolve(".dart_tool/package_config.json"));
        var scanner = new DartSourceIntegrityScanner();
        var baseline = new DartThreeWayIntegrityGate(scanner).evaluate(
                scanner.scan(baselineBytes, baselineDescriptor), baselineDescriptor, generator.generate(baselineDocument, catalog));
        var decoded = (FdDecodeResult.Current) codec.decode(codec.encode(baselineDocument));
        var analyzer = new DartCandidateAnalyzer(executable, ignored -> { });
        assertEquals(45, DartCandidateAnalysisLimits.DEFAULT.totalTimeout().toSeconds());
        long version = 1;
        for (var entry : candidates) {
            var generation = generator.generate(document(baselineDescriptor, entry.root()), catalog);
            assertTrue(generation.generated().isPresent(), () -> entry.name() + ": " + generation);
            var transition = new DartSourceTransitionPlanner(scanner).plan(
                    baseline, baselineBytes, baselineDescriptor, generation).plan().orElseThrow();
            var prepared = new DesignerPairPreparationPlanner().prepare(decoded.original(),
                    document(transition.prospectiveDescriptor(), entry.root()), transition).preparedPair().orElseThrow();
            var probes = GeneratedDartSymbolProbePlanner.plan(prepared, sdk, project);
            assertFalse(probes.isEmpty());
            assertTrue(probes.size() <= 2048);
            assertEquals(probes.size(), probes.stream().map(DartSymbolProbe::id).distinct().count());
            if (entry.name().startsWith("_")) {
                assertEquals(1, probes.stream().flatMap(probe -> probe.staticTypeProbe().stream()).count(), entry.name());
            }
            byte[] candidate = prepared.prospectiveDartBytes();
            String source = new String(candidate, StandardCharsets.UTF_8);
            assertTrue(source.contains("IconButton"), source);
            assertFalse(source.contains("_IconButtonM3"), "Do not depend on private SDK implementation types");
            assertFalse(source.contains("FilledButton("), source);
            assertFalse(source.contains("TextButton("), source);
            assertFalse(source.contains("OutlinedButton("), source);
            if (List.of("standard", "filled", "filledTonal", "outlined").contains(entry.name())) {
                assertTrue(source.contains("IconButton" + (entry.name().equals("standard") ? "" : "." + entry.name()) + "("), source);
                assertTrue(source.contains("IconButtonTheme"), "Complete compound fields against this component's theme");
                assertTrue(probes.size() > 750, entry.name() + ": keep all " + probes.size() + " occurrences");
            }
            if (entry.name().equals("sparse-compounds-and-disabled")) {
                assertFalse(source.contains("package:not_installed/disabled.dart"), source);
                assertFalse(source.contains("disabledPressed"), source);
                assertFalse(source.contains("disabledLongPress"), source);
            }
            var request = new DartCandidateAnalysisRequest(project, file, source, version++,
                    HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(candidate)),
                    DartCandidateWarningPolicy.ALLOW, probes, generation.generated().orElseThrow().candidateCapacityBudget());
            var result = analyzer.analyze(request).result().toCompletableFuture().get(60, TimeUnit.SECONDS);
            if (entry.accepted()) {
                assertEquals(DartCandidateAnalysisStatus.PASSED, result.status(), () -> entry.name() + ": " + result.diagnostics()
                        + " issue=" + result.issue() + " rejected=" + result.symbolEvidence().stream().filter(v -> !v.accepted()).limit(5).toList());
                assertEquals(probes.size(), result.symbolEvidence().size());
                assertTrue(result.symbolEvidence().stream().allMatch(DartSymbolEvidence::accepted));
            } else {
                assertNotEquals(DartCandidateAnalysisStatus.PASSED, result.status(), entry.name());
                assertTrue(result.symbolEvidence().stream().anyMatch(v -> !v.accepted()
                        && v.probe().expectedSymbolName().equals(entry.name())
                        && v.probe().staticTypeProbe().map(type -> type.expectedDartType().equals("ButtonStyle")).orElse(false)),
                        () -> entry.name() + ": " + result);
                var otherTyped = result.symbolEvidence().stream().filter(v -> v.probe().staticTypeProbe().isPresent()
                        && !v.probe().expectedSymbolName().equals(entry.name())).toList();
                assertEquals(0, otherTyped.size());
                assertTrue(otherTyped.stream().allMatch(DartSymbolEvidence::accepted), () -> entry.name() + ": " + otherTyped);
            }
            var reopened = (FdDecodeResult.Current) codec.decode(prepared.prospectiveFdBytes());
            assertEquals(entry.root(), reopened.document().root());
            assertArrayEquals(baselineBytes, Files.readAllBytes(file));
            assertArrayEquals(pubspec, Files.readAllBytes(project.resolve("pubspec.yaml")));
            assertArrayEquals(packageConfig, Files.readAllBytes(project.resolve(".dart_tool/package_config.json")));
            assertFalse(Files.exists(project.resolve("analysis_options.yaml")));
            assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
        }
    }

    private static PropertyName p(String value) { return new PropertyName(value); }
    private static PropertyValue.DartObjectReferenceValue reference(String name) {
        return new PropertyValue.DartObjectReferenceValue(Optional.empty(), name, Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }
    private static WidgetNode text(String value) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(p("data"), new PropertyValue.StringValue(value)), Map.of());
    }
    private static PropertyValue.DartObjectReferenceValue factory(String name) {
        return new PropertyValue.DartObjectReferenceValue(Optional.empty(), name, Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(false));
    }
    private static PropertyValue.DartObjectReferenceValue externalReference(String uri, String name) {
        return new PropertyValue.DartObjectReferenceValue(Optional.of(uri), name, Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }
    private static WidgetNode button(String variant, Map<PropertyName, PropertyValue> values) {
        var properties = new LinkedHashMap<>(values);
        properties.put(p("variant"), new PropertyValue.StringValue(variant));
        properties.putIfAbsent(p("enabled"), new PropertyValue.BooleanValue(false));
        return new WidgetNode(StableId.random(), ICON_BUTTON, properties,
                Map.of(new SlotName("icon"), WidgetSlot.SingleSlot.of(text("Icon")),
                        new SlotName("selectedIcon"), WidgetSlot.SingleSlot.of(text("Selected icon"))));
    }

    private static DesignerDocument document(DartSourceDescriptor descriptor, WidgetNode root) {
        return new DesignerDocument(DOCUMENT_ID, descriptor, root);
    }

    private static DartSourceDescriptor descriptor(String imports, String build) {
        return new DartSourceDescriptor("home_page.dart", "HomePage", WidgetClassKind.STATELESS, Optional.empty(),
                new ManagedRegions(new ManagedRegion(DartManagedRegionHashing.normalizedSha256(imports)),
                        new ManagedRegion(DartManagedRegionHashing.normalizedSha256(build))));
    }

    private static byte[] source(GeneratedDartRegions generated) {
        return ("// <netbeans-flutter-designer region=\"imports\">\n" + generated.imports().payload()
                + "// </netbeans-flutter-designer>\n"
                + "void _onPressed() {}\nvoid _onLongPress() {}\nvoid _onHover(bool value) {}\n"
                + "final _focusNode = FocusNode();\nfinal _statesController = WidgetStatesController();\n"
                + "FocusNode _makeFocusNode() => FocusNode();\nWidgetStatesController _makeStatesController() => WidgetStatesController();\n"
                + "const _style = ButtonStyle();\ndynamic _dynamicStyle = const ButtonStyle();\nButtonStyle? _nullableStyle = const ButtonStyle();\n"
                + "final _mouseCursor = SystemMouseCursors.click;\n"
                + "Widget _styleBackgroundBuilder(BuildContext context, Set<WidgetState> states, Widget? child) => child ?? const SizedBox.shrink();\n"
                + "Widget _styleForegroundBuilder(BuildContext context, Set<WidgetState> states, Widget? child) => child ?? const SizedBox.shrink();\n"
                + "class HomePage extends StatelessWidget {\n"
                + "  // <netbeans-flutter-designer region=\"build\">\n" + generated.build().payload()
                + "  // </netbeans-flutter-designer>\n}\n").getBytes(StandardCharsets.UTF_8);
    }

    private static Path configured(String name) {
        String value = System.getProperty(name, "").strip();
        assumeTrue(!value.isEmpty(), "Set -D" + name + " for the pinned Flutter SDK gate.");
        Path path = Path.of(value).toAbsolutePath().normalize();
        assertTrue(Files.exists(path), name + ": " + path);
        return path;
    }

    private static void configure(Path project, Path sdk, Path cache) throws Exception {
        var config = JSON.createObjectNode().put("configVersion", 2);
        var packages = config.putArray("packages");
        addPackage(packages, "button_probe", project, "3.10");
        addPackage(packages, "flutter", sdk.resolve("packages/flutter"), "3.10");
        addPackage(packages, "sky_engine", sdk.resolve("bin/cache/pkg/sky_engine"), "3.10");
        for (var dependency : Map.of("characters", "1.4.1", "collection", "1.19.1",
                "material_color_utilities", "0.13.0", "meta", "1.18.0", "vector_math", "2.2.0").entrySet()) {
            addPackage(packages, dependency.getKey(), cache.resolve("hosted/pub.dev")
                    .resolve(dependency.getKey() + "-" + dependency.getValue()), "3.4");
        }
        Path tool = Files.createDirectories(project.resolve(".dart_tool"));
        Files.writeString(tool.resolve("package_config.json"), JSON.writeValueAsString(config), StandardCharsets.UTF_8);
        Files.writeString(project.resolve("pubspec.yaml"), "name: button_probe\nenvironment:\n  sdk: ^3.10.0\n"
                + "dependencies:\n  flutter:\n    sdk: flutter\n", StandardCharsets.UTF_8);
    }

    private static void addPackage(ArrayNode packages, String name, Path root, String language) {
        assertTrue(Files.isDirectory(root), "Pinned package missing: " + root);
        packages.addObject().put("name", name).put("rootUri", root.toUri().toString())
                .put("packageUri", "lib/").put("languageVersion", language);
    }
}
