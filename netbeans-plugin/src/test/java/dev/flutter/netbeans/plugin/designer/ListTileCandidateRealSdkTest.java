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
import dev.flutter.netbeans.plugin.designer.properties.ListTilePropertyContractTest;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Exact ListTile constructor/source proofs and pair-save authority against the pinned SDK. */
class ListTileCandidateRealSdkTest {
    @TempDir Path workspace;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final StableId DOCUMENT_ID = StableId.parse("79000000-0000-4000-8000-000000000001");
    private static final WidgetTypeId LIST_TILE = new WidgetTypeId("flutter.material.ListTile");
    private static final List<String> SLOTS = List.of("leading", "title", "subtitle", "trailing");
    private static final List<String> COLORS = List.of("selectedColor", "iconColor", "textColor",
            "focusColor", "hoverColor", "splashColor", "tileColor", "selectedTileColor");

    @Test
    void denseLocalStylesStateMapsShapesAndEverySlotAcquirePairSaveAuthority() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (boolean physicalPadding : List.of(false, true)) {
            for (boolean foregroundPaint : List.of(false, true)) {
                cases.add(valid("dense-" + physicalPadding + "-" + foregroundPaint,
                        tile(ListTilePropertyContractTest.full(physicalPadding, foregroundPaint), 15)));
            }
        }
        validate(cases);
    }

    @Test
    void omittedDefaultsAllSlotCombinationsNullableBooleansAndFixedNonfiniteValuesCompile() throws Exception {
        var tiles = new ArrayList<WidgetNode>();
        for (int mask = 0; mask < 16; mask++) {
            var values = new LinkedHashMap<PropertyName, PropertyValue>();
            if ((mask & 4) != 0) values.put(p("isThreeLine"), new PropertyValue.BooleanValue(true));
            tiles.add(tile(values, mask));
        }
        for (String property : List.of("isThreeLine", "dense", "enableFeedback")) {
            tiles.add(tile(Map.of(p(property), new PropertyValue.NullValue()), 0));
        }
        for (String property : List.of("horizontalTitleGap", "minVerticalPadding", "minLeadingWidth", "minTileHeight")) {
            for (String member : List.of("infinity", "negativeInfinity", "nan")) {
                tiles.add(tile(Map.of(p(property), new PropertyValue.EnumValue("double", member)), 0));
            }
            tiles.add(tile(Map.of(p(property), new PropertyValue.NullValue()), 0));
        }
        validate(List.of(valid("defaults-slots-nonfinite", column(tiles))));
    }

    @Test
    void allEnumsWholeReferencesFactoriesAndDisabledStoredCallbacksKeepSdkSemantics() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (boolean factories : List.of(false, true)) {
            var values = new LinkedHashMap<PropertyName, PropertyValue>();
            COLORS.forEach(name -> values.put(p(name), ref("_color")));
            values.put(p("shape"), ref("_shape"));
            values.put(p("visualDensity"), ref("_density"));
            for (String name : List.of("titleTextStyle", "subtitleTextStyle", "leadingAndTrailingTextStyle")) {
                values.put(p(name), factories ? factory("_makeTextStyle") : ref("_textStyle"));
            }
            values.put(p("contentPadding"), factories ? factory("_makePadding") : ref("_padding"));
            values.put(p("mouseCursor"), ref("_cursor"));
            values.put(p("focusNode"), factories ? factory("_makeFocusNode") : ref("_focusNode"));
            values.put(p("statesController"), factories ? factory("_makeStatesController") : ref("_statesController"));
            values.put(p("onTap"), factories ? factory("_makeCallback") : ref("_onTap"));
            values.put(p("onLongPress"), ref("_onLongPress"));
            values.put(p("onFocusChange"), ref("_onFocusChange"));
            values.put(p("enabled"), new PropertyValue.BooleanValue(false));
            cases.add(valid("whole-" + factories, tile(values, 15)));
        }
        var tiles = new ArrayList<WidgetNode>();
        for (String style : List.of("list", "drawer")) {
            for (String alignment : List.of("threeLine", "titleHeight", "top", "center", "bottom")) {
                tiles.add(tile(Map.of(p("style"), new PropertyValue.EnumValue("ListTileStyle", style),
                        p("titleAlignment"), new PropertyValue.EnumValue("ListTileTitleAlignment", alignment),
                        p("onTap"), new PropertyValue.StringValue("noop"),
                        p("onLongPress"), new PropertyValue.NullValue(),
                        p("onFocusChange"), new PropertyValue.StringValue("noop")), 15));
            }
        }
        cases.add(valid("enums-and-controlled-callbacks", column(tiles)));
        validate(cases);
    }

    @Test
    void strictReferencesRejectDynamicNullableAndWrongTypesWithoutChangingUserFiles() throws Exception {
        var cases = new ArrayList<Candidate>();
        var properties = new LinkedHashMap<String, String>();
        properties.put("iconColor", "Color");
        properties.put("shape", "Shape");
        properties.put("visualDensity", "Density");
        properties.put("titleTextStyle", "TextStyle");
        properties.put("contentPadding", "Padding");
        properties.put("mouseCursor", "Cursor");
        properties.put("focusNode", "FocusNode");
        properties.put("statesController", "StatesController");
        properties.put("onTap", "Callback");
        properties.put("onFocusChange", "FocusCallback");
        for (var entry : properties.entrySet()) {
            for (String kind : List.of("dynamic", "nullable")) {
                String symbol = "_" + kind + entry.getValue();
                cases.add(new Candidate(entry.getKey() + "-" + kind,
                        tile(Map.of(p(entry.getKey()), ref(symbol)), 15), symbol, false));
            }
        }
        cases.add(new Candidate("wrong-color-type", tile(Map.of(p("textColor"), ref("_wrongType")), 15),
                "_wrongType", true));
        validate(cases);
    }

    private record Candidate(String name, WidgetNode root, String badSymbol, boolean compileFailure) { }
    private static Candidate valid(String name, WidgetNode root) { return new Candidate(name, root, "", false); }

    private void validate(List<Candidate> candidates) throws Exception {
        Path executable = configured("dart.executable"), sdk = configured("flutter.sdk"), cache = configured("pub.cache");
        Path project = Files.createDirectories(workspace.resolve("list_tile_probe"));
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
        var current = new FlutterDesignerDocumentState.Current(decoded,
                new dev.flutter.netbeans.designer.validation.ValidationResult(List.of()), catalog,
                List.of(), List.of(), Optional.of(baseline.source()), Optional.of(baseline));
        var analyzer = new DartCandidateAnalyzer(executable, ignored -> { });
        assertEquals(45, DartCandidateAnalysisLimits.DEFAULT.totalTimeout().toSeconds());
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
            String candidate = new String(prepared.prospectiveDartBytes(), StandardCharsets.UTF_8);
            String build = generation.generated().orElseThrow().build().payload();
            assertTrue(build.contains("ListTile("), build);
            assertFalse(build.contains("_ListTile"), "No private SDK implementation types");
            if (entry.name().startsWith("whole-")) {
                for (String callback : List.of("_onLongPress", "_onFocusChange",
                        entry.name().endsWith("true") ? "_makeCallback" : "_onTap")) {
                    assertTrue(build.contains(callback), "Disabled ListTile must retain callback presence: " + callback);
                }
            }
            var ticket = PairSaveEvidenceGate.prepareAnalysis(current, prepared, project, file,
                    DartCandidateWarningPolicy.ALLOW, sdk);
            assertEquals(probes, ticket.request().symbolProbes());
            var result = analyzer.analyze(ticket.request()).result().toCompletableFuture().get(60, TimeUnit.SECONDS);
            if (entry.badSymbol().isEmpty()) {
                assertEquals(DartCandidateAnalysisStatus.PASSED, result.status(), () -> entry.name() + ": "
                        + result.diagnostics().stream().filter(DartCandidateDiagnostic::blocking).toList()
                        + " issue=" + result.issue() + " rejected="
                        + result.symbolEvidence().stream().filter(value -> !value.accepted()).limit(5).toList());
                assertEquals(probes.size(), result.symbolEvidence().size());
                assertTrue(result.symbolEvidence().stream().allMatch(DartSymbolEvidence::accepted));
                assertInstanceOf(PairAnalyzedCandidateResult.Ready.class, ticket.accept(result),
                        () -> entry.name() + ": " + PairSaveEvidenceGate.evaluateAnalysis(ticket, result));
            } else {
                assertNotEquals(DartCandidateAnalysisStatus.PASSED, result.status(), entry.name());
                if (entry.compileFailure()) {
                    assertTrue(result.diagnostics().stream().anyMatch(d -> d.blocking()
                            && d.offset() >= 0 && d.offset() + d.length() <= candidate.length()
                            && candidate.substring(d.offset(), d.offset() + d.length()).contains(entry.badSymbol())),
                            () -> entry.name() + ": " + result.diagnostics());
                } else {
                    assertTrue(result.symbolEvidence().stream().anyMatch(value -> !value.accepted()
                            && value.probe().expectedSymbolName().equals(entry.badSymbol())
                            && value.probe().staticTypeProbe().isPresent()),
                            () -> entry.name() + ": missing exact rejected reference proof; " + result.issue());
                }
                assertInstanceOf(PairAnalyzedCandidateResult.Rejected.class, ticket.accept(result),
                        "Rejected SDK/static-type evidence must not acquire pair-save authority");
            }
            assertEquals(entry.root(), ((FdDecodeResult.Current) codec.decode(prepared.prospectiveFdBytes())).document().root());
            assertArrayEquals(baselineBytes, Files.readAllBytes(file));
            assertArrayEquals(pubspec, Files.readAllBytes(project.resolve("pubspec.yaml")));
            assertArrayEquals(packageConfig, Files.readAllBytes(project.resolve(".dart_tool/package_config.json")));
            assertFalse(Files.exists(project.resolve("analysis_options.yaml")));
            assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
        }
    }

    private static PropertyName p(String value) { return new PropertyName(value); }
    private static PropertyValue.DartObjectReferenceValue ref(String name) {
        return new PropertyValue.DartObjectReferenceValue(Optional.empty(), name, Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }
    private static PropertyValue.DartObjectReferenceValue factory(String name) {
        return new PropertyValue.DartObjectReferenceValue(Optional.empty(), name, Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(false));
    }
    private static WidgetNode text(String value) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(p("data"), new PropertyValue.StringValue(value)), Map.of());
    }
    private static WidgetNode tile(Map<PropertyName, PropertyValue> values, int mask) {
        var slots = new LinkedHashMap<SlotName, WidgetSlot>();
        for (int index = 0; index < SLOTS.size(); index++) {
            if ((mask & (1 << index)) != 0) {
                slots.put(new SlotName(SLOTS.get(index)), WidgetSlot.SingleSlot.of(text(SLOTS.get(index))));
            }
        }
        return new WidgetNode(StableId.random(), LIST_TILE, values, slots);
    }
    private static WidgetNode column(List<WidgetNode> children) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(children)));
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
                + """
                const _color = Color(0xff123456);
                const _shape = RoundedRectangleBorder();
                const _density = VisualDensity.compact;
                const _textStyle = TextStyle(fontSize: 18);
                const _padding = EdgeInsetsDirectional.only(start: -3, end: 12);
                final _cursor = SystemMouseCursors.click;
                final _focusNode = FocusNode();
                final _statesController = WidgetStatesController();
                void _onTap() {}
                void _onLongPress() {}
                void _onFocusChange(bool focused) {}
                TextStyle _makeTextStyle() => throw StateError('Never execute a project factory');
                EdgeInsetsGeometry _makePadding() => throw StateError('Never execute a project factory');
                FocusNode _makeFocusNode() => throw StateError('Never execute a project factory');
                WidgetStatesController _makeStatesController() => throw StateError('Never execute a project factory');
                VoidCallback _makeCallback() => throw StateError('Never execute a project factory');
                dynamic _dynamicColor = _color; Color? _nullableColor = _color;
                dynamic _dynamicShape = _shape; ShapeBorder? _nullableShape = _shape;
                dynamic _dynamicDensity = _density; VisualDensity? _nullableDensity = _density;
                dynamic _dynamicTextStyle = _textStyle; TextStyle? _nullableTextStyle = _textStyle;
                dynamic _dynamicPadding = _padding; EdgeInsetsGeometry? _nullablePadding = _padding;
                dynamic _dynamicCursor = _cursor; MouseCursor? _nullableCursor = _cursor;
                dynamic _dynamicFocusNode = _focusNode; FocusNode? _nullableFocusNode = _focusNode;
                dynamic _dynamicStatesController = _statesController; WidgetStatesController? _nullableStatesController = _statesController;
                dynamic _dynamicCallback = _onTap; VoidCallback? _nullableCallback = _onTap;
                dynamic _dynamicFocusCallback = _onFocusChange; ValueChanged<bool>? _nullableFocusCallback = _onFocusChange;
                const _wrongType = 'not a color';
                class HomePage extends StatelessWidget {
                  // <netbeans-flutter-designer region="build">
                """ + generated.build().payload()
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
        addPackage(packages, "list_tile_probe", project, "3.10");
        addPackage(packages, "flutter", sdk.resolve("packages/flutter"), "3.10");
        addPackage(packages, "sky_engine", sdk.resolve("bin/cache/pkg/sky_engine"), "3.10");
        for (var dependency : Map.of("characters", "1.4.1", "collection", "1.19.1",
                "material_color_utilities", "0.13.0", "meta", "1.18.0", "vector_math", "2.2.0").entrySet()) {
            addPackage(packages, dependency.getKey(), cache.resolve("hosted/pub.dev")
                    .resolve(dependency.getKey() + "-" + dependency.getValue()), "3.4");
        }
        Path tool = Files.createDirectories(project.resolve(".dart_tool"));
        Files.writeString(tool.resolve("package_config.json"), JSON.writeValueAsString(config), StandardCharsets.UTF_8);
        Files.writeString(project.resolve("pubspec.yaml"), "name: list_tile_probe\nenvironment:\n  sdk: ^3.10.0\n"
                + "dependencies:\n  flutter:\n    sdk: flutter\n", StandardCharsets.UTF_8);
    }
    private static void addPackage(ArrayNode packages, String name, Path root, String language) {
        assertTrue(Files.isDirectory(root), "Pinned package missing: " + root);
        packages.addObject().put("name", name).put("rootUri", root.toUri().toString())
                .put("packageUri", "lib/").put("languageVersion", language);
    }
}
