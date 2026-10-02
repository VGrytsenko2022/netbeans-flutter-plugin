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
import io.github.vgrytsenko2022.plugin.designer.properties.MenuAnchorPropertyContractTest;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Complete MenuAnchor styles, constructor, SDK proof and immutable pair-save candidates. */
class MenuAnchorCandidateRealSdkTest {
    @TempDir Path workspace;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final StableId DOCUMENT_ID = StableId.parse("81000000-0000-4000-8000-000000000001");
    private static final WidgetTypeId MENU_ANCHOR = new WidgetTypeId("flutter.material.MenuAnchor");

    private static final Map<String, String> REFERENCE_TYPES = Map.ofEntries(
            Map.entry("controller", "MenuController"), Map.entry("childFocusNode", "FocusNode"),
            Map.entry("style", "MenuStyle"), Map.entry("alignmentOffset", "Offset"),
            Map.entry("reservedPadding", "EdgeInsetsGeometry"), Map.entry("layerLink", "LayerLink"),
            Map.entry("onOpen", "VoidCallback"), Map.entry("onClose", "VoidCallback"),
            Map.entry("onAnimationStatusChanged", "ValueChanged<AnimationStatus>"),
            Map.entry("builder", "MenuAnchorChildBuilder"));

    @Test
    void completeLocalStylesWholeStyleDefaultsNullsAndDeprecatedArgumentKeepExactProofs() throws Exception {
        var cases = new ArrayList<Candidate>();
        cases.add(valid("dense-physical", MenuAnchorPropertyContractTest.full(false, false)));
        cases.add(valid("dense-directional", MenuAnchorPropertyContractTest.full(true, true)));
        cases.add(valid("defaults", base()));
        var nulls = base();
        for (String name : REFERENCE_TYPES.keySet()) nulls.put(p(name), nil());
        cases.add(valid("null-optionals", nulls));
        var imported = base(); imported.put(p("style"), external("remoteStyle"));
        cases.add(valid("imported-style", imported));
        var whole = base(); whole.put(p("style"), reference("_style"));
        cases.add(valid("whole-style", whole));
        var infinite = base(); infinite.put(p("alignmentOffset"), reference("_nonfiniteOffset"));
        cases.add(valid("constructor-nonfinite-offset-not-mount-validity", infinite));
        var deprecated = base(); deprecated.put(p("anchorTapClosesMenu"), bool(true));
        cases.add(valid("deprecated-inert-argument-preserved", deprecated));
        validate(cases);
    }

