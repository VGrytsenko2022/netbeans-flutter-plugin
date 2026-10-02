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
import io.github.vgrytsenko2022.plugin.designer.properties.CheckboxPropertyContractTest;
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

/** Checkbox standard/adaptive generation and strict same-file SDK proof without widening safety budgets. */
class CheckboxCandidateRealSdkTest {
    @TempDir Path workspace;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final StableId DOCUMENT_ID = StableId.parse("76000000-0000-4000-8000-000000000001");
    private static final WidgetTypeId CHECKBOX = new WidgetTypeId("flutter.material.Checkbox");

    @Test
    void denseLocalFamiliesUseBothConstructorsAndEverySupportedShape() throws Exception {
        var cases = new ArrayList<Candidate>();
        int index = 0;
        for (String shape : CardWidgetPropertySchema.shapeKinds()) {
            String variant = index++ % 2 == 0 ? "standard" : "adaptive";
            var values = CheckboxPropertyContractTest.full(variant, shape, true);
            values.put(p("enabled"), new PropertyValue.BooleanValue(true));
            values.put(p("value"), new PropertyValue.NullValue());
            values.put(p("tristate"), new PropertyValue.BooleanValue(true));
            assertTrue(values.size() <= 512);
            cases.add(new Candidate("dense-" + variant + "-" + shape, checkbox(variant, values), ""));
        }
        validate(cases);
    }

