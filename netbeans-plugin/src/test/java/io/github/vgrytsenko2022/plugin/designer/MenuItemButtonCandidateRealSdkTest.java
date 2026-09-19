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
import io.github.vgrytsenko2022.plugin.designer.properties.MenuItemButtonPropertyContractTest;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Complete MenuItemButton styles, shortcuts, SDK proof and immutable pair-save candidates. */
class MenuItemButtonCandidateRealSdkTest {
    @TempDir Path workspace;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final StableId DOCUMENT_ID = StableId.parse("81000000-0000-4000-8000-000000000001");
    private static final WidgetTypeId MENU_ITEM = new WidgetTypeId("flutter.material.MenuItemButton");

    private static final Map<String, String> REFERENCE_TYPES = Map.ofEntries(
            Map.entry("onPressed", "VoidCallback"), Map.entry("onHover", "ValueChanged<bool>"),
            Map.entry("onFocusChange", "ValueChanged<bool>"), Map.entry("focusNode", "FocusNode"),
            Map.entry("statesController", "WidgetStatesController"), Map.entry("shortcut", "MenuSerializableShortcut"),
            Map.entry("style", "ButtonStyle"), Map.entry("styleBackgroundBuilder", "ButtonLayerBuilder"),
            Map.entry("styleForegroundBuilder", "ButtonLayerBuilder"));

    @Test
    void denseStylesBothShortcutBranchesNullOmissionAndDisabledCallbacksKeepExactProofs() throws Exception {
        var cases = new ArrayList<Candidate>();
        cases.add(valid("dense-physical-colors", MenuItemButtonPropertyContractTest.full(false, false)));
        var paints = MenuItemButtonPropertyContractTest.full(true, true);
        for (String name : MenuItemButtonWidgetPropertySchema.shortcutLocalProperties()) paints.remove(p(name));
        paints.put(p("shortcutCharacter"), string("Ж"));
        paints.put(p("shortcutControl"), bool(true)); paints.put(p("shortcutAlt"), bool(true));
        paints.put(p("shortcutMeta"), bool(true)); paints.put(p("shortcutIncludeRepeats"), bool(false));
        cases.add(valid("dense-directional-paints-character", paints));
        cases.add(valid("defaults", base()));
        var disabled = MenuItemButtonPropertyContractTest.full();
        disabled.put(p("enabled"), bool(false)); cases.add(valid("disabled-retained-callbacks", disabled));
        var nulls = base();
        for (String name : List.of("onHover", "onFocusChange", "focusNode", "statesController", "shortcut", "style", "semanticsLabel")) nulls.put(p(name), nil());
        cases.add(valid("null-optionals", nulls));
        var imported = base(); imported.put(p("shortcut"), external("remoteShortcut"));
        cases.add(valid("imported-shortcut", imported));
        var wholeStyle = base(); wholeStyle.put(p("style"), reference("_style"));
        cases.add(valid("whole-style", wholeStyle));
        var unicode = base(); unicode.put(p("semanticsLabel"), string("Меню\n'quoted' 🚀"));
        cases.add(valid("unicode-semantics", unicode));
        validate(cases);
    }