    @Test
    void allStrictFamiliesAcceptReferencesMembersAndZeroArgumentFactories() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (int access = 0; access < 3; access++) {
            var values = base();
            for (String name : new TreeSet<>(REFERENCE_TYPES.keySet())) values.put(p(name),
                    access == 0 ? reference("_" + name)
                    : access == 1 ? factory("_make" + upper(name)) : member("References", name));
            cases.add(valid("all-strict-families-" + access, values));
        }
        validate(cases);
    }

    @Test
    void wrongDynamicAndNullableReferencesCannotAcquireSaveAuthority() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (String name : new TreeSet<>(REFERENCE_TYPES.keySet())) {
            for (String prefix : List.of("_wrong", "_dynamic", "_nullable")) {
                var values = base(); String symbol = prefix + upper(name);
                values.put(p(name), reference(symbol));
                cases.add(prefix.equals("_wrong") ? compileFailure("wrong-" + name, values, symbol)
                        : proofFailure(prefix + "-" + name, values, symbol));
            }
        }
        validate(cases);
    }

    @Test
    void everyBooleanClipAndOptionalChildMenuListShapeGeneratesCompleteConstructors() throws Exception {
        var nodes = new ArrayList<WidgetNode>();
        for (String property : List.of("anchorTapClosesMenu", "consumeOutsideTap", "crossAxisUnconstrained", "useRootOverlay", "animated")) {
            for (boolean value : List.of(false, true)) {
                var values = base(); values.put(p(property), bool(value)); nodes.add(anchor(values, 1, 1));
            }
        }
        for (String clip : List.of("none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer")) {
            var values = base(); values.put(p("clipBehavior"), new PropertyValue.EnumValue("Clip", clip)); nodes.add(anchor(values, 1, 1));
        }
        for (int child = 0; child < 2; child++) for (int count = 0; count < 3; count++) nodes.add(anchor(base(), child, count));
        for (double x : List.of(-3.5, 0.0, 11.25)) {
            var values = base(); values.put(p("alignmentOffset"), new PropertyValue.OffsetValue(java.math.BigDecimal.valueOf(x), java.math.BigDecimal.valueOf(-x)));
            nodes.add(anchor(values, 1, 1));
        }
        assertEquals(23, nodes.size());
        var root = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(nodes)));
        validate(List.of(new Candidate("all-constructor-branches", Map.of(), "", false, 1, root)));
    }

    private static WidgetNode anchor(Map<PropertyName, PropertyValue> values, int child, int count) {
        return new WidgetNode(StableId.random(), MENU_ANCHOR, values, slots(child, count));
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
        Path project = Files.createDirectories(workspace.resolve("menu_anchor_probe"));
        Path lib = Files.createDirectories(project.resolve("lib"));
        configure(project, sdk, cache);
        Files.writeString(lib.resolve("types.dart"), """
                import 'package:flutter/material.dart';
                const MenuStyle remoteStyle = MenuStyle(elevation: WidgetStatePropertyAll<double?>(4));
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
            var node = entry.suppliedRoot() == null ? new WidgetNode(StableId.random(), MENU_ANCHOR, entry.values(), slots(entry.slotMask())) : entry.suppliedRoot();
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
            assertTrue(source.contains("MenuAnchor("), source);
            assertFalse(source.contains("onPressed:"), source);
            assertFalse(source.contains("ButtonStyle("), source);
            assertFalse(source.contains("MenuButtonTheme"), source);
            if (entry.name().startsWith("dense-")) {
                assertTrue(probes.size() > 200, "Retain complete local menu-style proofs");
                assertTrue(source.contains("MenuStyle("), source);
            }
            if (entry.name().equals("defaults")) {
                String build = generation.generated().orElseThrow().build().payload();
                assertTrue(build.contains("menuChildren:"), build);
                assertFalse(build.contains("controller:"), build);
                assertFalse(build.contains("builder:"), build);
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


    private static Map<SlotName, WidgetSlot> slots(int mask) { return slots(mask & 1, 1); }
    private static Map<SlotName, WidgetSlot> slots(int child, int count) {
        var menu = new ArrayList<WidgetNode>();
        for (int index = 0; index < count; index++) menu.add(text("Menu item " + index));
        return Map.of(new SlotName("child"), child == 0 ? WidgetSlot.SingleSlot.empty() : WidgetSlot.SingleSlot.of(text("Anchor")),
                new SlotName("menuChildren"), new WidgetSlot.ListSlot(menu));
    }
    private static Map<PropertyName, PropertyValue> base() { return new LinkedHashMap<>(); }
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
        return new PropertyValue.DartObjectReferenceValue(Optional.of("package:menu_anchor_probe/types.dart"), name, Optional.empty(),
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
                final MenuController _controller = MenuController();
                final FocusNode _childFocusNode = FocusNode();
                const MenuStyle _style = MenuStyle();
                const Offset _alignmentOffset = Offset(3, -2);
                const Offset _nonfiniteOffset = Offset(double.infinity, double.nan);
                const EdgeInsetsGeometry _reservedPadding = EdgeInsetsDirectional.fromSTEB(3, 4, 5, 6);
                final LayerLink _layerLink = LayerLink();
                void _onOpen() {}
                void _onClose() {}
                void _onAnimationStatusChanged(AnimationStatus value) {}
                Widget _builder(BuildContext context, MenuController controller, Widget? child) => child ?? const SizedBox.shrink();
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
        addPackage(packages, "menu_anchor_probe", project, "3.10");
        addPackage(packages, "flutter", sdk.resolve("packages/flutter"), "3.10");
        addPackage(packages, "sky_engine", sdk.resolve("bin/cache/pkg/sky_engine"), "3.10");
        for (var dependency : Map.of("characters", "1.4.1", "collection", "1.19.1",
                "material_color_utilities", "0.13.0", "meta", "1.18.0", "vector_math", "2.2.0").entrySet()) {
            addPackage(packages, dependency.getKey(), cache.resolve("hosted/pub.dev")
                    .resolve(dependency.getKey() + "-" + dependency.getValue()), "3.4");
        }
        Path tool = Files.createDirectories(project.resolve(".dart_tool"));
        Files.writeString(tool.resolve("package_config.json"), JSON.writeValueAsString(config), StandardCharsets.UTF_8);
        Files.writeString(project.resolve("pubspec.yaml"), "name: menu_anchor_probe\nenvironment:\n  sdk: ^3.10.0\n"
                + "dependencies:\n  flutter:\n    sdk: flutter\nflutter:\n  uses-material-design: true\n", StandardCharsets.UTF_8);
    }

    private static void addPackage(ArrayNode packages, String name, Path root, String language) {
        assertTrue(Files.isDirectory(root), "Pinned package missing: " + root);
        packages.addObject().put("name", name).put("rootUri", root.toUri().toString())
                .put("packageUri", "lib/").put("languageVersion", language);
    }
}


