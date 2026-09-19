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
import io.github.vgrytsenko2022.plugin.designer.properties.SwitchPropertyContractTest;
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

/** Switch standard/adaptive generation, state icons/images and strict same-file SDK proof without widening safety budgets. */
class SwitchCandidateRealSdkTest {
    @TempDir Path workspace;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final StableId DOCUMENT_ID = StableId.parse("77000000-0000-4000-8000-000000000001");
    private static final WidgetTypeId SWITCH = new WidgetTypeId("flutter.material.Switch");


    private static final List<String> COLORS = List.of("thumbColor", "trackColor", "trackOutlineColor", "overlayColor");
    private static final List<String> STATES = List.of("Default", "Disabled", "Error", "Dragged", "Pressed", "Selected", "ScrolledUnder", "Hovered", "Focused");

    @Test
    void bothDenseConstructorsPreserveEveryLocalIconFieldAndStateMap() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (String variant : List.of("standard", "adaptive")) {
            var values = SwitchPropertyContractTest.full(variant);
            values.put(p("enabled"), new PropertyValue.BooleanValue(true));
            values.put(p("trackOutlineWidthSelected"), new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(-2)));
            values.put(p("trackOutlineWidthDisabled"), new PropertyValue.NullValue());
            cases.add(new Candidate("dense-" + variant, switchNode(variant, values), ""));
        }
        validate(cases);
    }

    @Test
    void wholeStatePropertiesAndCallbackFactoriesUseStrictPublicTypes() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (String variant : List.of("standard", "adaptive")) for (boolean invoke : List.of(false, true)) {
            var values = new LinkedHashMap<PropertyName, PropertyValue>();
            values.put(p("enabled"), new PropertyValue.BooleanValue(true));
            values.put(p("activeThumbImage"), PropertyValue.ImageProviderValue.asset("assets/thumb.png"));
            values.put(p("inactiveThumbImage"), PropertyValue.ImageProviderValue.exactAsset("assets/thumb.png", BigDecimal.valueOf(2)));
            for (String name : List.of("onChanged", "onFocusChange", "onActiveThumbImageError", "onInactiveThumbImageError",
                    "focusNode", "mouseCursor", "thumbColor", "trackColor", "trackOutlineColor", "overlayColor", "trackOutlineWidth", "thumbIcon")) {
                values.put(p(name), invoke ? factory("_make" + Character.toUpperCase(name.charAt(0)) + name.substring(1)) : reference("_" + name));
            }
            cases.add(new Candidate("whole-" + variant + "-" + invoke, switchNode(variant, values), ""));
        }
        validate(cases);
    }

    @Test
    void iconNumberColorAndCallbackWitnessesRejectWrongGenericsDynamicAndNullableOuterTypes() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (String family : List.of("thumbIcon", "trackOutlineWidth", "thumbColor")) {
            String suffix = Character.toUpperCase(family.charAt(0)) + family.substring(1);
            String type = family.equals("thumbIcon") ? "WidgetStateProperty<Icon?>" : family.equals("trackOutlineWidth")
                    ? "WidgetStateProperty<double?>" : "WidgetStateProperty<Color?>";
            for (String prefix : List.of("", "dynamic", "nullable", "wrong")) {
                String name = prefix.isEmpty() ? "_" + family : "_" + prefix + suffix;
                cases.add(new Candidate(name, switchNode("standard", Map.of(p(family), reference(name))), prefix.isEmpty() ? "" : type));
            }
        }
        for (String family : List.of("onChanged", "onActiveThumbImageError")) {
            String suffix = Character.toUpperCase(family.charAt(0)) + family.substring(1);
            for (String prefix : List.of("dynamic", "nullable", "wrong")) {
                String name = "_" + prefix + suffix;
                var values = new LinkedHashMap<PropertyName, PropertyValue>();
                values.put(p("enabled"), new PropertyValue.BooleanValue(true));
                values.put(p(family), reference(name));
                if (family.equals("onActiveThumbImageError")) values.put(p("activeThumbImage"), PropertyValue.ImageProviderValue.asset("assets/thumb.png"));
                cases.add(new Candidate(name, switchNode("adaptive", values),
                        family.equals("onChanged") ? "ValueChanged<bool>" : "ImageErrorListener"));
            }
        }
        validate(cases);
    }

    @Test
    void explicitNullNumericMapsEmptyIconsAndInactiveCallbacksRemainDistinct() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (String variant : List.of("standard", "adaptive")) {
            var values = new LinkedHashMap<PropertyName, PropertyValue>();
            values.put(p("enabled"), new PropertyValue.BooleanValue(false));
            values.put(p("onChanged"), externalReference("package:not_installed/disabled.dart", "disabledChanged"));
            for (String state : STATES) {
                values.put(p("trackOutlineWidth" + state), new PropertyValue.NullValue());
                for (String family : COLORS) values.put(p(family + state), new PropertyValue.NullValue());
            }
            values.put(p("thumbIconSelectedMode"), new PropertyValue.StringValue("icon"));
            values.put(p("thumbIconSelectedData"), PropertyValue.IconDataValue.none());
            values.put(p("thumbIconDisabledMode"), new PropertyValue.StringValue("inherit"));
            values.put(p("splashRadius"), new PropertyValue.EnumValue("double", "infinity"));
            if (variant.equals("adaptive")) values.put(p("applyCupertinoTheme"), new PropertyValue.NullValue());
            cases.add(new Candidate("null-map-and-disabled-" + variant, switchNode(variant, values), ""));
        }
        validate(cases);
    }

    @Test
    void typedImageProvidersScalesResizePoliciesAndAdaptiveThemeOptionsAnalyze() throws Exception {
        var cases = new ArrayList<Candidate>(); int index = 0;
        for (String variant : List.of("standard", "adaptive")) for (var kind : PropertyValue.ImageProviderValue.ProviderKind.values()) for (boolean resize : List.of(false, true)) {
            var provider = new PropertyValue.ImageProviderValue(kind, "assets/thumb.png",
                    resize ? Optional.of("switch_probe") : Optional.empty(),
                    kind == PropertyValue.ImageProviderValue.ProviderKind.EXACT_ASSET ? Optional.of(new BigDecimal("1.5")) : Optional.empty(),
                    resize ? Optional.of(new PropertyValue.ImageProviderValue.ResizeImageConfig(Optional.of(32), Optional.of(48),
                            kind == PropertyValue.ImageProviderValue.ProviderKind.ASSET ? PropertyValue.ImageProviderValue.ResizePolicy.FIT : PropertyValue.ImageProviderValue.ResizePolicy.EXACT, true)) : Optional.empty());
            var values = new LinkedHashMap<PropertyName, PropertyValue>();
            values.put(p("activeThumbImage"), provider); values.put(p("inactiveThumbImage"), provider);
            values.put(p("onActiveThumbImageError"), reference("_onActiveThumbImageError"));
            values.put(p("onInactiveThumbImageError"), factory("_makeOnInactiveThumbImageError"));
            values.put(p("activeColor"), new PropertyValue.ColorValue(0xff112233L));
            values.put(p("activeThumbColor"), new PropertyValue.ColorValue(0xff445566L));
            values.put(p("activeTrackColor"), new PropertyValue.ColorValue(0xff778899L));
            values.put(p("trackOutlineWidthDefault"), new PropertyValue.DoubleValue(new BigDecimal("-1.5")));
            values.put(p("trackOutlineWidthSelected"), new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(3)));
            values.put(p("splashRadius"), new PropertyValue.DoubleValue(BigDecimal.valueOf(-5)));
            if (variant.equals("adaptive")) values.put(p("applyCupertinoTheme"), new PropertyValue.BooleanValue(resize));
            cases.add(new Candidate("image-" + variant + "-" + index++, switchNode(variant, values), ""));
        }
        validate(cases);
    }

    @Test
    void equalAndMixedInfiniteOutlineWidthsRetainExactSdkSourceTypes() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (String variant : List.of("standard", "adaptive")) for (boolean mixed : List.of(false, true)) {
            var values = new LinkedHashMap<PropertyName, PropertyValue>();
            for (String state : STATES) values.put(p("trackOutlineWidth" + state), new PropertyValue.EnumValue("double", "infinity"));
            if (mixed) {
                values.put(p("trackOutlineWidthSelected"), new PropertyValue.DoubleValue(new BigDecimal("-1.5")));
                values.put(p("trackOutlineWidthDisabled"), new PropertyValue.NullValue());
            }
            cases.add(new Candidate("infinite-width-" + variant + "-" + mixed, switchNode(variant, values), ""));
        }
        validate(cases);
    }

    private record Candidate(String name, WidgetNode root, String rejectedType) { }

    private void validate(List<Candidate> candidates) throws Exception {
        Path executable = configured("dart.executable"), sdk = configured("flutter.sdk"), cache = configured("pub.cache");
        Path project = Files.createDirectories(workspace.resolve("switch_probe"));
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
        byte[] thumbAsset = Files.readAllBytes(project.resolve("assets/thumb.png"));
        byte[] helperThumbAsset = Files.readAllBytes(project.resolve("assets/switch_thumb.png"));
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
            assertTrue(source.contains("Switch"), source);
            assertFalse(source.contains("_SwitchDefaults"), "No private SDK implementation types");
            assertFalse(source.contains("IconButton("), source);
            if (entry.name().startsWith("dense-")) {
                String variant = entry.name().endsWith("-adaptive") ? ".adaptive" : "";
                assertTrue(source.contains("Switch" + variant + "("), entry.name());
                assertTrue(probes.size() > 100, entry.name() + ": preserve all generated occurrences");
            }
            if (entry.name().startsWith("null-map-and-disabled-")) {
                assertFalse(source.contains("package:not_installed/disabled.dart"), source);
                assertFalse(source.contains("disabledChanged"), source);
                assertTrue(source.contains("WidgetStateProperty.fromMap("), "Numeric maps use exact downward SDK inference");
                assertTrue(source.contains("Icon(null"), source);
                assertTrue(source.contains("onChanged: null"), source);
                assertTrue(source.contains("(1.0 / 0.0)"), source);
            }
            if (entry.name().startsWith("infinite-width-")) {
                assertTrue(source.contains("WidgetStateProperty.fromMap("), source);
                assertTrue(source.contains("(1.0 / 0.0)"), source);
                assertFalse(generation.generated().orElseThrow().build().payload().contains("WidgetStateProperty<double"),
                        "Numeric state maps use SDK argument inference");
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
            assertArrayEquals(thumbAsset, Files.readAllBytes(project.resolve("assets/thumb.png")));
            assertArrayEquals(helperThumbAsset, Files.readAllBytes(project.resolve("assets/switch_thumb.png")));
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
    private static WidgetNode switchNode(String variant, Map<PropertyName, PropertyValue> values) {
        var properties = new LinkedHashMap<>(values);
        properties.put(p("variant"), new PropertyValue.StringValue(variant));
        properties.putIfAbsent(p("enabled"), new PropertyValue.BooleanValue(false));
        properties.putIfAbsent(p("value"), new PropertyValue.BooleanValue(false));
        return new WidgetNode(StableId.random(), SWITCH, properties, Map.of());
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
                void _onChanged(bool value) {}
                ValueChanged<bool> _makeOnChanged() => _onChanged;
                void _onFocusChange(bool value) {}
                ValueChanged<bool> _makeOnFocusChange() => _onFocusChange;
                void _onActiveThumbImageError(Object error, StackTrace? stackTrace) {}
                void _onInactiveThumbImageError(Object error, StackTrace? stackTrace) {}
                ImageErrorListener _makeOnActiveThumbImageError() => _onActiveThumbImageError;
                ImageErrorListener _makeOnInactiveThumbImageError() => _onInactiveThumbImageError;
                void _wrongOnChanged(int value) {}
                dynamic _dynamicOnChanged = _onChanged;
                ValueChanged<bool>? _nullableOnChanged = _onChanged;
                void _wrongOnActiveThumbImageError(String error, bool stackTrace) {}
                dynamic _dynamicOnActiveThumbImageError = _onActiveThumbImageError;
                ImageErrorListener? _nullableOnActiveThumbImageError = _onActiveThumbImageError;
                final _focusNode = FocusNode();
                FocusNode _makeFocusNode() => FocusNode();
                final _mouseCursor = WidgetStateMouseCursor.clickable;
                MouseCursor _makeMouseCursor() => _mouseCursor;
                const _thumbIcon = WidgetStatePropertyAll<Icon?>(Icon(Icons.check));
                WidgetStateProperty<Icon?> _makeThumbIcon() => _thumbIcon;
                dynamic _dynamicThumbIcon = _thumbIcon;
                WidgetStateProperty<Icon?>? _nullableThumbIcon = _thumbIcon;
                const _wrongThumbIcon = WidgetStatePropertyAll<Widget?>(Text('Wrong type'));
                const _trackOutlineWidth = WidgetStatePropertyAll<double?>(-2.0);
                WidgetStateProperty<double?> _makeTrackOutlineWidth() => _trackOutlineWidth;
                dynamic _dynamicTrackOutlineWidth = _trackOutlineWidth;
                WidgetStateProperty<double?>? _nullableTrackOutlineWidth = _trackOutlineWidth;
                const _wrongTrackOutlineWidth = WidgetStatePropertyAll<Color?>(Color(0xff112233));
                """);
        for (String family : COLORS) {
            declarations.append("const _").append(family).append(" = WidgetStatePropertyAll<Color?>(Color(0xff123456));\n")
                    .append("WidgetStateProperty<Color?> _make").append(Character.toUpperCase(family.charAt(0))).append(family.substring(1))
                    .append("() => _").append(family).append(";\n");
        }
        declarations.append("""
                dynamic _dynamicThumbColor = _thumbColor;
                WidgetStateProperty<Color?>? _nullableThumbColor = _thumbColor;
                const _wrongThumbColor = WidgetStatePropertyAll<TextStyle?>(TextStyle());
                """);
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
        addPackage(packages, "switch_probe", project, "3.10");
        addPackage(packages, "flutter", sdk.resolve("packages/flutter"), "3.10");
        addPackage(packages, "sky_engine", sdk.resolve("bin/cache/pkg/sky_engine"), "3.10");
        for (var dependency : Map.of("characters", "1.4.1", "collection", "1.19.1",
                "material_color_utilities", "0.13.0", "meta", "1.18.0", "vector_math", "2.2.0").entrySet()) {
            addPackage(packages, dependency.getKey(), cache.resolve("hosted/pub.dev")
                    .resolve(dependency.getKey() + "-" + dependency.getValue()), "3.4");
        }
        Path tool = Files.createDirectories(project.resolve(".dart_tool"));
        Files.writeString(tool.resolve("package_config.json"), JSON.writeValueAsString(config), StandardCharsets.UTF_8);
        Files.writeString(project.resolve("pubspec.yaml"), "name: switch_probe\nenvironment:\n  sdk: ^3.10.0\n"
                + "dependencies:\n  flutter:\n    sdk: flutter\nflutter:\n  uses-material-design: true\n  assets:\n    - assets/\n", StandardCharsets.UTF_8);
        Files.createDirectories(project.resolve("assets"));
        byte[] thumb = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a1S0AAAAASUVORK5CYII=");
        Files.write(project.resolve("assets/thumb.png"), thumb);
        Files.write(project.resolve("assets/switch_thumb.png"), thumb);
    }

    private static void addPackage(ArrayNode packages, String name, Path root, String language) {
        assertTrue(Files.isDirectory(root), "Pinned package missing: " + root);
        packages.addObject().put("name", name).put("rootUri", root.toUri().toString())
                .put("packageUri", "lib/").put("languageVersion", language);
    }
}