    @Test
    void everyStrictFamilyAcceptsDirectGetterMemberAndFactoryAccessWithoutMixingWholeStyle() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (int access = 0; access < 3; access++) {
            var values = base();
            for (String name : new TreeSet<>(REFERENCE_TYPES.keySet())) {
                if (name.equals("style")) continue;
                values.put(p(name), access == 0 ? reference("_" + name)
                        : access == 1 ? factory("_make" + upper(name)) : member("References", name));
            }
            cases.add(valid("strict-local-families-" + access, values));
            var whole = base();
            whole.put(p("style"), access == 0 ? reference("_style")
                    : access == 1 ? factory("_makeStyle") : member("References", "style"));
            cases.add(valid("strict-whole-style-" + access, whole));
        }
        validate(cases);
    }

    @Test
    void allWrongDynamicAndNullableTypedReferencesFailClosedWithoutMutatingThePair() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (String family : new TreeSet<>(REFERENCE_TYPES.keySet())) {
            for (String kind : List.of("dynamic", "nullable", "wrong")) {
                var values = base();
                String symbol = "_" + kind + upper(family);
                values.put(p(family), reference(symbol));
                cases.add(kind.equals("wrong") ? compileFailure(kind + "-" + family, values, symbol)
                        : proofFailure(kind + "-" + family, values, symbol));
            }
        }
        assertEquals(27, cases.size());
        validate(cases);
    }

    @Test
    void everyPinnedAllowedLogicalKeyCompilesWithServicesNavigationAndNoTrustExpansion() throws Exception {
        var cases = new ArrayList<Candidate>();
        var keys = MenuShortcutKeyCatalog.names();
        for (int offset = 0; offset < keys.size(); offset += 72) {
            var nodes = new ArrayList<WidgetNode>();
            for (String key : keys.subList(offset, Math.min(offset + 72, keys.size()))) {
                var values = base();
                values.put(p("shortcutTrigger"), new PropertyValue.EnumValue("LogicalKeyboardKey", key));
                nodes.add(tile(values, 0));
            }
            cases.add(new Candidate("key-batch-" + offset, base(), "", false, 0, column(nodes)));
        }
        assertEquals(6, cases.size());
        validate(cases);
    }

    @Test
    void allOptionalSlotsModesFlagsLocksAndCharacterStringsCompileInOneBoundedMatrix() throws Exception {
        var nodes = new ArrayList<WidgetNode>();
        for (int mask = 0; mask < 8; mask++) nodes.add(tile(base(), mask));
        for (String name : List.of("enabled", "autofocus", "requestFocusOnHover", "closeOnActivate")) {
            for (boolean flag : List.of(false, true)) {
                var values = base(); values.put(p(name), bool(flag)); nodes.add(tile(values, 7));
            }
        }
        for (String clip : List.of("none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer")) {
            var values = base(); values.put(p("clipBehavior"), new PropertyValue.EnumValue("Clip", clip)); nodes.add(tile(values, 7));
        }
        for (String axis : List.of("horizontal", "vertical")) {
            var values = base(); values.put(p("overflowAxis"), new PropertyValue.EnumValue("Axis", axis)); nodes.add(tile(values, 7));
        }
        for (String lock : List.of("ignored", "locked", "unlocked")) {
            for (boolean repeats : List.of(false, true)) {
                var values = base(); values.put(p("shortcutTrigger"), new PropertyValue.EnumValue("LogicalKeyboardKey", "numpad1"));
                values.put(p("shortcutNumLock"), new PropertyValue.EnumValue("LockState", lock));
                values.put(p("shortcutIncludeRepeats"), bool(repeats)); nodes.add(tile(values, 1));
            }
        }
        for (String character : List.of("", "a", "Ж", "😀", "e\u0301", "two chars", "'", "\n")) {
            var values = base(); values.put(p("shortcutCharacter"), string(character)); nodes.add(tile(values, 1));
        }
        assertEquals(36, nodes.size());
        validate(List.of(new Candidate("36-menu-sdk-matrix", base(), "", false, 0, column(nodes))));
    }

    private static WidgetNode column(List<WidgetNode> nodes) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(nodes)));
    }

    private static WidgetNode tile(Map<PropertyName, PropertyValue> values, int mask) {
        return new WidgetNode(StableId.random(), MENU_ITEM, values, slots(mask));
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
        Path project = Files.createDirectories(workspace.resolve("menu_item_probe"));
        Path lib = Files.createDirectories(project.resolve("lib"));
        configure(project, sdk, cache);
        Files.writeString(lib.resolve("types.dart"), """
                import 'package:flutter/material.dart';
                const MenuSerializableShortcut remoteShortcut = CharacterActivator("Ж", control: true);
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
            var node = entry.suppliedRoot() == null ? new WidgetNode(StableId.random(), MENU_ITEM, entry.values(), slots(entry.slotMask())) : entry.suppliedRoot();
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
            assertTrue(source.contains("MenuItemButton("), source);
            assertFalse(source.contains("onLongPress:"), source);
            assertFalse(source.contains("TextButtonTheme"), source);
            assertFalse(source.contains("TextButton("), source);
            if (entry.name().startsWith("dense-")) {
                assertTrue(probes.size() > 750, "Retain complete dense style proofs");
                assertTrue(source.contains("MenuButtonTheme"), source);
            }
            if (entry.name().startsWith("key-batch-")) {
                assertTrue(probes.stream().anyMatch(probe -> probe.expectedLibraryUri().equals("package:flutter/services.dart")));
            }
            if (entry.name().equals("defaults")) {
                String build = generation.generated().orElseThrow().build().payload();
                assertTrue(build.contains("onPressed: () {}"), build);
                assertFalse(build.contains("shortcut:"), build);
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
        int bit = 1;
        for (String name : List.of("child", "leadingIcon", "trailingIcon")) {
            slots.put(new SlotName(name), (mask & bit) == 0 ? WidgetSlot.SingleSlot.empty() : WidgetSlot.SingleSlot.of(text("Menu " + name)));
            bit <<= 1;
        }
        return slots;
    }

    private static Map<PropertyName, PropertyValue> base() {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        values.put(p("enabled"), bool(true));
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
        return new PropertyValue.DartObjectReferenceValue(Optional.of("package:menu_item_probe/types.dart"), name, Optional.empty(),
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
                void _onPressed() {}
                void _onHover(bool value) {}
                void _onFocusChange(bool value) {}
                final _focusNode = FocusNode();
                final _statesController = WidgetStatesController();
                const MenuSerializableShortcut _shortcut = CharacterActivator('k', control: true);
                const _style = ButtonStyle();
                Widget _styleBackgroundBuilder(BuildContext context, Set<WidgetState> states, Widget? child) => child ?? const SizedBox.shrink();
                Widget _styleForegroundBuilder(BuildContext context, Set<WidgetState> states, Widget? child) => child ?? const SizedBox.shrink();
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
        addPackage(packages, "menu_item_probe", project, "3.10");
        addPackage(packages, "flutter", sdk.resolve("packages/flutter"), "3.10");
        addPackage(packages, "sky_engine", sdk.resolve("bin/cache/pkg/sky_engine"), "3.10");
        for (var dependency : Map.of("characters", "1.4.1", "collection", "1.19.1",
                "material_color_utilities", "0.13.0", "meta", "1.18.0", "vector_math", "2.2.0").entrySet()) {
            addPackage(packages, dependency.getKey(), cache.resolve("hosted/pub.dev")
                    .resolve(dependency.getKey() + "-" + dependency.getValue()), "3.4");
        }
        Path tool = Files.createDirectories(project.resolve(".dart_tool"));
        Files.writeString(tool.resolve("package_config.json"), JSON.writeValueAsString(config), StandardCharsets.UTF_8);
        Files.writeString(project.resolve("pubspec.yaml"), "name: menu_item_probe\nenvironment:\n  sdk: ^3.10.0\n"
                + "dependencies:\n  flutter:\n    sdk: flutter\nflutter:\n  uses-material-design: true\n", StandardCharsets.UTF_8);
    }

    private static void addPackage(ArrayNode packages, String name, Path root, String language) {
        assertTrue(Files.isDirectory(root), "Pinned package missing: " + root);
        packages.addObject().put("name", name).put("rootUri", root.toUri().toString())
                .put("packageUri", "lib/").put("languageVersion", language);
    }
}