    @Test
    void wholeStatePropertiesOutlinedShapesSidesAndFactoriesUseStrictPublicTypes() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (String variant : List.of("standard", "adaptive")) {
            for (boolean factory : List.of(false, true)) {
                var values = new LinkedHashMap<PropertyName, PropertyValue>();
                values.put(p("enabled"), new PropertyValue.BooleanValue(true));
                values.put(p("onChanged"), reference("_onChanged"));
                for (String name : List.of("focusNode", "mouseCursor", "shape", "side", "fillColor", "overlayColor")) {
                    values.put(p(name), factory ? factory("_make" + Character.toUpperCase(name.charAt(0)) + name.substring(1))
                            : reference("_" + name));
                }
                cases.add(new Candidate("whole-" + variant + "-" + factory, checkbox(variant, values), ""));
            }
        }
        validate(cases);
    }

    @Test
    void statePropertyReferenceWitnessesRejectDynamicNullableWrongGenericAndWrongCallback() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (String name : List.of("_fillColor", "_dynamicFillColor", "_nullableFillColor", "_wrongFillColor")) {
            cases.add(new Candidate(name, checkbox("standard", Map.of(p("fillColor"), reference(name))),
                    name.equals("_fillColor") ? "" : "WidgetStateProperty<Color?>"));
        }
        for (String name : List.of("_broadShape", "_nullableShape")) {
            cases.add(new Candidate(name, checkbox("adaptive", Map.of(p("shape"), reference(name))), "OutlinedBorder"));
        }
        cases.add(new Candidate("_nullableSide", checkbox("standard",
                Map.of(p("side"), reference("_nullableSide"))), "BorderSide"));
        for (String name : List.of("_wrongOnChanged", "_nullableOnChanged", "_dynamicOnChanged")) {
            cases.add(new Candidate(name, checkbox("standard", Map.of(p("onChanged"), reference(name),
                    p("enabled"), new PropertyValue.BooleanValue(true))), "ValueChanged<bool?>"));
        }
        validate(cases);
    }

    @Test
    void explicitNullResolversPlainStatefulSidesAndInactiveCallbacksStayDistinct() throws Exception {
        var widgets = new ArrayList<WidgetNode>();
        for (String variant : List.of("standard", "adaptive")) {
            var values = new LinkedHashMap<PropertyName, PropertyValue>();
            values.put(p("enabled"), new PropertyValue.BooleanValue(false));
            values.put(p("onChanged"), externalReference("package:not_installed/disabled.dart", "disabledChanged"));
            values.put(p("value"), new PropertyValue.NullValue());
            values.put(p("tristate"), new PropertyValue.BooleanValue(true));
            values.put(p("fillColorDefault"), new PropertyValue.ColorValue(0xff123456L));
            values.put(p("fillColorDisabled"), new PropertyValue.NullValue());
            values.put(p("overlayColorDefault"), new PropertyValue.ColorValue(0x44123456L));
            values.put(p("overlayColorPressed"), new PropertyValue.NullValue());
            values.put(p("sideColor"), new PropertyValue.ColorValue(0xff112233L));
            values.put(p("sideWidth"), new PropertyValue.DoubleValue(BigDecimal.ONE));
            values.put(p("splashRadius"), new PropertyValue.DoubleValue(BigDecimal.valueOf(-5)));
            widgets.add(checkbox(variant, values));
            values.put(p("sideStateful"), new PropertyValue.BooleanValue(true));
            values.put(p("sideDisabledMode"), new PropertyValue.StringValue("inherit"));
            values.put(p("sideSelectedWidth"), new PropertyValue.DoubleValue(BigDecimal.valueOf(2)));
            values.put(p("sideSelectedMode"), new PropertyValue.StringValue("border"));
            values.put(p("visualDensityVertical"), new PropertyValue.DoubleValue(BigDecimal.valueOf(-1)));
            values.put(p("splashRadius"), new PropertyValue.EnumValue("double", "infinity"));
            widgets.add(checkbox(variant, values));
        }
        var root = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(widgets)));
        validate(List.of(new Candidate("null-side-and-disabled", root, "")));
    }

    private record Candidate(String name, WidgetNode root, String rejectedType) { }

    private void validate(List<Candidate> candidates) throws Exception {
        Path executable = configured("dart.executable"), sdk = configured("flutter.sdk"), cache = configured("pub.cache");
        Path project = Files.createDirectories(workspace.resolve("checkbox_probe"));
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
            assertTrue(source.contains("Checkbox"), source);
            assertFalse(source.contains("_CheckboxDefaults"), "No private SDK implementation types");
            assertFalse(source.contains("IconButton("), source);
            if (entry.name().startsWith("dense-")) {
                String variant = entry.name().contains("-adaptive-") ? ".adaptive" : "";
                assertTrue(source.contains("Checkbox" + variant + "("), entry.name());
                assertTrue(probes.size() > 30, entry.name() + ": preserve all generated occurrences");
            }
            if (entry.name().equals("null-side-and-disabled")) {
                assertFalse(source.contains("package:not_installed/disabled.dart"), source);
                assertFalse(source.contains("disabledChanged"), source);
                assertTrue(source.contains("WidgetStateBorderSide"), source);
                assertTrue(source.contains("value: null"), source);
                assertTrue(source.contains("onChanged: null"), source);
                assertTrue(source.contains("(1.0 / 0.0)"), source);
            }
            var request = new DartCandidateAnalysisRequest(project, file, source, version++,
                    HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(candidate)),
                    DartCandidateWarningPolicy.ALLOW, probes, generation.generated().orElseThrow().candidateCapacityBudget());
            var result = analyzer.analyze(request).result().toCompletableFuture().get(60, TimeUnit.SECONDS);
            if (entry.rejectedType().isEmpty()) {
                assertEquals(DartCandidateAnalysisStatus.PASSED, result.status(), () -> entry.name() + ": " + result.diagnostics()
                        + " issue=" + result.issue() + " rejected=" + result.symbolEvidence().stream().filter(v -> !v.accepted()).limit(5).toList());
                assertEquals(probes.size(), result.symbolEvidence().size());
                assertTrue(result.symbolEvidence().stream().allMatch(DartSymbolEvidence::accepted));
            } else {
                assertNotEquals(DartCandidateAnalysisStatus.PASSED, result.status(), entry.name());
                if (Set.of("_wrongFillColor", "_broadShape", "_wrongOnChanged").contains(entry.name())) {
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
    private static WidgetNode checkbox(String variant, Map<PropertyName, PropertyValue> values) {
        var properties = new LinkedHashMap<>(values);
        properties.put(p("variant"), new PropertyValue.StringValue(variant));
        properties.putIfAbsent(p("enabled"), new PropertyValue.BooleanValue(false));
        properties.putIfAbsent(p("value"), new PropertyValue.BooleanValue(false));
        return new WidgetNode(StableId.random(), CHECKBOX, properties, Map.of());
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
                + "void _onChanged(bool? value) {}\nvoid _wrongOnChanged(bool value) {}\n"
                + "ValueChanged<bool?>? _nullableOnChanged = _onChanged;\ndynamic _dynamicOnChanged = _onChanged;\n"
                + "final _focusNode = FocusNode();\nFocusNode _makeFocusNode() => FocusNode();\n"
                + "final _mouseCursor = WidgetStateMouseCursor.clickable;\nMouseCursor _makeMouseCursor() => _mouseCursor;\n"
                + "const _shape = RoundedRectangleBorder();\nOutlinedBorder _makeShape() => _shape;\n"
                + "ShapeBorder _broadShape = const RoundedRectangleBorder();\nOutlinedBorder? _nullableShape = _shape;\n"
                + "final _side = WidgetStateBorderSide.resolveWith((states) => states.contains(WidgetState.selected) ? const BorderSide(color: Color(0xff123456), width: 2) : null);\n"
                + "BorderSide _makeSide() => _side;\nBorderSide? _nullableSide = const BorderSide();\n"
                + "const _fillColor = WidgetStatePropertyAll<Color?>(Color(0xff123456));\n"
                + "const _overlayColor = WidgetStatePropertyAll<Color?>(Color(0x44123456));\n"
                + "WidgetStateProperty<Color?> _makeFillColor() => _fillColor;\n"
                + "WidgetStateProperty<Color?> _makeOverlayColor() => _overlayColor;\n"
                + "dynamic _dynamicFillColor = _fillColor;\nWidgetStateProperty<Color?>? _nullableFillColor = _fillColor;\n"
                + "const _wrongFillColor = WidgetStatePropertyAll<TextStyle?>(TextStyle());\n"
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
        addPackage(packages, "checkbox_probe", project, "3.10");
        addPackage(packages, "flutter", sdk.resolve("packages/flutter"), "3.10");
        addPackage(packages, "sky_engine", sdk.resolve("bin/cache/pkg/sky_engine"), "3.10");
        for (var dependency : Map.of("characters", "1.4.1", "collection", "1.19.1",
                "material_color_utilities", "0.13.0", "meta", "1.18.0", "vector_math", "2.2.0").entrySet()) {
            addPackage(packages, dependency.getKey(), cache.resolve("hosted/pub.dev")
                    .resolve(dependency.getKey() + "-" + dependency.getValue()), "3.4");
        }
        Path tool = Files.createDirectories(project.resolve(".dart_tool"));
        Files.writeString(tool.resolve("package_config.json"), JSON.writeValueAsString(config), StandardCharsets.UTF_8);
        Files.writeString(project.resolve("pubspec.yaml"), "name: checkbox_probe\nenvironment:\n  sdk: ^3.10.0\n"
                + "dependencies:\n  flutter:\n    sdk: flutter\n", StandardCharsets.UTF_8);
    }

    private static void addPackage(ArrayNode packages, String name, Path root, String language) {
        assertTrue(Files.isDirectory(root), "Pinned package missing: " + root);
        packages.addObject().put("name", name).put("rootUri", root.toUri().toString())
                .put("packageUri", "lib/").put("languageVersion", language);
    }
}
