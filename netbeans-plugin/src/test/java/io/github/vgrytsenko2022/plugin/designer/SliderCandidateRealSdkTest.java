package io.github.vgrytsenko2022.plugin.designer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.github.vgrytsenko2022.dart.*;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.codec.*;
import io.github.vgrytsenko2022.designer.generation.*;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.pair.*;
import io.github.vgrytsenko2022.designer.source.*;
import io.github.vgrytsenko2022.designer.transition.*;
import io.github.vgrytsenko2022.plugin.designer.properties.SliderPropertyContractTest;
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

/** Full Slider constructor domains and strict same-file public SDK proof, without widening safety budgets. */
class SliderCandidateRealSdkTest {
    @TempDir Path workspace;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final StableId DOCUMENT_ID = StableId.parse("77000000-0000-4000-8000-000000000001");
    private static final WidgetTypeId SLIDER = new WidgetTypeId("flutter.material.Slider");


    private static final List<String> STATES = List.of("Default", "Disabled", "Error", "Dragged", "Pressed", "Selected", "ScrolledUnder", "Hovered", "Focused");
    private static final List<String> REFERENCES = List.of("onChanged", "onChangeStart", "onChangeEnd", "semanticFormatterCallback", "focusNode", "mouseCursor", "overlayColor");

    @Test
    void bothDenseConstructorsPreserveAllPropertiesAndLocalOverlayStates() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (String variant : List.of("standard", "adaptive")) {
            var values = SliderPropertyContractTest.full(variant);
            values.put(p("enabled"), new PropertyValue.BooleanValue(true));
            cases.add(new Candidate("dense-" + variant, sliderNode(variant, values), ""));
        }
        validate(cases);
    }

    @Test
    void typedCallbacksStatePropertiesFocusAndCursorReferencesAndFactoriesAnalyze() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (String variant : List.of("standard", "adaptive")) for (boolean invoke : List.of(false, true)) {
            var values = new LinkedHashMap<PropertyName, PropertyValue>();
            values.put(p("enabled"), new PropertyValue.BooleanValue(true));
            for (String name : REFERENCES) {
                values.put(p(name), invoke ? factory("_make" + Character.toUpperCase(name.charAt(0)) + name.substring(1)) : reference("_" + name));
            }
            cases.add(new Candidate("whole-" + variant + "-" + invoke, sliderNode(variant, values), ""));
        }
        validate(cases);
    }

    @Test
    void callbackAndStateWitnessesRejectWrongGenericsDynamicAndNullableOuterTypes() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (String family : List.of("onChanged", "onChangeStart", "onChangeEnd", "semanticFormatterCallback", "overlayColor")) {
            String suffix = Character.toUpperCase(family.charAt(0)) + family.substring(1);
            String type = family.equals("overlayColor") ? "WidgetStateProperty<Color?>"
                    : family.equals("semanticFormatterCallback") ? "SemanticFormatterCallback" : "ValueChanged<double>";
            for (String prefix : List.of("", "dynamic", "nullable", "wrong")) {
                String name = prefix.isEmpty() ? "_" + family : "_" + prefix + suffix;
                cases.add(new Candidate(name, sliderNode("adaptive", Map.of(
                        p("enabled"), new PropertyValue.BooleanValue(true), p(family), reference(name))), prefix.isEmpty() ? "" : type));
            }
        }
        validate(cases);
    }

    @Test
    void nullableFieldsEmptyLabelInactiveOnChangedAndActiveOtherCallbacksRemainDistinct() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (String variant : List.of("standard", "adaptive")) {
            var values = new LinkedHashMap<PropertyName, PropertyValue>();
            values.put(p("enabled"), new PropertyValue.BooleanValue(false));
            values.put(p("onChanged"), externalReference("package:not_installed/disabled.dart", "disabledChanged"));
            values.put(p("onChangeStart"), reference("_onChangeStart"));
            values.put(p("onChangeEnd"), factory("_makeOnChangeEnd"));
            values.put(p("semanticFormatterCallback"), reference("_semanticFormatterCallback"));
            for (String name : List.of("divisions", "secondaryTrackValue", "year2023"))
                values.put(p(name), new PropertyValue.NullValue());
            for (String state : STATES) values.put(p("overlayColor" + state), new PropertyValue.NullValue());
            values.put(p("label"), new PropertyValue.StringValue(""));
            cases.add(new Candidate("null-and-disabled-" + variant, sliderNode(variant, values), ""));
        }
        validate(cases);
    }

    @Test
    void exactSignedAndInfiniteRangesEnumsAndPortableDivisionLimitAnalyze() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (String variant : List.of("standard", "adaptive")) {
            for (String member : List.of("infinity", "negativeInfinity")) {
                var values = new LinkedHashMap<PropertyName, PropertyValue>();
                for (String name : List.of("value", "min", "max", "secondaryTrackValue"))
                    values.put(p(name), new PropertyValue.EnumValue("double", member));
                values.put(p("divisions"), new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(9007199254740991L)));
                cases.add(new Candidate("infinite-" + member + "-" + variant, sliderNode(variant, values), ""));
            }
            for (String interaction : List.of("tapAndSlide", "tapOnly", "slideOnly", "slideThumb")) {
                var values = new LinkedHashMap<PropertyName, PropertyValue>();
                values.put(p("min"), new PropertyValue.DoubleValue(new BigDecimal("-1e308")));
                values.put(p("max"), new PropertyValue.DoubleValue(new BigDecimal("1e308")));
                values.put(p("value"), new PropertyValue.DoubleValue(new BigDecimal("1e308")));
                values.put(p("allowedInteraction"), new PropertyValue.EnumValue("SliderInteraction", interaction));
                values.put(p("showValueIndicator"), new PropertyValue.EnumValue("ShowValueIndicator", "alwaysVisible"));
                cases.add(new Candidate("extreme-" + interaction + "-" + variant, sliderNode(variant, values), ""));
            }
            for (String indicator : List.of("onlyForDiscrete", "onlyForContinuous", "always", "never", "onDrag", "alwaysVisible")) {
                cases.add(new Candidate("indicator-" + indicator + "-" + variant, sliderNode(variant,
                        Map.of(p("showValueIndicator"), new PropertyValue.EnumValue("ShowValueIndicator", indicator))), ""));
            }
        }
        validate(cases);
    }

    private record Candidate(String name, WidgetNode root, String rejectedType) { }

    private void validate(List<Candidate> candidates) throws Exception {
        Path executable = configured("dart.executable"), sdk = configured("flutter.sdk"), cache = configured("pub.cache");
        Path project = Files.createDirectories(workspace.resolve("slider_probe"));
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
            var typedProbes = probes.stream().flatMap(probe -> probe.staticTypeProbe().stream()).toList();
            boolean materialFormatter = typedProbes.stream().anyMatch(probe -> probe.expectedDartType().equals("SemanticFormatterCallback"));
            assertTrue(typedProbes.stream().allMatch(probe -> probe.expectedTypeLibraryUri().equals(materialFormatter
                    ? "package:flutter/material.dart" : "package:flutter/widgets.dart")),
                    "A Material-only formatter selects one Material umbrella for every strict witness, irrespective of occurrence order");
            if (entry.name().startsWith("_")) {
                assertEquals(1, probes.stream().flatMap(probe -> probe.staticTypeProbe().stream()).count(), entry.name());
            }
            byte[] candidate = prepared.prospectiveDartBytes();
            String source = new String(candidate, StandardCharsets.UTF_8);
            assertTrue(source.contains("Slider"), source);
            assertFalse(source.contains("_SliderDefaults"), "No private SDK implementation types");
            assertFalse(source.contains("IconButton("), source);
            if (entry.name().startsWith("dense-")) {
                String variant = entry.name().endsWith("-adaptive") ? ".adaptive" : "";
                assertTrue(source.contains("Slider" + variant + "("), entry.name());
                assertTrue(probes.size() > 20, entry.name() + ": preserve generated occurrences");
                if (variant.equals(".adaptive")) assertFalse(source.contains("padding:"), source);
            }
            if (entry.name().startsWith("null-and-disabled-")) {
                assertFalse(source.contains("package:not_installed/disabled.dart"), source);
                assertFalse(source.contains("disabledChanged"), source);
                assertTrue(source.contains("onChanged: null"), source);
                assertTrue(source.contains("onChangeStart: _onChangeStart"), source);
                assertTrue(source.contains("onChangeEnd: _makeOnChangeEnd()"), source);
                for (String name : List.of("divisions", "secondaryTrackValue", "year2023"))
                    assertTrue(source.contains(name + ": null"), source);
            }
            if (entry.name().startsWith("infinite-")) {
                assertTrue(source.contains(entry.name().contains("negativeInfinity") ? "(-1.0 / 0.0)" : "(1.0 / 0.0)"), source);
            }
            var request = new DartCandidateAnalysisRequest(project, file, source, version++,
                    HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(candidate)),
                    DartCandidateWarningPolicy.ALLOW, probes, generation.generated().orElseThrow().candidateCapacityBudget());
            var result = analyzer.analyze(request).result().toCompletableFuture().get(60, TimeUnit.SECONDS);
            if (entry.rejectedType().isEmpty()) {
                assertEquals(DartCandidateAnalysisStatus.PASSED, result.status(), () -> entry.name() + ": " + result.diagnostics().stream().filter(DartCandidateDiagnostic::blocking).toList()
                        + " issue=" + result.issue() + " rejected=" + result.symbolEvidence().stream().filter(v -> !v.accepted()).limit(5).toList());
                assertEquals(probes.size(), result.symbolEvidence().size());
                assertTrue(result.symbolEvidence().stream().allMatch(DartSymbolEvidence::accepted));
            } else {
                assertNotEquals(DartCandidateAnalysisStatus.PASSED, result.status(), entry.name());
                if (entry.name().startsWith("_wrong")) {
                    // Incompatible arguments fail compilation before the analyzer requests symbol evidence.
                    assertEquals(DartCandidateAnalysisStatus.REJECTED, result.status(), entry.name());
                    assertTrue(result.diagnostics().stream().anyMatch(d -> d.blocking()
                            && d.code().filter("argument_type_not_assignable"::equals).isPresent()
                            && d.offset() >= 0 && d.offset() + d.length() <= source.length()
                            && source.substring(d.offset(), d.offset() + d.length()).equals(entry.name())),
                            () -> entry.name() + ": " + result.diagnostics());
                    assertTrue(result.symbolEvidence().isEmpty(), entry.name());
                } else {
                    assertTrue(result.symbolEvidence().stream().anyMatch(v -> !v.accepted()
                            && v.probe().expectedSymbolName().equals(entry.name())
                            && v.probe().staticTypeProbe().map(type -> type.expectedDartType().equals(entry.rejectedType())).orElse(false)),
                            () -> entry.name() + ": " + result);
                }
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
    private static WidgetNode sliderNode(String variant, Map<PropertyName, PropertyValue> values) {
        var properties = new LinkedHashMap<>(values);
        properties.put(p("variant"), new PropertyValue.StringValue(variant));
        properties.putIfAbsent(p("enabled"), new PropertyValue.BooleanValue(false));
        properties.putIfAbsent(p("value"), new PropertyValue.IntegerValue(java.math.BigInteger.ZERO));
        return new WidgetNode(StableId.random(), SLIDER, properties, Map.of());
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
        var declarations = new StringBuilder("""
                final _focusNode = FocusNode();
                FocusNode _makeFocusNode() => FocusNode();
                final _mouseCursor = WidgetStateMouseCursor.clickable;
                MouseCursor _makeMouseCursor() => _mouseCursor;
                const _overlayColor = WidgetStatePropertyAll<Color?>(Color(0xff123456));
                WidgetStateProperty<Color?> _makeOverlayColor() => _overlayColor;
                dynamic _dynamicOverlayColor = _overlayColor;
                WidgetStateProperty<Color?>? _nullableOverlayColor = _overlayColor;
                const _wrongOverlayColor = WidgetStatePropertyAll<TextStyle?>(TextStyle());
                String _semanticFormatterCallback(double value) => 'Value $value';
                SemanticFormatterCallback _makeSemanticFormatterCallback() => _semanticFormatterCallback;
                dynamic _dynamicSemanticFormatterCallback = _semanticFormatterCallback;
                SemanticFormatterCallback? _nullableSemanticFormatterCallback = _semanticFormatterCallback;
                int _wrongSemanticFormatterCallback(double value) => 0;
                """);
        for (String family : List.of("onChanged", "onChangeStart", "onChangeEnd")) {
            String suffix = Character.toUpperCase(family.charAt(0)) + family.substring(1);
            declarations.append("void _").append(family).append("(double value) {}\n")
                    .append("ValueChanged<double> _make").append(suffix).append("() => _").append(family).append(";\n")
                    .append("dynamic _dynamic").append(suffix).append(" = _").append(family).append(";\n")
                    .append("ValueChanged<double>? _nullable").append(suffix).append(" = _").append(family).append(";\n")
                    .append("void _wrong").append(suffix).append("(int value) {}\n");
        }
        return ("// <netbeans-flutter-designer region=\"imports\">\n" + generated.imports().payload()
                + "// </netbeans-flutter-designer>\n" + declarations
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
        addPackage(packages, "slider_probe", project, "3.10");
        addPackage(packages, "flutter", sdk.resolve("packages/flutter"), "3.10");
        addPackage(packages, "sky_engine", sdk.resolve("bin/cache/pkg/sky_engine"), "3.10");
        for (var dependency : Map.of("characters", "1.4.1", "collection", "1.19.1",
                "material_color_utilities", "0.13.0", "meta", "1.18.0", "vector_math", "2.2.0").entrySet()) {
            addPackage(packages, dependency.getKey(), cache.resolve("hosted/pub.dev")
                    .resolve(dependency.getKey() + "-" + dependency.getValue()), "3.4");
        }
        Path tool = Files.createDirectories(project.resolve(".dart_tool"));
        Files.writeString(tool.resolve("package_config.json"), JSON.writeValueAsString(config), StandardCharsets.UTF_8);
        Files.writeString(project.resolve("pubspec.yaml"), "name: slider_probe\nenvironment:\n  sdk: ^3.10.0\n"
                + "dependencies:\n  flutter:\n    sdk: flutter\nflutter:\n  uses-material-design: true\n", StandardCharsets.UTF_8);
    }

    private static void addPackage(ArrayNode packages, String name, Path root, String language) {
        assertTrue(Files.isDirectory(root), "Pinned package missing: " + root);
        packages.addObject().put("name", name).put("rootUri", root.toUri().toString())
                .put("packageUri", "lib/").put("languageVersion", language);
    }
}
