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
import io.github.vgrytsenko2022.plugin.designer.properties.CheckboxListTilePropertyContractTest;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Exact CheckboxListTile constructor/source proofs and pair-save authority against the pinned SDK. */
class CheckboxListTileCandidateRealSdkTest {
    @TempDir Path workspace;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final StableId DOCUMENT_ID = StableId.parse("7a000000-0000-4000-8000-000000000001");
    private static final WidgetTypeId CHECKBOX_LIST_TILE = new WidgetTypeId("flutter.material.CheckboxListTile");
    private static final List<String> SLOTS = List.of("title", "subtitle", "secondary");
    private static final List<String> COLORS = List.of("activeColor", "checkColor", "hoverColor", "tileColor", "selectedTileColor");

    @Test
    void denseLocalTileAndCheckboxShapesStatesAndAllSlotsAcquirePairSaveAuthority() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (String variant : List.of("standard", "adaptive")) {
            for (boolean physical : List.of(false, true)) {
                var values = CheckboxListTilePropertyContractTest.full();
                values.put(p("variant"), new PropertyValue.StringValue(variant));
                values.put(p("contentPadding"), physical
                        ? new PropertyValue.EdgeInsetsValue(number(-2), number(7), number(12), number(5))
                        : new PropertyValue.EdgeInsetsDirectionalValue(number(-3), number(7), number(13), number(5)));
                cases.add(valid("dense-" + variant + "-" + physical, tile(values, 7)));
            }
        }
        validate(cases);
    }

    @Test
    void defaultsEverySlotNullableBooleansTristateAndSignedNonfiniteNumbersCompile() throws Exception {
        var tiles = new ArrayList<WidgetNode>();
        for (String variant : List.of("standard", "adaptive")) {
            for (int mask = 0; mask < 8; mask++) {
                var values = new LinkedHashMap<PropertyName, PropertyValue>();
                values.put(p("variant"), new PropertyValue.StringValue(variant));
                if ((mask & 2) != 0) values.put(p("isThreeLine"), new PropertyValue.BooleanValue(true));
                tiles.add(tile(values, mask));
            }
            for (PropertyValue enabled : List.of(new PropertyValue.BooleanValue(false),
                    new PropertyValue.BooleanValue(true), new PropertyValue.NullValue())) {
                for (PropertyValue callback : List.of(new PropertyValue.NullValue(), new PropertyValue.StringValue("noop"))) {
                    tiles.add(tile(Map.of(p("variant"), new PropertyValue.StringValue(variant),
                            p("enabled"), enabled, p("onChanged"), callback,
                            p("value"), new PropertyValue.NullValue(), p("tristate"), new PropertyValue.BooleanValue(true)), 7));
                }
            }
        }
        for (String property : List.of("isThreeLine", "dense", "enableFeedback", "enabled")) {
            tiles.add(tile(Map.of(p(property), new PropertyValue.NullValue()), 0));
        }
        for (String property : List.of("horizontalTitleGap", "minVerticalPadding", "minLeadingWidth",
                "minTileHeight", "splashRadius", "checkboxScaleFactor")) {
            for (String member : List.of("infinity", "negativeInfinity", "nan")) {
                tiles.add(tile(Map.of(p(property), new PropertyValue.EnumValue("double", member)), 0));
            }
            tiles.add(tile(Map.of(p(property), new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(-2))), 0));
            if (!property.equals("checkboxScaleFactor")) tiles.add(tile(Map.of(p(property), new PropertyValue.NullValue()), 0));
        }
        validate(List.of(valid("defaults-slots-nonfinite", column(tiles))));
    }

    @Test
    void everyEnumWholeReferencesFactoriesAndDisabledStoredCallbacksKeepSdkSemantics() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (boolean factories : List.of(false, true)) {
            var values = new LinkedHashMap<PropertyName, PropertyValue>();
            values.put(p("variant"), new PropertyValue.StringValue(factories ? "adaptive" : "standard"));
            COLORS.forEach(name -> values.put(p(name), ref("_color")));
            values.put(p("shape"), ref("_shape"));
            values.put(p("checkboxShape"), factories ? factory("_makeCheckboxShape") : ref("_checkboxShape"));
            values.put(p("side"), factories ? factory("_makeSide") : ref("_side"));
            values.put(p("visualDensity"), ref("_density"));
            values.put(p("fillColor"), ref("_stateColor"));
            values.put(p("overlayColor"), factories ? factory("_makeStateColor") : ref("_stateColor"));
            values.put(p("contentPadding"), factories ? factory("_makePadding") : ref("_padding"));
            values.put(p("mouseCursor"), ref("_cursor"));
            values.put(p("focusNode"), factories ? factory("_makeFocusNode") : ref("_focusNode"));
            values.put(p("statesController"), factories ? factory("_makeStatesController") : ref("_statesController"));
            values.put(p("onChanged"), factories ? factory("_makeCallback") : ref("_onChanged"));
            values.put(p("onFocusChange"), ref("_onFocusChange"));
            values.put(p("enabled"), new PropertyValue.BooleanValue(false));
            cases.add(valid("whole-" + factories, tile(values, 7)));
        }
        var tiles = new ArrayList<WidgetNode>();
        for (String affinity : List.of("leading", "trailing", "platform")) {
            for (String alignment : List.of("threeLine", "titleHeight", "top", "center", "bottom")) {
                for (String target : List.of("padded", "shrinkWrap")) {
                    tiles.add(tile(Map.of(p("controlAffinity"), new PropertyValue.EnumValue("ListTileControlAffinity", affinity),
                            p("titleAlignment"), new PropertyValue.EnumValue("ListTileTitleAlignment", alignment),
                            p("materialTapTargetSize"), new PropertyValue.EnumValue("MaterialTapTargetSize", target),
                            p("onFocusChange"), new PropertyValue.StringValue("noop")), 7));
                }
            }
        }
        cases.add(valid("enums-and-controlled-callbacks", column(tiles)));
        validate(cases);
    }

    @Test
    void strictReferencesRejectDynamicNullableAndWrongTypesWithoutChangingUserFiles() throws Exception {
        var cases = new ArrayList<Candidate>();
        var properties = new LinkedHashMap<String, String>();
        properties.put("activeColor", "Color");
        properties.put("shape", "Shape");
        properties.put("checkboxShape", "CheckboxShape");
        properties.put("side", "Side");
        properties.put("fillColor", "StateColor");
        properties.put("visualDensity", "Density");
        properties.put("contentPadding", "Padding");
        properties.put("mouseCursor", "Cursor");
        properties.put("focusNode", "FocusNode");
        properties.put("statesController", "StatesController");
        properties.put("onChanged", "Callback");
        properties.put("onFocusChange", "FocusCallback");
        for (var entry : properties.entrySet()) {
            for (String kind : List.of("dynamic", "nullable")) {
                String symbol = "_" + kind + entry.getValue();
                cases.add(new Candidate(entry.getKey() + "-" + kind,
                        tile(Map.of(p(entry.getKey()), ref(symbol)), 7), symbol, false));
            }
        }
        cases.add(new Candidate("wrong-color-type", tile(Map.of(p("checkColor"), ref("_wrongType")), 7),
                "_wrongType", true));
        cases.add(new Candidate("non-nullable-bool-callback", tile(Map.of(p("onChanged"), ref("_wrongCallback")), 7),
                "_wrongCallback", true));
        validate(cases);
    }

    private record Candidate(String name, WidgetNode root, String badSymbol, boolean compileFailure) { }
    private static Candidate valid(String name, WidgetNode root) { return new Candidate(name, root, "", false); }

    private void validate(List<Candidate> candidates) throws Exception {
        Path executable = configured("dart.executable"), sdk = configured("flutter.sdk"), cache = configured("pub.cache");
        Path project = Files.createDirectories(workspace.resolve("checkbox_list_tile_probe"));
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
                new io.github.vgrytsenko2022.designer.validation.ValidationResult(List.of()), catalog,
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
            assertTrue(build.contains("CheckboxListTile(") || build.contains("CheckboxListTile.adaptive("), build);
            assertFalse(build.contains("_CheckboxListTile"), "No private SDK implementation types");
            if (entry.name().startsWith("whole-")) {
                for (String callback : List.of("_onFocusChange",
                        entry.name().endsWith("true") ? "_makeCallback" : "_onChanged")) {
                    assertTrue(build.contains(callback), "Disabled CheckboxListTile must retain callback presence: " + callback);
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
    private static java.math.BigDecimal number(long value) { return java.math.BigDecimal.valueOf(value); }
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
        var required = new LinkedHashMap<PropertyName, PropertyValue>();
        required.put(p("value"), new PropertyValue.BooleanValue(false));
        required.put(p("onChanged"), new PropertyValue.StringValue("noop"));
        required.put(p("variant"), new PropertyValue.StringValue("standard"));
        required.putAll(values);
        return new WidgetNode(StableId.random(), CHECKBOX_LIST_TILE, required, slots);
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
                const _checkboxShape = RoundedRectangleBorder();
                const _side = BorderSide(width: 2);
                const _stateColor = WidgetStatePropertyAll<Color?>(Color(0xff654321));
                const _padding = EdgeInsetsDirectional.only(start: -3, end: 12);
                final _cursor = SystemMouseCursors.click;
                final _focusNode = FocusNode();
                final _statesController = WidgetStatesController();
                void _onChanged(bool? value) {}
                void _wrongCallback(bool value) {}
                void _onFocusChange(bool focused) {}
                OutlinedBorder _makeCheckboxShape() => throw StateError('Never execute a project factory');
                BorderSide _makeSide() => throw StateError('Never execute a project factory');
                WidgetStateProperty<Color?> _makeStateColor() => throw StateError('Never execute a project factory');
                EdgeInsetsGeometry _makePadding() => throw StateError('Never execute a project factory');
                FocusNode _makeFocusNode() => throw StateError('Never execute a project factory');
                WidgetStatesController _makeStatesController() => throw StateError('Never execute a project factory');
                ValueChanged<bool?> _makeCallback() => throw StateError('Never execute a project factory');
                dynamic _dynamicColor = _color; Color? _nullableColor = _color;
                dynamic _dynamicShape = _shape; ShapeBorder? _nullableShape = _shape;
                dynamic _dynamicDensity = _density; VisualDensity? _nullableDensity = _density;
                dynamic _dynamicCheckboxShape = _checkboxShape; OutlinedBorder? _nullableCheckboxShape = _checkboxShape;
                dynamic _dynamicSide = _side; BorderSide? _nullableSide = _side;
                dynamic _dynamicStateColor = _stateColor; WidgetStateProperty<Color?>? _nullableStateColor = _stateColor;
                dynamic _dynamicPadding = _padding; EdgeInsetsGeometry? _nullablePadding = _padding;
                dynamic _dynamicCursor = _cursor; MouseCursor? _nullableCursor = _cursor;
                dynamic _dynamicFocusNode = _focusNode; FocusNode? _nullableFocusNode = _focusNode;
                dynamic _dynamicStatesController = _statesController; WidgetStatesController? _nullableStatesController = _statesController;
                dynamic _dynamicCallback = _onChanged; ValueChanged<bool?>? _nullableCallback = _onChanged;
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
        addPackage(packages, "checkbox_list_tile_probe", project, "3.10");
        addPackage(packages, "flutter", sdk.resolve("packages/flutter"), "3.10");
        addPackage(packages, "sky_engine", sdk.resolve("bin/cache/pkg/sky_engine"), "3.10");
        for (var dependency : Map.of("characters", "1.4.1", "collection", "1.19.1",
                "material_color_utilities", "0.13.0", "meta", "1.18.0", "vector_math", "2.2.0").entrySet()) {
            addPackage(packages, dependency.getKey(), cache.resolve("hosted/pub.dev")
                    .resolve(dependency.getKey() + "-" + dependency.getValue()), "3.4");
        }
        Path tool = Files.createDirectories(project.resolve(".dart_tool"));
        Files.writeString(tool.resolve("package_config.json"), JSON.writeValueAsString(config), StandardCharsets.UTF_8);
        Files.writeString(project.resolve("pubspec.yaml"), "name: checkbox_list_tile_probe\nenvironment:\n  sdk: ^3.10.0\n"
                + "dependencies:\n  flutter:\n    sdk: flutter\n", StandardCharsets.UTF_8);
    }
    private static void addPackage(ArrayNode packages, String name, Path root, String language) {
        assertTrue(Files.isDirectory(root), "Pinned package missing: " + root);
        packages.addObject().put("name", name).put("rootUri", root.toUri().toString())
                .put("packageUri", "lib/").put("languageVersion", language);
    }
}
