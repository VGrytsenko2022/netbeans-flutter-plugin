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
import io.github.vgrytsenko2022.plugin.designer.properties.TooltipPropertyContractTest;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Exact Tooltip constructor, compound/callback proofs and immutable pair-save candidates. */
class TooltipCandidateRealSdkTest {
    @TempDir Path workspace;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final StableId DOCUMENT_ID = StableId.parse("78000000-0000-4000-8000-000000000001");
    private static final WidgetTypeId TOOLTIP = new WidgetTypeId("flutter.material.Tooltip");

    private static final Map<String, String> REFERENCE_TYPES = Map.ofEntries(
            Map.entry("richMessage", "InlineSpan"), Map.entry("constraints", "BoxConstraints"),
            Map.entry("padding", "EdgeInsetsGeometry"), Map.entry("margin", "EdgeInsetsGeometry"),
            Map.entry("decoration", "Decoration"), Map.entry("textStyle", "TextStyle"),
            Map.entry("waitDurationUs", "Duration"), Map.entry("showDurationUs", "Duration"),
            Map.entry("exitDurationUs", "Duration"), Map.entry("onTriggered", "TooltipTriggeredCallback"),
            Map.entry("mouseCursor", "MouseCursor"), Map.entry("positionDelegate", "TooltipPositionDelegate"));

    @Test
    void completeLocalCompoundFieldsContentBranchesNullsAndOmissionHaveExactSdkProofs() throws Exception {
        var cases = new ArrayList<Candidate>();
        cases.add(valid("all-local-fields", new LinkedHashMap<>(TooltipPropertyContractTest.full())));
        var paints = new LinkedHashMap<>(TooltipPropertyContractTest.full());
        paints.remove(p("textStyleColor"));
        paints.remove(p("textStyleBackgroundColor"));
        paints.put(p("textStyleForeground"), TooltipPropertyContractTest.value("textStyleForeground"));
        paints.put(p("textStyleBackground"), TooltipPropertyContractTest.value("textStyleBackground"));
        cases.add(valid("complementary-local-text-paints", paints));
        cases.add(valid("defaults", base()));
        cases.add(valid("empty-message", Map.of(p("message"), string(""))));
        cases.add(valid("unicode-message", Map.of(p("message"), string("Info:\n 'quoted' Привіт 🚀"))));
        var nulls = base();
        for (String name : REFERENCE_TYPES.keySet()) nulls.put(p(name), nil());
        nulls.put(p("height"), nil());
        cases.add(valid("null-optionals", nulls));
        for (String symbol : List.of("_richMessage", "_widgetSpan", "_interactiveSpan")) {
            cases.add(valid("span-" + symbol, Map.of(p("message"), nil(), p("richMessage"), reference(symbol))));
        }
        cases.add(valid("shape-decoration", Map.of(p("message"), string("Shape"), p("decoration"), reference("_shapeDecoration"))));
        cases.add(valid("imported-rich-message", Map.of(p("richMessage"), external("remoteSpan"))));
        validate(cases);
    }

