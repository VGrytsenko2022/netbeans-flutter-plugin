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
import io.github.vgrytsenko2022.plugin.designer.properties.TooltipThemePropertyContractTest;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Exact TooltipThemeData constructor and compound proofs and immutable pair-save candidates. */
class TooltipThemeCandidateRealSdkTest {
    @TempDir Path workspace;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final StableId DOCUMENT_ID = StableId.parse("80000000-0000-4000-8000-000000000001");
    private static final WidgetTypeId TOOLTIP = new WidgetTypeId("flutter.material.TooltipTheme");

    private static final Map<String, String> REFERENCE_TYPES = Map.ofEntries(
            Map.entry("data", "TooltipThemeData"), Map.entry("constraints", "BoxConstraints"),
            Map.entry("padding", "EdgeInsetsGeometry"), Map.entry("margin", "EdgeInsetsGeometry"),
            Map.entry("decoration", "Decoration"), Map.entry("textStyle", "TextStyle"),
            Map.entry("waitDurationUs", "Duration"), Map.entry("showDurationUs", "Duration"),
            Map.entry("exitDurationUs", "Duration"));

    @Test
    void completeLocalCompoundFieldsContentBranchesNullsAndOmissionHaveExactSdkProofs() throws Exception {
        var cases = new ArrayList<Candidate>();
        cases.add(valid("all-local-fields", new LinkedHashMap<>(TooltipThemePropertyContractTest.full())));
        var paints = new LinkedHashMap<>(TooltipThemePropertyContractTest.full());
        paints.remove(p("textStyleColor"));
        paints.remove(p("textStyleBackgroundColor"));
        paints.put(p("textStyleForeground"), TooltipThemePropertyContractTest.value("textStyleForeground"));
        paints.put(p("textStyleBackground"), TooltipThemePropertyContractTest.value("textStyleBackground"));
        cases.add(valid("complementary-local-text-paints", paints));
        cases.add(valid("defaults", base()));
        var nulls = base();
        for (String name : REFERENCE_TYPES.keySet()) if (!name.equals("data")) nulls.put(p(name), nil());
        nulls.put(p("height"), nil());
        cases.add(valid("null-optionals", nulls));
        cases.add(valid("legacy-height", Map.of(p("height"), new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(32)))));
        cases.add(valid("shape-decoration", Map.of(p("decoration"), reference("_shapeDecoration"))));
        cases.add(valid("imported-whole-theme", Map.of(p("data"), external("remoteTheme"))));
        cases.add(valid("whole-theme", Map.of(p("data"), reference("_tooltipThemeData"))));
        cases.add(valid("unicode-font-family", Map.of(p("textStyleFontFamily"), string("Шрифт 'quoted'"))));
        validate(cases);
    }

    @Test
    void everyDirectReferenceFamilySupportsGettersFactoriesAndMembers() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (int access = 0; access < 3; access++) {
            var values = base();
            for (String name : new TreeSet<>(REFERENCE_TYPES.keySet())) {
                if (name.equals("data")) continue;
                values.put(p(name), access == 0 ? reference("_" + name)
                        : access == 1 ? factory("_make" + upper(name)) : member("References", name));
            }
            cases.add(valid("strict-local-families-" + access, values));
            cases.add(valid("strict-whole-data-" + access, Map.of(p("data"), access == 0 ? reference("_data")
                    : access == 1 ? factory("_makeData") : member("References", "data"))));
        }
        validate(cases);
    }

    @Test
    void wrongDynamicAndNullableValuesCannotAcquireSaveAuthorityForAnyReferenceFamily() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (String family : new TreeSet<>(REFERENCE_TYPES.keySet())) {
            for (String kind : List.of("dynamic", "nullable", "wrong")) {
                var values = base();
                String symbol = "_" + kind + upper(family);
                values.put(p(family), reference(symbol));
                cases.add((kind.equals("wrong") || kind.equals("nullable") && family.equals("data")) ? compileFailure(kind + "-" + family, values, symbol)
                        : proofFailure(kind + "-" + family, values, symbol));
            }
        }
        assertEquals(27, cases.size());
        validate(cases);
    }

    @Test
    void allModesNullableFlagsChildLayoutsAndExactSignedDurationsCompileTogether() throws Exception {
        var nodes = new ArrayList<WidgetNode>();
        for (String mode : List.of("manual", "longPress", "tap")) {
            for (int mask = 1; mask < 2; mask++) {
                var values = base(); values.put(p("triggerMode"), new PropertyValue.EnumValue("TooltipTriggerMode", mode));
                nodes.add(tile(values, mask));
            }
        }
        for (String name : List.of("waitDurationUs", "showDurationUs", "exitDurationUs")) {
            for (PropertyValue value : List.of(integer(-1), integer(0), integer(1),
                    integer(9_007_199_254_740_991L), integer(-9_007_199_254_740_991L), nil())) {
                var values = base(); values.put(p(name), value); nodes.add(tile(values, 1));
            }
        }
        for (String name : List.of("height", "verticalOffset")) {
            for (String member : List.of("infinity", "negativeInfinity", "nan")) {
                var values = base(); values.put(p(name), new PropertyValue.EnumValue("double", member)); nodes.add(tile(values, 1));
            }
        }
        for (String name : List.of("preferBelow", "excludeFromSemantics", "enableFeedback")) {
            for (boolean flag : List.of(false, true)) {
                var values = base(); values.put(p(name), bool(flag)); nodes.add(tile(values, 1));
            }
            {
                var values = base(); values.put(p(name), nil()); nodes.add(tile(values, 1));
            }
        }
        for (String alignment : List.of("left", "right", "center", "justify", "start", "end")) {
            var values = base(); values.put(p("textAlign"), new PropertyValue.EnumValue("TextAlign", alignment)); nodes.add(tile(values, 1));
        }
        assertEquals(42, nodes.size());
        var root = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(nodes)));
        validate(List.of(new Candidate("42-theme-sdk-matrix", base(), "", false, 0, root)));
    }

    private static WidgetNode tile(Map<PropertyName, PropertyValue> values, int mask) {
        return new WidgetNode(StableId.random(), TOOLTIP, values, slots(mask));
    }

    private record Candidate(String name, Map<PropertyName, PropertyValue> values, String badSymbol, boolean compileFailure, int slotMask, WidgetNode suppliedRoot) {
        Candidate(String name, Map<PropertyName, PropertyValue> values, String badSymbol, boolean compileFailure) {
            this(name, values, badSymbol, compileFailure, 1, null);
        }
    }
    private static Candidate valid(String name, Map<PropertyName, PropertyValue> values) {
        return new Candidate(name, values, "", false);
    }
    private static Candidate proofFailure(String name, Map<PropertyName, PropertyValue> values, String badSymbol) {
        return new Candidate(name, values, badSymbol, false);
    }
    private static Candidate compileFailure(String name, Map<PropertyName, PropertyValue> values, String badSymbol) {
        return new Candidate(name, values, badSymbol, true);
    }

    private void validate(List<Candidate> candidates) throws Exception {
        Path executable = configured("dart.executable"), sdk = configured("flutter.sdk"), cache = configured("pub.cache");
        Path project = Files.createDirectories(workspace.resolve("tooltip_theme_probe"));
        Path lib = Files.createDirectories(project.resolve("lib"));
        configure(project, sdk, cache);
        Files.writeString(lib.resolve("types.dart"), """
                import 'package:flutter/material.dart';
                const TooltipThemeData remoteTheme = TooltipThemeData(exitDuration: Duration(microseconds: 321), preferBelow: false);
                """, StandardCharsets.UTF_8);
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
        byte[] remote = Files.readAllBytes(lib.resolve("types.dart"));
        var scanner = new DartSourceIntegrityScanner();
        var baseline = new DartThreeWayIntegrityGate(scanner).evaluate(
                scanner.scan(baselineBytes, baselineDescriptor), baselineDescriptor, generator.generate(baselineDocument, catalog));
        var decoded = (FdDecodeResult.Current) codec.decode(codec.encode(baselineDocument));
        var current = new FlutterDesignerDocumentState.Current(decoded,
                new io.github.vgrytsenko2022.designer.validation.ValidationResult(List.of()), catalog,
                List.of(), List.of(), Optional.of(baseline.source()), Optional.of(baseline));
        var analyzer = new DartCandidateAnalyzer(executable, ignored -> { });
        assertEquals(45, DartCandidateAnalysisLimits.DEFAULT.totalTimeout().toSeconds());
        for (var entry : candidates) {
            var node = entry.suppliedRoot() == null ? new WidgetNode(StableId.random(), TOOLTIP, entry.values(), slots(entry.slotMask())) : entry.suppliedRoot();
            var generation = generator.generate(document(baselineDescriptor, node), catalog);
            assertTrue(generation.generated().isPresent(), () -> entry.name() + ": " + generation);
            var transition = new DartSourceTransitionPlanner(scanner).plan(
                    baseline, baselineBytes, baselineDescriptor, generation).plan().orElseThrow();
            var prepared = new DesignerPairPreparationPlanner().prepare(decoded.original(),
                    document(transition.prospectiveDescriptor(), node), transition).preparedPair().orElseThrow();
            var probes = GeneratedDartSymbolProbePlanner.plan(prepared, sdk, project);
            assertFalse(probes.isEmpty());
            assertTrue(probes.size() <= 2048);
            assertEquals(probes.size(), probes.stream().map(DartSymbolProbe::id).distinct().count());
            byte[] candidate = prepared.prospectiveDartBytes();
            String source = new String(candidate, StandardCharsets.UTF_8);
            assertTrue(source.contains("TooltipTheme("), source);
            assertFalse(source.contains("_TooltipState"), "No private SDK implementation types");
            if (entry.name().equals("defaults")) {
                String build = generation.generated().orElseThrow().build().payload();
                assertTrue(build.contains("TooltipThemeData()"), build);
                assertFalse(build.contains("copyWith("), build);
                assertFalse(build.contains("triggerMode:"), build);
            }
            var ticket = PairSaveEvidenceGate.prepareAnalysis(current, prepared, project, file,
                    DartCandidateWarningPolicy.ALLOW, sdk);
            var request = ticket.request();
            assertEquals(probes, request.symbolProbes(), "Pair-save must bind the same exact generated manifest");
            var result = analyzer.analyze(request).result().toCompletableFuture().get(60, TimeUnit.SECONDS);
            if (entry.badSymbol().isEmpty()) {
                assertEquals(DartCandidateAnalysisStatus.PASSED, result.status(), () -> entry.name() + ": "
                        + result.diagnostics().stream().filter(DartCandidateDiagnostic::blocking).toList()
                        + " issue=" + result.issue() + " rejected="
                        + result.symbolEvidence().stream().filter(v -> !v.accepted()).limit(5).toList());
                assertEquals(probes.size(), result.symbolEvidence().size());
                assertTrue(result.symbolEvidence().stream().allMatch(DartSymbolEvidence::accepted));
                assertInstanceOf(PairAnalyzedCandidateResult.Ready.class, ticket.accept(result),
                        () -> entry.name() + ": " + PairSaveEvidenceGate.evaluateAnalysis(ticket, result));
            } else if (entry.compileFailure()) {
                assertEquals(DartCandidateAnalysisStatus.REJECTED, result.status(), entry.name());
                assertTrue(result.diagnostics().stream().anyMatch(d -> d.blocking()
                        && d.offset() >= 0 && d.offset() + d.length() <= source.length()
                        && source.substring(d.offset(), d.offset() + d.length()).contains(entry.badSymbol())),
                        () -> entry.name() + ": " + result.diagnostics());
                assertTrue(result.symbolEvidence().isEmpty(), entry.name());
            } else {
                assertNotEquals(DartCandidateAnalysisStatus.PASSED, result.status(), entry.name());
                assertTrue(result.symbolEvidence().stream().anyMatch(v -> !v.accepted()
                        && v.probe().expectedSymbolName().equals(entry.badSymbol())
                        && v.probe().staticTypeProbe().isPresent()),
                        () -> entry.name() + ": " + result);
            }
            if (!entry.badSymbol().isEmpty()) {
                assertInstanceOf(PairAnalyzedCandidateResult.Rejected.class, ticket.accept(result),
                        "Invalid SDK evidence cannot acquire pair-save authority: " + entry.name());
            }
            assertEquals(node, ((FdDecodeResult.Current) codec.decode(prepared.prospectiveFdBytes())).document().root());
            assertArrayEquals(baselineBytes, Files.readAllBytes(file));
            assertArrayEquals(pubspec, Files.readAllBytes(project.resolve("pubspec.yaml")));
            assertArrayEquals(packageConfig, Files.readAllBytes(project.resolve(".dart_tool/package_config.json")));
            assertArrayEquals(remote, Files.readAllBytes(lib.resolve("types.dart")));
            assertFalse(Files.exists(project.resolve("analysis_options.yaml")));
            assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
        }
    }


    private static Map<SlotName, WidgetSlot> slots(int mask) {
        return Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text("Theme child")));
    }

    private static Map<PropertyName, PropertyValue> base() {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        return values;
    }
    private static PropertyName p(String value) { return new PropertyName(value); }
    private static PropertyValue string(String value) { return new PropertyValue.StringValue(value); }
    private static PropertyValue bool(boolean value) { return new PropertyValue.BooleanValue(value); }
    private static PropertyValue nil() { return new PropertyValue.NullValue(); }
    private static PropertyValue integer(long value) { return new PropertyValue.IntegerValue(BigInteger.valueOf(value)); }
    private static String upper(String value) { return Character.toUpperCase(value.charAt(0)) + value.substring(1); }
    private static PropertyValue.DartObjectReferenceValue reference(String name) {
        return new PropertyValue.DartObjectReferenceValue(Optional.empty(), name, Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }
    private static PropertyValue.DartObjectReferenceValue member(String name, String member) {
        return new PropertyValue.DartObjectReferenceValue(Optional.empty(), name, Optional.of(member),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }
    private static PropertyValue.DartObjectReferenceValue factory(String name) {
        return new PropertyValue.DartObjectReferenceValue(Optional.empty(), name, Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(false));
    }
    private static PropertyValue.DartObjectReferenceValue external(String name) {
        return new PropertyValue.DartObjectReferenceValue(Optional.of("package:tooltip_theme_probe/types.dart"), name, Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }
    private static WidgetNode text(String value) {
        return new WidgetNode(StableId.random(),
                new WidgetTypeId("flutter.widgets.Text"), Map.of(p("data"), string(value)), Map.of());
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
                const TooltipThemeData _tooltipThemeData = TooltipThemeData(exitDuration: Duration(microseconds: 321));
                const TooltipThemeData _data = _tooltipThemeData;
                const _constraints = BoxConstraints(minWidth: 24, maxWidth: 400);
                const _padding = EdgeInsetsDirectional.fromSTEB(4, 3, 8, 5);
                const _margin = EdgeInsets.all(12);
                const Decoration _decoration = BoxDecoration(color: Color(0xff123456), borderRadius: BorderRadius.all(Radius.circular(8)));
                const Decoration _shapeDecoration = ShapeDecoration(shape: StadiumBorder(), color: Color(0xff345678));
                const _textStyle = TextStyle(fontSize: 16, color: Color(0xffffffff), fontWeight: FontWeight.w600);
                const _waitDurationUs = Duration(microseconds: 123456);
                const _showDurationUs = Duration(microseconds: 234567);
                const _exitDurationUs = Duration(microseconds: -123456);
                """);
        REFERENCE_TYPES.forEach((family, type) -> {
            declarations.append(type).append(" _make").append(upper(family)).append("() => _").append(family).append(";\n")
                    .append("dynamic _dynamic").append(upper(family)).append(" = _").append(family).append(";\n")
                    .append(type).append("? _nullable").append(upper(family)).append(" = _").append(family).append(";\n")
                    .append("const _wrong").append(upper(family)).append(" = 1;\n");
        });
        declarations.append("class References {\n");
        REFERENCE_TYPES.forEach((family, type) -> declarations.append("static ").append(type).append(" get ").append(family).append(" => _").append(family).append(";\n"));
        declarations.append("}\n");
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
        addPackage(packages, "tooltip_theme_probe", project, "3.10");
        addPackage(packages, "flutter", sdk.resolve("packages/flutter"), "3.10");
        addPackage(packages, "sky_engine", sdk.resolve("bin/cache/pkg/sky_engine"), "3.10");
        for (var dependency : Map.of("characters", "1.4.1", "collection", "1.19.1",
                "material_color_utilities", "0.13.0", "meta", "1.18.0", "vector_math", "2.2.0").entrySet()) {
            addPackage(packages, dependency.getKey(), cache.resolve("hosted/pub.dev")
                    .resolve(dependency.getKey() + "-" + dependency.getValue()), "3.4");
        }
        Path tool = Files.createDirectories(project.resolve(".dart_tool"));
        Files.writeString(tool.resolve("package_config.json"), JSON.writeValueAsString(config), StandardCharsets.UTF_8);
        Files.writeString(project.resolve("pubspec.yaml"), "name: tooltip_theme_probe\nenvironment:\n  sdk: ^3.10.0\n"
                + "dependencies:\n  flutter:\n    sdk: flutter\nflutter:\n  uses-material-design: true\n", StandardCharsets.UTF_8);
    }

    private static void addPackage(ArrayNode packages, String name, Path root, String language) {
        assertTrue(Files.isDirectory(root), "Pinned package missing: " + root);
        packages.addObject().put("name", name).put("rootUri", root.toUri().toString())
                .put("packageUri", "lib/").put("languageVersion", language);
    }
}
