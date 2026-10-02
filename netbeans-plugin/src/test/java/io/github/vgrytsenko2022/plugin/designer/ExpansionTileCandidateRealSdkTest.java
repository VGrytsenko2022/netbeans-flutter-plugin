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
import io.github.vgrytsenko2022.plugin.designer.properties.ExpansionTilePropertyContractTest;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Exact ExpansionTile constructor, compound/callback proofs and immutable pair-save candidates. */
class ExpansionTileCandidateRealSdkTest {
    @TempDir Path workspace;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final StableId DOCUMENT_ID = StableId.parse("78000000-0000-4000-8000-000000000001");
    private static final WidgetTypeId RADIO = new WidgetTypeId("flutter.material.ExpansionTile");


    private static final Map<String, String> REFERENCE_TYPES = Map.ofEntries(
            Map.entry("onExpansionChanged", "ValueChanged<bool>"), Map.entry("controller", "ExpansibleController"),
            Map.entry("statesController", "WidgetStatesController"), Map.entry("expansionAnimationStyle", "AnimationStyle"),
            Map.entry("shape", "ShapeBorder"), Map.entry("collapsedShape", "ShapeBorder"),
            Map.entry("visualDensity", "VisualDensity"), Map.entry("tilePadding", "EdgeInsetsGeometry"),
            Map.entry("childrenPadding", "EdgeInsetsGeometry"), Map.entry("expandedAlignment", "AlignmentGeometry"),
            Map.entry("backgroundColor", "Color"), Map.entry("collapsedBackgroundColor", "Color"),
            Map.entry("textColor", "Color"), Map.entry("collapsedTextColor", "Color"),
            Map.entry("iconColor", "Color"), Map.entry("collapsedIconColor", "Color"), Map.entry("splashColor", "Color"),
            Map.entry("expansionAnimationStyleDurationUs", "Duration"), Map.entry("expansionAnimationStyleReverseDurationUs", "Duration"),
            Map.entry("expansionAnimationStyleCurve", "Curve"), Map.entry("expansionAnimationStyleReverseCurve", "Curve"));
    private static boolean animationLeaf(String name) { return name.startsWith("expansionAnimationStyle") && !name.equals("expansionAnimationStyle"); }

    @Test
    void completeLocalStylesNoAnimationNullAndOmissionHaveExactSdkProofs() throws Exception {
        var cases = new ArrayList<Candidate>();
        cases.add(valid("all-local-fields", new LinkedHashMap<>(ExpansionTilePropertyContractTest.full())));
        cases.add(valid("defaults", base()));
        cases.add(valid("no-animation", Map.of(p("expansionAnimationStyle"), string("noAnimation"))));
        cases.add(valid("null-animation-and-callback", Map.of(p("expansionAnimationStyle"), nil(), p("onExpansionChanged"), nil())));
        var empty = base();
        for (String name : REFERENCE_TYPES.keySet()) if (animationLeaf(name)) empty.put(p(name), nil());
        cases.add(valid("explicit-empty-local-animation", empty));
        validate(cases);
    }

