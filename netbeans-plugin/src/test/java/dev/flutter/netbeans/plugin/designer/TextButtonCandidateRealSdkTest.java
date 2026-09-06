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
import dev.flutter.netbeans.plugin.designer.properties.TextButtonPropertyContractTest;
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

/** Actual generation, production probe planner and strict SDK proof for dense styles. */
class TextButtonCandidateRealSdkTest {
    @TempDir Path workspace;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final StableId DOCUMENT_ID = StableId.parse("73000000-0000-4000-8000-000000000001");

    @Test
    void validatesDenseStandardAndIconCandidatesWithoutRelaxingThe45SecondAnalyzerBudget() throws Exception {
        Path executable = configured("dart.executable");
        Path sdk = configured("flutter.sdk");
        Path cache = configured("pub.cache");
        Path project = Files.createDirectories(workspace.resolve("button_probe"));
        Path lib = Files.createDirectories(project.resolve("lib"));
        configure(project, sdk, cache);
        Path file = lib.resolve("home_page.dart");
        var codec = new FdDocumentCodec();
        var generator = new DartRegionGenerator();
        var catalog = BuiltInWidgetCatalog.getDefault();
        var label = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue("Label")), Map.of());
        var seed = descriptor("", "");
        var baselineGenerated = generator.generate(document(seed, label), catalog).generated().orElseThrow();
        var baselineDescriptor = descriptor(baselineGenerated.imports().payload(), baselineGenerated.build().payload());
        var baselineDocument = document(baselineDescriptor, label);
        byte[] baselineBytes = source(baselineGenerated);
        Files.write(file, baselineBytes);
        byte[] pubspec = Files.readAllBytes(project.resolve("pubspec.yaml"));
        byte[] packageConfig = Files.readAllBytes(project.resolve(".dart_tool/package_config.json"));
        var scanner = new DartSourceIntegrityScanner();
        var baseline = new DartThreeWayIntegrityGate(scanner).evaluate(
                scanner.scan(baselineBytes, baselineDescriptor), baselineDescriptor,
                generator.generate(baselineDocument, catalog));
        var decoded = (FdDecodeResult.Current) codec.decode(codec.encode(baselineDocument));
        var analyzer = new DartCandidateAnalyzer(executable, ignored -> { });
        assertEquals(45, DartCandidateAnalysisLimits.DEFAULT.totalTimeout().toSeconds());
        long version = 1;
        for (boolean icon : List.of(false, true)) {
            var properties = TextButtonPropertyContractTest.full(icon, icon, icon);
            properties.put(new PropertyName("enabled"), new PropertyValue.BooleanValue(true));
            // Populate all nine independent lists, retaining repeated stable item IDs
            // across different property paths to exercise scoped occurrence identities.
            var itemId = StableId.parse("73000000-0000-4000-8000-000000000002");
            properties.replaceAll((name, value) -> switch (value) {
                case PropertyValue.ShadowListValue ignored -> new PropertyValue.ShadowListValue(List.of(
                        new PropertyValue.ShadowListValue.Shadow(itemId, new ColorSource.Literal(0xff123456L),
                                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE)));
                case PropertyValue.FontFeatureListValue ignored -> new PropertyValue.FontFeatureListValue(List.of(
                        new PropertyValue.FontFeatureListValue.FontFeature(itemId, "kern", 1)));
                case PropertyValue.FontVariationListValue ignored -> new PropertyValue.FontVariationListValue(List.of(
                        new PropertyValue.FontVariationListValue.FontVariation(itemId, "wght", BigDecimal.valueOf(500))));
                default -> value;
            });
            assertEquals(icon ? 464 : 491, properties.size());
            var slots = new LinkedHashMap<SlotName, WidgetSlot>();
            slots.put(new SlotName("child"), WidgetSlot.SingleSlot.of(label));
            slots.put(new SlotName("icon"), icon ? WidgetSlot.SingleSlot.of(new WidgetNode(
                    StableId.random(), label.type(), label.properties(), Map.of())) : WidgetSlot.SingleSlot.empty());
            var button = new WidgetNode(StableId.random(), TextButtonWidgetPropertySchema.TEXT_BUTTON_TYPE, properties, slots);
            var generation = generator.generate(document(baselineDescriptor, button), catalog);
            assertTrue(generation.generated().isPresent(), () -> generation.toString());
            var transition = new DartSourceTransitionPlanner(scanner).plan(
                    baseline, baselineBytes, baselineDescriptor, generation).plan().orElseThrow();
            var prepared = new DesignerPairPreparationPlanner().prepare(decoded.original(),
                    document(transition.prospectiveDescriptor(), button), transition).preparedPair().orElseThrow();
            var probes = GeneratedDartSymbolProbePlanner.plan(prepared, sdk, project);
            // The icon fixture uses directional branches; the standard fixture
            // includes the larger physical-border/text-style constructor family.
            assertTrue(probes.size() > (icon ? 750 : 1000),
                    "Dense styles must retain every generated occurrence: icon=" + icon + ", probes=" + probes.size());
            assertTrue(probes.size() <= 2048);
            assertEquals(probes.size(), probes.stream().map(DartSymbolProbe::id).distinct().count());
            var typed = probes.stream().flatMap(value -> value.staticTypeProbe().stream()).toList();
            assertEquals(8, typed.size());
            assertTrue(typed.stream().allMatch(value -> value.expectedTypeLibraryUri().equals("package:flutter/material.dart")));
            byte[] candidate = prepared.prospectiveDartBytes();
            var request = new DartCandidateAnalysisRequest(project, file, new String(candidate, StandardCharsets.UTF_8),
                    version++, HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(candidate)),
                    DartCandidateWarningPolicy.ALLOW, probes, generation.generated().orElseThrow().candidateCapacityBudget());
            var result = analyzer.analyze(request).result().toCompletableFuture().get(60, TimeUnit.SECONDS);
            assertEquals(DartCandidateAnalysisStatus.PASSED, result.status(), () -> "icon=" + icon
                    + " diagnostics=" + result.diagnostics() + " issue=" + result.issue()
                    + " rejected=" + result.symbolEvidence().stream().filter(value -> !value.accepted())
                            .limit(8).map(value -> value.probe().id() + ": " + value.rejectionReason()).toList());
            assertEquals(probes.size(), result.symbolEvidence().size());
            assertTrue(result.symbolEvidence().stream().allMatch(DartSymbolEvidence::accepted));
            var reopened = (FdDecodeResult.Current) codec.decode(prepared.prospectiveFdBytes());
            assertEquals(button.properties(), reopened.document().root().properties());
            assertArrayEquals(baselineBytes, Files.readAllBytes(file));
            assertArrayEquals(pubspec, Files.readAllBytes(project.resolve("pubspec.yaml")));
            assertArrayEquals(packageConfig, Files.readAllBytes(project.resolve(".dart_tool/package_config.json")));
            assertFalse(Files.exists(project.resolve("analysis_options.yaml")));
            assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
        }
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
                + "void _onPressed() {}\nvoid _onLongPress() {}\n"
                + "void _onHover(bool value) {}\nvoid _onFocusChange(bool value) {}\n"
                + "final _focusNode = FocusNode();\nfinal _statesController = WidgetStatesController();\n"
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
