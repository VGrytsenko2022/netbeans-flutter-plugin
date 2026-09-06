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
import dev.flutter.netbeans.plugin.designer.properties.FloatingActionButtonPropertyContractTest;
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

/** Actual SDK candidates, strict Object witnesses and reopen for all four FAB constructors. */
class FloatingActionButtonCandidateRealSdkTest {
    @TempDir Path workspace;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final StableId DOCUMENT_ID = StableId.parse("74000000-0000-4000-8000-000000000001");
    private static final WidgetTypeId FAB = new WidgetTypeId("flutter.material.FloatingActionButton");

    @Test
    void allFourDenseConstructorsRetainFullSourceAndStrictProjectReferenceEvidence() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (String variant : List.of("standard", "small", "large", "extended")) {
            var values = FloatingActionButtonPropertyContractTest.full(variant);
            values.put(p("enabled"), new PropertyValue.BooleanValue(true));
            cases.add(new Candidate(variant, button(variant, values, true), true));
        }
        validate(cases);
    }

    @Test
    void defaultNullLiteralHeroesAndAllThreeEmptyChildrenGenerateWithoutInventedTags() throws Exception {
        var children = new ArrayList<WidgetNode>();
        for (String variant : List.of("standard", "small", "large")) {
            children.add(button(variant, Map.of(), false));
        }
        var tags = List.<PropertyValue>of(new PropertyValue.NullValue(),
                new PropertyValue.StringValue("literal ' \\ tag"),
                new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(42)),
                new PropertyValue.IntegerValue(new java.math.BigInteger("9007199254740991")),
                new PropertyValue.IntegerValue(new java.math.BigInteger("-9007199254740991")),
                new PropertyValue.DoubleValue(new BigDecimal("42.5")),
                new PropertyValue.BooleanValue(true), new PropertyValue.BooleanValue(false));
        for (var tag : tags) children.add(button("extended", Map.of(p("heroTag"), tag), true));
        var root = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(children)));
        validate(List.of(new Candidate("literal-and-empty", root, true)));
    }

    @Test
    void typedObjectShapeCursorAndCallbackWitnessesAcceptConcreteButRejectDynamicAndNullableTags() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (String tag : List.of("_heroTag", "_dynamicHeroTag", "_nullableHeroTag")) {
            var values = Map.<PropertyName, PropertyValue>of(
                    p("heroTag"), reference(tag), p("shape"), reference("_shape"),
                    p("mouseCursor"), reference("_mouseCursor"), p("focusNode"), reference("_focusNode"),
                    p("onPressed"), reference("_onPressed"), p("enabled"), new PropertyValue.BooleanValue(true));
            cases.add(new Candidate(tag, button("extended", values, true), tag.equals("_heroTag")));
        }
        validate(cases);
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
                var typed = probes.stream().flatMap(probe -> probe.staticTypeProbe().stream()).toList();
                assertEquals(5, typed.size(), entry.name());
            }
            byte[] candidate = prepared.prospectiveDartBytes();
            String source = new String(candidate, StandardCharsets.UTF_8);
            assertTrue(source.contains("FloatingActionButton"), source);
            assertFalse(source.contains("FilledButton("), source);
            if (List.of("standard", "small", "large", "extended").contains(entry.name())) {
                assertTrue(source.contains("FloatingActionButton" + (entry.name().equals("standard") ? "" : "." + entry.name()) + "("), source);
            }
            if (entry.name().equals("literal-and-empty")) {
                assertTrue(source.contains("heroTag: null"), source);
                assertTrue(source.contains("heroTag: true"), source);
                assertTrue(source.contains("heroTag: false"), source);
                assertTrue(source.contains("heroTag: 42"), source);
                assertTrue(source.contains("child: null"), source);
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
                        && v.probe().staticTypeProbe().map(type -> type.expectedDartType().equals("Object")).orElse(false)),
                        () -> entry.name() + ": " + result);
                var otherTyped = result.symbolEvidence().stream().filter(v -> v.probe().staticTypeProbe().isPresent()
                        && !v.probe().expectedSymbolName().equals(entry.name())).toList();
                assertEquals(4, otherTyped.size());
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
    private static WidgetNode button(String variant, Map<PropertyName, PropertyValue> values, boolean child) {
        var properties = new LinkedHashMap<>(values);
        properties.put(p("variant"), new PropertyValue.StringValue(variant));
        properties.putIfAbsent(p("enabled"), new PropertyValue.BooleanValue(false));
        return new WidgetNode(StableId.random(), FAB, properties,
                Map.of(new SlotName("child"), child ? WidgetSlot.SingleSlot.of(text("Label")) : WidgetSlot.SingleSlot.empty(),
                        new SlotName("icon"), child && variant.equals("extended") ? WidgetSlot.SingleSlot.of(text("+")) : WidgetSlot.SingleSlot.empty()));
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
                + "void _onPressed() {}\nfinal _focusNode = FocusNode();\n"
                + "final _heroTag = const ValueKey<String>('fab');\n"
                + "dynamic _dynamicHeroTag = const ValueKey<String>('dynamic');\n"
                + "Object? _nullableHeroTag = const ValueKey<String>('nullable');\n"
                + "final _shape = const StadiumBorder();\nfinal _mouseCursor = SystemMouseCursors.click;\n"
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
