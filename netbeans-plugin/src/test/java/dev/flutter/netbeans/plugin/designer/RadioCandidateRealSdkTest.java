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
import dev.flutter.netbeans.plugin.designer.properties.RadioPropertyContractTest;
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

/** Real SDK proofs for explicit Radio<T>, nullable values and invariant registry consumption. */
class RadioCandidateRealSdkTest {
    @TempDir Path workspace;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final StableId DOCUMENT_ID = StableId.parse("78000000-0000-4000-8000-000000000001");
    private static final WidgetTypeId RADIO = new WidgetTypeId("flutter.material.Radio");
    private static final List<String> STATES = List.of("Default", "Disabled", "Error", "Dragged",
            "Pressed", "Selected", "ScrolledUnder", "Hovered", "Focused");

    @Test
    void denseStandardAndAdaptiveConstructorsCoverEveryLocalStateFamily() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (String variant : List.of("standard", "adaptive")) {
            var values = new LinkedHashMap<>(RadioPropertyContractTest.full());
            values.put(p("variant"), string(variant));
            if (variant.equals("standard")) values.remove(p("useCupertinoCheckmarkStyle"));
            cases.add(valid("dense-" + variant, values));
        }
        validate(cases);
    }

    @Test
    void explicitBuiltinTypesPreserveNullableGroupAndValueReferences() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (String type : List.of("String", "int", "double", "num", "bool", "Object")) {
            for (boolean nullable : List.of(false, true)) {
                var values = base();
                values.put(p("valueType"), string(type));
                values.put(p("nullableValueType"), bool(nullable));
                values.put(p("value"), reference("_" + type + (nullable ? "Nullable" : "Value")));
                values.put(p("groupValue"), reference("_" + type + "Nullable"));
                values.put(p("onChanged"), reference("_" + type + "Changed"));
                cases.add(valid("builtin-" + type + "-" + nullable, values));
            }
        }
        validate(cases);
    }

    @Test
    void namedEnumsClassesTypedefsAndImportedTypesPreserveTheirExactGenericIdentity() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (boolean nullable : List.of(false, true)) {
            var values = base();
            values.put(p("valueType"), reference("Choice"));
            values.put(p("nullableValueType"), bool(nullable));
            values.put(p("value"), nullable ? reference("_nullableChoice") : member("Choice", "first"));
            values.put(p("groupValue"), reference("_nullableChoice"));
            values.put(p("onChanged"), reference("_choiceChanged"));
            values.put(p("groupRegistry"), reference(nullable ? "_nullableChoiceRegistry" : "_choiceRegistry"));
            cases.add(valid("enum-" + nullable, values));
        }
        var imported = base();
        imported.put(p("valueType"), external("RemoteChoice"));
        imported.put(p("value"), externalMember("RemoteChoice", "second"));
        imported.put(p("groupValue"), external("remoteChoice"));
        imported.put(p("onChanged"), external("remoteChanged"));
        imported.put(p("groupRegistry"), external("remoteRegistry"));
        cases.add(valid("imported-enum", imported));
        for (String type : List.of("Selection", "SelectionList")) {
            var values = base();
            values.put(p("valueType"), reference(type));
            values.put(p("value"), reference("_" + type + "Value"));
            values.put(p("groupValue"), reference("_" + type + "Nullable"));
            values.put(p("onChanged"), reference("_" + type + "Changed"));
            values.put(p("groupRegistry"), reference("_" + type + "Registry"));
            cases.add(valid("named-" + type, values));
        }
        validate(cases);
    }

    @Test
    void nullableDynamicAndCovariantRegistryMismatchesFailBeforeSave() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (String type : List.of("String", "Object")) {
            for (String field : List.of("value", "groupValue")) {
                var values = base();
                values.put(p("valueType"), string(type));
                values.put(p("nullableValueType"), bool(true));
                values.put(p("value"), nil());
                values.put(p(field), reference("_dynamicValue"));
                cases.add(proofFailure("dynamic-" + type + "-" + field, values, "_dynamicValue"));
            }
        }
        var callback = base();
        callback.put(p("onChanged"), reference("_dynamicChanged"));
        cases.add(proofFailure("dynamic-callback", callback, "_dynamicChanged"));
        var nullableCallback = base();
        nullableCallback.put(p("onChanged"), reference("_nullableChanged"));
        cases.add(proofFailure("nullable-callback", nullableCallback, "_nullableChanged"));
        var registry = base();
        registry.put(p("groupRegistry"), reference("_dynamicRegistry"));
        cases.add(proofFailure("dynamic-registry", registry, "_dynamicRegistry"));
        var nullableRegistry = base();
        nullableRegistry.put(p("groupRegistry"), reference("_nullableRegistry"));
        cases.add(proofFailure("nullable-outer-registry", nullableRegistry, "_nullableRegistry"));
        var covariance = base();
        covariance.put(p("valueType"), string("Object"));
        covariance.put(p("value"), integer(1));
        covariance.put(p("groupRegistry"), reference("_stringRegistry"));
        cases.add(proofFailure("covariant-registry-is-not-consumer-safe", covariance, "_stringRegistry"));
        var wrongCallback = base();
        wrongCallback.put(p("onChanged"), reference("_intChanged"));
        cases.add(compileFailure("wrong-callback", wrongCallback, "_intChanged"));
        var wrongValue = base();
        wrongValue.put(p("value"), reference("_intValue"));
        cases.add(compileFailure("wrong-value", wrongValue, "_intValue"));
        var wrongType = base();
        wrongType.put(p("valueType"), reference("_notAType"));
        cases.add(compileFailure("value-is-not-type", wrongType, "_notAType"));
        validate(cases);
    }

    @Test
    void nullableEnablementCallbackOmissionAndWholeReferenceFactoriesRemainDistinct() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (PropertyValue enabled : List.of(nil(), bool(false), bool(true))) {
            var values = base();
            values.put(p("enabled"), enabled);
            values.put(p("onChanged"), reference("_StringChanged"));
            cases.add(valid("explicit-enabled-" + enabled, values));
        }
        var omitted = base();
        omitted.remove(p("onChanged"));
        cases.add(valid("callback-omitted", omitted));
        var nullCallback = base();
        nullCallback.put(p("onChanged"), nil());
        nullCallback.put(p("enabled"), nil());
        cases.add(valid("callback-null", nullCallback));
        for (boolean invoke : List.of(false, true)) {
            var values = base();
            for (String name : List.of("fillColor", "overlayColor", "backgroundColor", "innerRadius",
                    "side", "focusNode", "mouseCursor", "visualDensity")) {
                values.put(p(name), invoke ? factory("_make" + upper(name)) : reference("_" + name));
            }
            values.put(p("groupValue"), factory("_makeNullableString"));
            cases.add(valid("whole-references-" + invoke, values));
        }
        validate(cases);
    }

    @Test
    void allStateNullsAndSignedInfiniteRadiiKeepExactSource() throws Exception {
        var cases = new ArrayList<Candidate>();
        var nulls = base();
        for (String state : STATES) {
            for (String family : List.of("fillColor", "overlayColor", "backgroundColor", "innerRadius")) {
                nulls.put(p(family + state), nil());
            }
        }
        cases.add(valid("all-state-null", nulls));
        for (String member : List.of("infinity", "negativeInfinity")) {
            var values = base();
            values.put(p("splashRadius"), new PropertyValue.EnumValue("double", member));
            for (String state : STATES) values.put(p("innerRadius" + state), new PropertyValue.EnumValue("double", member));
            cases.add(valid("radii-" + member, values));
        }
        var extreme = base();
        extreme.put(p("splashRadius"), new PropertyValue.DoubleValue(new BigDecimal("-1e308")));
        extreme.put(p("innerRadiusSelected"), new PropertyValue.DoubleValue(new BigDecimal("1e308")));
        cases.add(valid("signed-finite-extremes", extreme));
        for (String type : List.of("double", "num", "Object")) {
            for (String member : List.of("infinity", "negativeInfinity", "nan")) {
                var values = base();
                values.put(p("valueType"), string(type));
                values.put(p("value"), new PropertyValue.EnumValue("double", member));
                values.put(p("groupValue"), new PropertyValue.EnumValue("double", member));
                cases.add(valid("identity-" + type + "-" + member, values));
            }
        }
        validate(cases);
    }

    @Test
    void allNonGenericReferenceFamiliesRetainStrictWrongDynamicAndNullableRejection() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (String family : List.of("fillColor", "overlayColor", "backgroundColor", "innerRadius",
                "side", "focusNode", "mouseCursor", "visualDensity")) {
            for (String prefix : List.of("dynamic", "nullable", "wrong")) {
                var values = base();
                String symbol = "_" + prefix + upper(family);
                values.put(p(family), reference(symbol));
                cases.add(prefix.equals("wrong")
                        ? compileFailure("wrong-" + family, values, symbol)
                        : proofFailure(prefix + "-" + family, values, symbol));
            }
        }
        validate(cases);
    }

    @Test
    void contextualGenericFactoriesAndDynamicTypeAliasesRespectTheStrictProofBoundary() throws Exception {
        var cases = new ArrayList<Candidate>();
        var stringValue = base();
        stringValue.put(p("value"), factory("_genericValue"));
        cases.add(valid("generic-nonnullable-string", stringValue));
        var stringGroup = base();
        stringGroup.put(p("groupValue"), factory("_genericValue"));
        cases.add(valid("generic-nullable-core-group", stringGroup));
        var choiceValue = base();
        choiceValue.put(p("valueType"), reference("Choice"));
        choiceValue.put(p("value"), factory("_genericValue"));
        choiceValue.put(p("onChanged"), reference("_choiceChanged"));
        cases.add(valid("generic-nonnullable-choice", choiceValue));
        var broadGroup = base();
        broadGroup.put(p("valueType"), string("Object"));
        broadGroup.put(p("groupValue"), factory("_genericValue"));
        cases.add(proofFailure("generic-broad-nullable-group-needs-concrete-return-type", broadGroup, "_genericValue"));
        var dynamicType = base();
        dynamicType.put(p("valueType"), reference("AnyChoice"));
        dynamicType.put(p("nullableValueType"), bool(true));
        dynamicType.put(p("value"), nil());
        cases.add(proofFailure("typedef-dynamic-with-literal-null-is-rejected", dynamicType, "AnyChoice"));
        validate(cases);
    }

    private record Candidate(String name, Map<PropertyName, PropertyValue> values, String badSymbol, boolean compileFailure) { }
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
        Path project = Files.createDirectories(workspace.resolve("radio_probe"));
        Path lib = Files.createDirectories(project.resolve("lib"));
        configure(project, sdk, cache);
        Files.writeString(lib.resolve("types.dart"), """
                import 'package:flutter/material.dart';
                enum RemoteChoice { first, second }
                RemoteChoice? remoteChoice;
                void remoteChanged(RemoteChoice? value) {}
                class RemoteRegistry extends RadioGroupRegistry<RemoteChoice> {
                  @override RemoteChoice? get groupValue => remoteChoice;
                  @override ValueChanged<RemoteChoice?> get onChanged => remoteChanged;
                  @override void registerClient(RadioClient<RemoteChoice> radio) {}
                  @override void unregisterClient(RadioClient<RemoteChoice> radio) {}
                }
                final remoteRegistry = RemoteRegistry();
                """, StandardCharsets.UTF_8);
        Path file = lib.resolve("home_page.dart");
        var codec = new FdDocumentCodec();
        var generator = new DartRegionGenerator();
        var catalog = BuiltInWidgetCatalog.getDefault();
        var seed = descriptor("", "");
        var baselineGenerated = generator.generate(document(seed, text("Seed")), catalog).generated().orElseThrow();
        var baselineDescriptor = descriptor(baselineGenerated.imports().payload(), baselineGenerated.build().payload());
        var baselineDocument = document(baselineDescriptor, text("Seed"));
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
                new dev.flutter.netbeans.designer.validation.ValidationResult(List.of()), catalog,
                List.of(), List.of(), Optional.of(baseline.source()), Optional.of(baseline));
        var analyzer = new DartCandidateAnalyzer(executable, ignored -> { });
        assertEquals(45, DartCandidateAnalysisLimits.DEFAULT.totalTimeout().toSeconds());
        for (var entry : candidates) {
            var node = new WidgetNode(StableId.random(), RADIO, entry.values(), Map.of());
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
            assertTrue(source.contains("Radio<"), source);
            assertFalse(source.contains("_RadioDefaults"), "No private SDK implementation types");
            if (entry.name().equals("callback-omitted")) {
                String build = generation.generated().orElseThrow().build().payload();
                assertFalse(build.contains("onChanged:"), build);
                assertFalse(build.contains("enabled:"), build);
            }
            if (entry.name().equals("callback-null")) {
                assertTrue(source.contains("onChanged: null"), source);
                assertTrue(source.contains("enabled: null"), source);
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

    private static Map<PropertyName, PropertyValue> base() {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        values.put(p("variant"), string("standard"));
        values.put(p("valueType"), string("String"));
        values.put(p("value"), string("option"));
        values.put(p("onChanged"), string("noop"));
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
        return new PropertyValue.DartObjectReferenceValue(Optional.of("package:radio_probe/types.dart"), name, Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }
    private static PropertyValue.DartObjectReferenceValue externalMember(String name, String member) {
        return new PropertyValue.DartObjectReferenceValue(Optional.of("package:radio_probe/types.dart"), name, Optional.of(member),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }
    private static WidgetNode text(String value) {
        return new WidgetNode(StableId.parse("78000000-0000-4000-8000-000000000002"),
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
                enum Choice { first, second }
                typedef AnyChoice = dynamic;
                T _genericValue<T>() => throw StateError('Not executed by the Designer');
                Choice? _nullableChoice;
                void _choiceChanged(Choice? value) {}
                class Selection { const Selection(); }
                typedef SelectionList = List<String>;
                const Selection _SelectionValue = Selection();
                Selection? _SelectionNullable;
                void _SelectionChanged(Selection? value) {}
                final SelectionList _SelectionListValue = <String>['first'];
                SelectionList? _SelectionListNullable;
                void _SelectionListChanged(SelectionList? value) {}
                class TestRegistry<T> extends RadioGroupRegistry<T> {
                  TestRegistry(this.groupValue, this.onChanged);
                  @override final T? groupValue;
                  @override final ValueChanged<T?> onChanged;
                  @override void registerClient(RadioClient<T> radio) {}
                  @override void unregisterClient(RadioClient<T> radio) {}
                }
                final _choiceRegistry = TestRegistry<Choice>(Choice.first, _choiceChanged);
                final _nullableChoiceRegistry = TestRegistry<Choice?>(null, _choiceChanged);
                final _SelectionRegistry = TestRegistry<Selection>(null, _SelectionChanged);
                final _SelectionListRegistry = TestRegistry<SelectionList>(null, _SelectionListChanged);
                final _stringRegistry = TestRegistry<String>(null, _StringChanged);
                final _groupRegistry = TestRegistry<String?>(null, _StringChanged);
                void _onChanged(String? value) {}
                dynamic _dynamicRegistry = _stringRegistry;
                RadioGroupRegistry<String>? _nullableRegistry = _stringRegistry;
                dynamic _dynamicValue = 'option';
                dynamic _dynamicChanged = _StringChanged;
                ValueChanged<String?>? _nullableChanged = _StringChanged;
                const _notAType = 1;
                String? _makeNullableString() => null;
                const _fillColor = WidgetStatePropertyAll<Color?>(Color(0xff123456));
                const _overlayColor = WidgetStatePropertyAll<Color?>(Color(0xff345678));
                const _backgroundColor = WidgetStatePropertyAll<Color?>(Color(0xff567890));
                const _innerRadius = WidgetStatePropertyAll<double?>(-2);
                const _side = BorderSide(width: 2);
                final _focusNode = FocusNode();
                final _mouseCursor = WidgetStateMouseCursor.clickable;
                const _visualDensity = VisualDensity(horizontal: 1);
                WidgetStateProperty<Color?> _makeFillColor() => _fillColor;
                WidgetStateProperty<Color?> _makeOverlayColor() => _overlayColor;
                WidgetStateProperty<Color?> _makeBackgroundColor() => _backgroundColor;
                WidgetStateProperty<double?> _makeInnerRadius() => _innerRadius;
                BorderSide _makeSide() => _side;
                FocusNode _makeFocusNode() => _focusNode;
                MouseCursor _makeMouseCursor() => _mouseCursor;
                VisualDensity _makeVisualDensity() => _visualDensity;
                """);
        var literals = Map.of("String", "'option'", "int", "1", "double", "1.0",
                "num", "2.5", "bool", "true", "Object", "'object'");
        for (String type : List.of("String", "int", "double", "num", "bool", "Object")) {
            declarations.append(type).append(" _").append(type).append("Value = ").append(literals.get(type)).append(";\n")
                    .append(type).append("? _").append(type).append("Nullable;\n")
                    .append("void _").append(type).append("Changed(").append(type).append("? value) {}\n");
        }
        var referenceTypes = Map.of("fillColor", "WidgetStateProperty<Color?>", "overlayColor", "WidgetStateProperty<Color?>",
                "backgroundColor", "WidgetStateProperty<Color?>", "innerRadius", "WidgetStateProperty<double?>",
                "side", "BorderSide", "focusNode", "FocusNode", "mouseCursor", "MouseCursor", "visualDensity", "VisualDensity");
        referenceTypes.forEach((family, type) -> {
            declarations.append("dynamic _dynamic").append(upper(family)).append(" = _").append(family).append(";\n")
                    .append(type).append("? _nullable").append(upper(family)).append(" = _").append(family).append(";\n")
                    .append("const _wrong").append(upper(family)).append(" = 1;\n");
        });
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
        addPackage(packages, "radio_probe", project, "3.10");
        addPackage(packages, "flutter", sdk.resolve("packages/flutter"), "3.10");
        addPackage(packages, "sky_engine", sdk.resolve("bin/cache/pkg/sky_engine"), "3.10");
        for (var dependency : Map.of("characters", "1.4.1", "collection", "1.19.1",
                "material_color_utilities", "0.13.0", "meta", "1.18.0", "vector_math", "2.2.0").entrySet()) {
            addPackage(packages, dependency.getKey(), cache.resolve("hosted/pub.dev")
                    .resolve(dependency.getKey() + "-" + dependency.getValue()), "3.4");
        }
        Path tool = Files.createDirectories(project.resolve(".dart_tool"));
        Files.writeString(tool.resolve("package_config.json"), JSON.writeValueAsString(config), StandardCharsets.UTF_8);
        Files.writeString(project.resolve("pubspec.yaml"), "name: radio_probe\nenvironment:\n  sdk: ^3.10.0\n"
                + "dependencies:\n  flutter:\n    sdk: flutter\nflutter:\n  uses-material-design: true\n", StandardCharsets.UTF_8);
    }

    private static void addPackage(ArrayNode packages, String name, Path root, String language) {
        assertTrue(Files.isDirectory(root), "Pinned package missing: " + root);
        packages.addObject().put("name", name).put("rootUri", root.toUri().toString())
                .put("packageUri", "lib/").put("languageVersion", language);
    }
}