    @Test
    void everyDirectReferenceFamilySupportsGettersFactoriesAndMembers() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (int access = 0; access < 3; access++) {
            var values = base();
            values.put(p("message"), nil());
            for (String name : new TreeSet<>(REFERENCE_TYPES.keySet())) {
                values.put(p(name), access == 0 ? reference("_" + name)
                        : access == 1 ? factory("_make" + upper(name)) : member("References", name));
            }
            cases.add(valid("strict-families-" + access, values));
        }
        validate(cases);
    }

    @Test
    void wrongDynamicAndNullableValuesCannotAcquireSaveAuthorityForAnyReferenceFamily() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (String family : new TreeSet<>(REFERENCE_TYPES.keySet())) {
            for (String kind : List.of("dynamic", "nullable", "wrong")) {
                var values = base();
                if (family.equals("richMessage")) values.put(p("message"), nil());
                String symbol = "_" + kind + upper(family);
                values.put(p(family), reference(symbol));
                cases.add(kind.equals("wrong") ? compileFailure(kind + "-" + family, values, symbol)
                        : proofFailure(kind + "-" + family, values, symbol));
            }
        }
        assertEquals(36, cases.size());
        validate(cases);
    }

    @Test
    void allModesNullableFlagsChildLayoutsAndExactSignedDurationsCompileTogether() throws Exception {
        var nodes = new ArrayList<WidgetNode>();
        for (String mode : List.of("manual", "longPress", "tap")) {
            for (int mask = 0; mask < 2; mask++) {
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
        for (String name : List.of("preferBelow", "excludeFromSemantics", "enableTapToDismiss", "enableFeedback", "ignorePointer")) {
            for (boolean flag : List.of(false, true)) {
                var values = base(); values.put(p(name), bool(flag)); nodes.add(tile(values, 1));
            }
            if (!name.equals("enableTapToDismiss")) {
                var values = base(); values.put(p(name), nil()); nodes.add(tile(values, 1));
            }
        }
        for (String alignment : List.of("left", "right", "center", "justify", "start", "end")) {
            var values = base(); values.put(p("textAlign"), new PropertyValue.EnumValue("TextAlign", alignment)); nodes.add(tile(values, 1));
        }
        assertEquals(50, nodes.size());
        var root = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(nodes)));
        validate(List.of(new Candidate("50-tile-sdk-matrix", base(), "", false, 0, root)));
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
        Path project = Files.createDirectories(workspace.resolve("tooltip_probe"));
        Path lib = Files.createDirectories(project.resolve("lib"));
        configure(project, sdk, cache);
        Files.writeString(lib.resolve("types.dart"), """
                import 'package:flutter/material.dart';
                const InlineSpan remoteSpan = TextSpan(text: "Imported rich message", children: [WidgetSpan(child: Icon(Icons.info))]);
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
            assertTrue(source.contains("Tooltip("), source);
            assertFalse(source.contains("_TooltipState"), "No private SDK implementation types");
            if (entry.name().equals("defaults")) {
                String build = generation.generated().orElseThrow().build().payload();
                assertFalse(build.contains("onTriggered:"), build);
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
        return Map.of(new SlotName("child"), (mask & 1) == 0
                ? WidgetSlot.SingleSlot.empty() : WidgetSlot.SingleSlot.of(text("Tooltip child")));
    }

    private static Map<PropertyName, PropertyValue> base() {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        values.put(p("message"), string("Tooltip"));
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
        return new PropertyValue.DartObjectReferenceValue(Optional.of("package:tooltip_probe/types.dart"), name, Optional.empty(),
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
                void _onTriggered() {}
                const InlineSpan _richMessage = TextSpan(text: "Rich ", children: [TextSpan(text: "bold", style: TextStyle(fontWeight: FontWeight.bold))]);
                const InlineSpan _widgetSpan = TextSpan(children: [WidgetSpan(child: Icon(Icons.info)), TextSpan(text: " icon tooltip")]);
                final InlineSpan _interactiveSpan = TextSpan(text: "Open", recognizer: null, semanticsLabel: "Open details");
                const _constraints = BoxConstraints(minWidth: 24, maxWidth: 400);
                const _padding = EdgeInsetsDirectional.fromSTEB(4, 3, 8, 5);
                const _margin = EdgeInsets.all(12);
                const Decoration _decoration = BoxDecoration(color: Color(0xff123456), borderRadius: BorderRadius.all(Radius.circular(8)));
                const Decoration _shapeDecoration = ShapeDecoration(shape: StadiumBorder(), color: Color(0xff345678));
                const _textStyle = TextStyle(fontSize: 16, color: Color(0xffffffff), fontWeight: FontWeight.w600);
                const _waitDurationUs = Duration(microseconds: 123456);
                const _showDurationUs = Duration(microseconds: 234567);
                const _exitDurationUs = Duration(microseconds: -123456);
                const MouseCursor _mouseCursor = SystemMouseCursors.help;
                Offset _positionDelegate(TooltipPositionContext context) => Offset(context.target.dx, context.target.dy + context.verticalOffset);
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
        addPackage(packages, "tooltip_probe", project, "3.10");
        addPackage(packages, "flutter", sdk.resolve("packages/flutter"), "3.10");
        addPackage(packages, "sky_engine", sdk.resolve("bin/cache/pkg/sky_engine"), "3.10");
        for (var dependency : Map.of("characters", "1.4.1", "collection", "1.19.1",
                "material_color_utilities", "0.13.0", "meta", "1.18.0", "vector_math", "2.2.0").entrySet()) {
            addPackage(packages, dependency.getKey(), cache.resolve("hosted/pub.dev")
                    .resolve(dependency.getKey() + "-" + dependency.getValue()), "3.4");
        }
        Path tool = Files.createDirectories(project.resolve(".dart_tool"));
        Files.writeString(tool.resolve("package_config.json"), JSON.writeValueAsString(config), StandardCharsets.UTF_8);
        Files.writeString(project.resolve("pubspec.yaml"), "name: tooltip_probe\nenvironment:\n  sdk: ^3.10.0\n"
                + "dependencies:\n  flutter:\n    sdk: flutter\nflutter:\n  uses-material-design: true\n", StandardCharsets.UTF_8);
    }

    private static void addPackage(ArrayNode packages, String name, Path root, String language) {
        assertTrue(Files.isDirectory(root), "Pinned package missing: " + root);
        packages.addObject().put("name", name).put("rootUri", root.toUri().toString())
                .put("packageUri", "lib/").put("languageVersion", language);
    }
}