    @Test
    void wholeObjectsAndEveryAnimationLeafSupportReferencesFactoriesAndMembers() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (int access = 0; access < 3; access++) {
            for (boolean leaves : List.of(false, true)) {
                var values = base();
                for (String name : REFERENCE_TYPES.keySet()) {
                    if (leaves != animationLeaf(name)) continue;
                    values.put(p(name), access == 0 ? reference("_" + name)
                            : access == 1 ? factory("_make" + upper(name)) : member("References", name));
                }
                cases.add(valid("strict-families-" + access + "-" + leaves, values));
            }
        }
        var alias = base(); alias.put(p("controller"), reference("_legacyController"));
        cases.add(valid("deprecated-controller-alias-retains-exact-type", alias));
        validate(cases);
    }

    @Test
    void wrongDynamicAndNullableValuesCannotAcquireSaveAuthorityForAnyReferenceFamily() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (String family : new TreeSet<>(REFERENCE_TYPES.keySet())) {
            for (String kind : List.of("dynamic", "nullable", "wrong")) {
                var values = base(); String symbol = "_" + kind + upper(family);
                values.put(p(family), reference(symbol));
                cases.add(kind.equals("wrong") ? compileFailure(kind + "-" + family, values, symbol)
                        : proofFailure(kind + "-" + family, values, symbol));
            }
        }
        assertEquals(63, cases.size());
        validate(cases);
    }

    @Test
    void allCurvesSlotLayoutsNullableFieldsAndSignedGeometryCompileTogether() throws Exception {
        var tiles = new ArrayList<WidgetNode>();
        for (String curve : ExpansionTileWidgetPropertySchema.curvePresets()) {
            for (String property : List.of("expansionAnimationStyleCurve", "expansionAnimationStyleReverseCurve")) {
                tiles.add(tile(Map.of(p(property), string(curve)), 0));
            }
        }
        for (int mask = 0; mask < 16; mask++) tiles.add(tile(base(), mask));
        for (String affinity : List.of("leading", "trailing", "platform")) {
            tiles.add(tile(Map.of(p("controlAffinity"), new PropertyValue.EnumValue("ListTileControlAffinity", affinity)), 15));
        }
        for (String name : List.of("minTileHeight", "expansionAnimationStyleDurationUs", "expansionAnimationStyleReverseDurationUs")) {
            for (PropertyValue value : List.of(integer(-1), integer(0), integer(1234567), nil())) tiles.add(tile(Map.of(p(name), value), 0));
        }
        for (String member : List.of("infinity", "negativeInfinity", "nan")) {
            tiles.add(tile(Map.of(p("minTileHeight"), new PropertyValue.EnumValue("double", member)), 0));
        }
        for (String name : List.of("dense", "enableFeedback")) tiles.add(tile(Map.of(p(name), nil()), 0));
        assertEquals(122, tiles.size());
        var root = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(tiles)));
        validate(List.of(new Candidate("122-tile-sdk-matrix", base(), "", false, 0, root)));
    }

    @Test
    void preciseMicrosecondsAndIgnoredReverseDurationRemainSourceFaithful() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (long duration : new long[] {1, -1, 9_007_199_254_740_991L, -9_007_199_254_740_991L}) {
            cases.add(valid("duration-us-" + duration, Map.of(p("expansionAnimationStyleDurationUs"), integer(duration))));
            cases.add(valid("reverse-duration-us-" + duration, Map.of(p("expansionAnimationStyleReverseDurationUs"), integer(duration))));
        }
        validate(cases);
    }

    private static WidgetNode tile(Map<PropertyName, PropertyValue> values, int mask) {
        return new WidgetNode(StableId.random(), RADIO, values, slots(mask));
    }

    private record Candidate(String name, Map<PropertyName, PropertyValue> values, String badSymbol, boolean compileFailure, int slotMask, WidgetNode suppliedRoot) {
        Candidate(String name, Map<PropertyName, PropertyValue> values, String badSymbol, boolean compileFailure) {
            this(name, values, badSymbol, compileFailure, 7, null);
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
        Path project = Files.createDirectories(workspace.resolve("expansion_tile_probe"));
        Path lib = Files.createDirectories(project.resolve("lib"));
        configure(project, sdk, cache);
        Files.writeString(lib.resolve("types.dart"), """
                import 'package:flutter/material.dart';
                enum RemoteChoice { first, second }
                RemoteChoice? remoteChoice;
                void remoteChanged(RemoteChoice? value) {}
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
            var node = entry.suppliedRoot() == null ? new WidgetNode(StableId.random(), RADIO, entry.values(), slots(entry.slotMask())) : entry.suppliedRoot();
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
            assertTrue(source.contains("ExpansionTile("), source);
            assertFalse(source.contains("_ExpansionTileState"), "No private SDK implementation types");
            if (entry.name().equals("defaults")) {
                String build = generation.generated().orElseThrow().build().payload();
                assertFalse(build.contains("onExpansionChanged:"), build);
                assertFalse(build.contains("enabled:"), build);
            }
            if (entry.name().equals("null-animation-and-callback")) {
                assertTrue(source.contains("onExpansionChanged: null"), source);
                assertTrue(source.contains("expansionAnimationStyle: null"), source);
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
        var slots = new LinkedHashMap<SlotName, WidgetSlot>();
        int bit = 0;
        slots.put(new SlotName("title"), WidgetSlot.SingleSlot.of(text("Required title")));
        for (String name : List.of("leading", "subtitle", "trailing")) {
            if ((mask & (1 << bit++)) != 0) slots.put(new SlotName(name), WidgetSlot.SingleSlot.of(text(name)));
        }
        slots.put(new SlotName("children"), new WidgetSlot.ListSlot((mask & 8) == 0 ? List.of() : List.of(text("First child"), text("Second child"))));
        return slots;
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
        return new PropertyValue.DartObjectReferenceValue(Optional.of("package:expansion_tile_probe/types.dart"), name, Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }
    private static PropertyValue.DartObjectReferenceValue externalMember(String name, String member) {
        return new PropertyValue.DartObjectReferenceValue(Optional.of("package:expansion_tile_probe/types.dart"), name, Optional.of(member),
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
                void _onExpansionChanged(bool expanded) {}
                final _controller = ExpansibleController();
                final ExpansionTileController _legacyController = ExpansibleController();
                final _statesController = WidgetStatesController();
                const _expansionAnimationStyle = AnimationStyle(duration: Duration(microseconds: 234567), curve: Curves.easeInOut);
                const _shape = RoundedRectangleBorder(borderRadius: BorderRadius.all(Radius.circular(8)));
                const _collapsedShape = CircleBorder();
                const _visualDensity = VisualDensity(horizontal: 1);
                const _tilePadding = EdgeInsetsDirectional.fromSTEB(-2, 4, 10, 6);
                const _childrenPadding = EdgeInsets.all(12);
                const _expandedAlignment = AlignmentDirectional.centerStart;
                const _backgroundColor = Color(0xff123456);
                const _collapsedBackgroundColor = Color(0xff234567);
                const _textColor = Color(0xff345678);
                const _collapsedTextColor = Color(0xff456789);
                const _iconColor = Color(0xff56789a);
                const _collapsedIconColor = Color(0xff6789ab);
                const _splashColor = Color(0xff789abc);
                const _expansionAnimationStyleDurationUs = Duration(microseconds: 123456);
                const _expansionAnimationStyleReverseDurationUs = Duration(microseconds: -234567);
                const _expansionAnimationStyleCurve = Curves.easeIn;
                const _expansionAnimationStyleReverseCurve = Curves.easeOut;
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
        addPackage(packages, "expansion_tile_probe", project, "3.10");
        addPackage(packages, "flutter", sdk.resolve("packages/flutter"), "3.10");
        addPackage(packages, "sky_engine", sdk.resolve("bin/cache/pkg/sky_engine"), "3.10");
        for (var dependency : Map.of("characters", "1.4.1", "collection", "1.19.1",
                "material_color_utilities", "0.13.0", "meta", "1.18.0", "vector_math", "2.2.0").entrySet()) {
            addPackage(packages, dependency.getKey(), cache.resolve("hosted/pub.dev")
                    .resolve(dependency.getKey() + "-" + dependency.getValue()), "3.4");
        }
        Path tool = Files.createDirectories(project.resolve(".dart_tool"));
        Files.writeString(tool.resolve("package_config.json"), JSON.writeValueAsString(config), StandardCharsets.UTF_8);
        Files.writeString(project.resolve("pubspec.yaml"), "name: expansion_tile_probe\nenvironment:\n  sdk: ^3.10.0\n"
                + "dependencies:\n  flutter:\n    sdk: flutter\nflutter:\n  uses-material-design: true\n", StandardCharsets.UTF_8);
    }

    private static void addPackage(ArrayNode packages, String name, Path root, String language) {
        assertTrue(Files.isDirectory(root), "Pinned package missing: " + root);
        packages.addObject().put("name", name).put("rootUri", root.toUri().toString())
                .put("packageUri", "lib/").put("languageVersion", language);
    }
}
