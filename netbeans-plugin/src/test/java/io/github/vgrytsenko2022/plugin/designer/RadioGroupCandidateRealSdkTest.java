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

/** Real SDK proofs for explicit RadioGroup<T>, nullable selection and required strict callbacks. */
class RadioGroupCandidateRealSdkTest {
    @TempDir Path workspace;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final StableId DOCUMENT_ID = StableId.parse("78000000-0000-4000-8000-000000000001");
    private static final WidgetTypeId RADIO = new WidgetTypeId("flutter.widgets.RadioGroup");


    @Test
    void allBuiltinTypesAndTheirNullableFormsKeepExactSelectionAndCallbackTypes() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (String type : List.of("String", "int", "double", "num", "bool", "Object")) {
            for (boolean nullable : List.of(false, true)) {
                var values = base();
                values.put(p("valueType"), string(type));
                values.put(p("nullableValueType"), bool(nullable));
                values.put(p("groupValue"), reference("_" + type + "Nullable"));
                values.put(p("onChanged"), reference("_" + type + "Changed"));
                cases.add(valid("builtin-" + type + "-" + nullable, values));
            }
        }
        validate(cases);
    }

    @Test
    void everySelectionKindAndOptionalNullRemainDistinctFromOmission() throws Exception {
        var cases = new ArrayList<Candidate>();
        cases.add(valid("omitted-group-value", base()));
        var nil = base(); nil.put(p("groupValue"), nil()); cases.add(valid("explicit-null-group", nil));
        for (var entry : Map.of("String", string("selected"), "int", integer(7),
                "double", new PropertyValue.DoubleValue(new BigDecimal("1.25")),
                "bool", bool(false), "Object", string("identity")).entrySet()) {
            var values = base(); values.put(p("valueType"), string(entry.getKey()));
            values.put(p("groupValue"), entry.getValue());
            cases.add(valid("literal-" + entry.getKey(), values));
        }
        for (String type : List.of("double", "num", "Object")) {
            for (String member : List.of("infinity", "negativeInfinity", "nan")) {
                var values = base(); values.put(p("valueType"), string(type));
                values.put(p("groupValue"), new PropertyValue.EnumValue("double", member));
                cases.add(valid("nonfinite-" + type + "-" + member, values));
            }
        }
        validate(cases);
    }

    @Test
    void namedEnumsClassesTypedefsAndImportedTypesRetainExactIdentity() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (boolean nullable : List.of(false, true)) {
            var values = base(); values.put(p("valueType"), reference("Choice"));
            values.put(p("nullableValueType"), bool(nullable));
            values.put(p("groupValue"), nullable ? reference("_nullableChoice") : member("Choice", "first"));
            values.put(p("onChanged"), reference("_choiceChanged"));
            cases.add(valid("enum-" + nullable, values));
        }
        for (String type : List.of("Selection", "SelectionList")) {
            var values = base(); values.put(p("valueType"), reference(type));
            values.put(p("groupValue"), reference("_" + type + "Nullable"));
            values.put(p("onChanged"), reference("_" + type + "Changed"));
            cases.add(valid("named-" + type, values));
        }
        var imported = base(); imported.put(p("valueType"), external("RemoteChoice"));
        imported.put(p("groupValue"), externalMember("RemoteChoice", "second"));
        imported.put(p("onChanged"), external("remoteChanged")); cases.add(valid("imported-enum", imported));
        var nullableAlias = base(); nullableAlias.put(p("valueType"), reference("NullableChoice"));
        nullableAlias.put(p("nullableValueType"), bool(true));
        nullableAlias.put(p("groupValue"), reference("_nullableChoice"));
        nullableAlias.put(p("onChanged"), reference("_choiceChanged"));
        cases.add(valid("nullable-typedef-explicitly-enabled", nullableAlias));
        validate(cases);
    }

    @Test
    void wholeFactoriesAndControlledNoopNeverExecuteProjectCode() throws Exception {
        var cases = new ArrayList<Candidate>();
        var concrete = base(); concrete.put(p("groupValue"), factory("_makeNullableString"));
        concrete.put(p("onChanged"), factory("_makeChanged")); cases.add(valid("concrete-factories", concrete));
        var contextual = base(); contextual.put(p("groupValue"), factory("_genericValue"));
        cases.add(valid("contextual-nullable-string-factory", contextual));
        var broad = base(); broad.put(p("valueType"), string("Object"));
        broad.put(p("groupValue"), factory("_genericValue"));
        cases.add(proofFailure("broad-factory-needs-concrete-return-type", broad, "_genericValue"));
        validate(cases);
    }

    @Test
    void wrongDynamicNullableAndNonTypeReferencesCannotAcquirePairSaveAuthority() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (String type : List.of("String", "Object")) {
            var values = base(); values.put(p("valueType"), string(type));
            values.put(p("nullableValueType"), bool(true)); values.put(p("groupValue"), reference("_dynamicValue"));
            cases.add(proofFailure("dynamic-selection-" + type, values, "_dynamicValue"));
        }
        for (String name : List.of("_dynamicChanged", "_nullableChanged")) {
            var values = base(); values.put(p("onChanged"), reference(name));
            cases.add(name.equals("_nullableChanged")
                    ? compileFailure("strict-callback-" + name, values, name)
                    : proofFailure("strict-callback-" + name, values, name));
        }
        var wrongCallback = base(); wrongCallback.put(p("onChanged"), reference("_intChanged"));
        cases.add(compileFailure("wrong-callback", wrongCallback, "_intChanged"));
        var wrongValue = base(); wrongValue.put(p("groupValue"), reference("_intValue"));
        cases.add(compileFailure("wrong-selection", wrongValue, "_intValue"));
        var wrongType = base(); wrongType.put(p("valueType"), reference("_notAType"));
        cases.add(compileFailure("value-is-not-type", wrongType, "_notAType"));
        var dynamicType = base(); dynamicType.put(p("valueType"), reference("AnyChoice"));
        dynamicType.put(p("nullableValueType"), bool(true)); dynamicType.put(p("groupValue"), nil());
        cases.add(proofFailure("dynamic-alias-even-with-null-selection", dynamicType, "AnyChoice"));
        var nullableAlias = base(); nullableAlias.put(p("valueType"), reference("NullableChoice"));
        nullableAlias.put(p("groupValue"), reference("_nullableChoice"));
        nullableAlias.put(p("onChanged"), reference("_choiceChanged"));
        cases.add(proofFailure("nullable-alias-needs-explicit-flag", nullableAlias, "NullableChoice"));
        validate(cases);
    }

    @Test
    void requiredCallbackTypeAndChildRejectBeforeAnyCandidateAnalysis() {
        var catalog = BuiltInWidgetCatalog.getDefault(); var generator = new DartRegionGenerator();
        for (String required : List.of("valueType", "onChanged")) {
            var omitted = base(); omitted.remove(p(required));
            var nil = base(); nil.put(p(required), nil());
            for (var values : List.of(omitted, nil)) {
                assertTrue(generator.generate(document(descriptor("", ""), group(values)), catalog).generated().isEmpty());
            }
        }
        assertTrue(generator.generate(document(descriptor("", ""),
                new WidgetNode(StableId.random(), RADIO, base(), Map.of())), catalog).generated().isEmpty());
    }

    @Test
    void mixedRadioAndNestedGroupHeadersBindTheirOwnExactEvidenceFamilies() throws Exception {
        var values = base(); values.put(p("groupValue"), string("option"));
        validate(List.of(valid("mixed-nested-radio-groups", values)));
    }

    private static WidgetNode mixedGroup(Map<PropertyName, PropertyValue> values) {
        var radioType = new WidgetTypeId("flutter.material.Radio");
        var first = new WidgetNode(StableId.random(), radioType,
                Map.of(p("variant"), string("standard"), p("valueType"), string("String"),
                        p("value"), string("option")), Map.of());
        var second = new WidgetNode(StableId.random(), radioType,
                Map.of(p("variant"), string("standard"), p("valueType"), string("int"),
                        p("value"), integer(1)), Map.of());
        var nestedValues = base(); nestedValues.put(p("valueType"), string("int"));
        nestedValues.put(p("groupValue"), integer(1));
        var nested = new WidgetNode(StableId.random(), RADIO, nestedValues,
                Map.of(new SlotName("child"), new WidgetSlot.SingleSlot(Optional.of(second))));
        var column = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(first, nested))));
        return new WidgetNode(StableId.random(), RADIO, values,
                Map.of(new SlotName("child"), new WidgetSlot.SingleSlot(Optional.of(column))));
    }

    private static WidgetNode group(Map<PropertyName, PropertyValue> values) {
        return new WidgetNode(StableId.random(), RADIO, values,
                Map.of(new SlotName("child"), new WidgetSlot.SingleSlot(Optional.of(text("Group child")))));
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
        Path project = Files.createDirectories(workspace.resolve("radio_group_probe"));
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
                new io.github.vgrytsenko2022.designer.validation.ValidationResult(List.of()), catalog,
                List.of(), List.of(), Optional.of(baseline.source()), Optional.of(baseline));
        var analyzer = new DartCandidateAnalyzer(executable, ignored -> { });
        assertEquals(45, DartCandidateAnalysisLimits.DEFAULT.totalTimeout().toSeconds());
        for (var entry : candidates) {
            var node = entry.name().equals("mixed-nested-radio-groups") ? mixedGroup(entry.values()) : group(entry.values());
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
            if (entry.name().equals("mixed-nested-radio-groups")) {
                assertEquals(2, probes.stream().filter(probe -> probe.id().endsWith(":radio-core-value-type")).count());
                assertEquals(2, probes.stream().filter(probe -> probe.id().endsWith(":radio-group-core-value-type")).count());
            }
            byte[] candidate = prepared.prospectiveDartBytes();
            String source = new String(candidate, StandardCharsets.UTF_8);
            assertTrue(source.contains("RadioGroup<"), source);
            assertFalse(source.contains("_RadioDefaults"), "No private SDK implementation types");
            assertTrue(source.contains("onChanged:"), source);
            if (entry.name().equals("omitted-group-value")) {
                assertFalse(generation.generated().orElseThrow().build().payload().contains("groupValue:"));
            }
            if (entry.name().equals("explicit-null-group")) assertTrue(source.contains("groupValue: null"));
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
        values.put(p("valueType"), string("String"));
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
        return new PropertyValue.DartObjectReferenceValue(Optional.of("package:radio_group_probe/types.dart"), name, Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }
    private static PropertyValue.DartObjectReferenceValue externalMember(String name, String member) {
        return new PropertyValue.DartObjectReferenceValue(Optional.of("package:radio_group_probe/types.dart"), name, Optional.of(member),
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
                typedef NullableChoice = Choice?;
                T _genericValue<T>() => throw StateError('Never execute project code');
                Choice? _nullableChoice;
                void _choiceChanged(Choice? value) {}
                class Selection { const Selection(); }
                typedef SelectionList = List<String>;
                Selection? _SelectionNullable;
                void _SelectionChanged(Selection? value) {}
                SelectionList? _SelectionListNullable;
                void _SelectionListChanged(SelectionList? value) {}
                dynamic _dynamicValue = 'option';
                dynamic _dynamicChanged = _StringChanged;
                ValueChanged<String?>? _nullableChanged = _StringChanged;
                const _notAType = 1;
                String? _makeNullableString() => throw StateError('Never execute project code');
                ValueChanged<String?> _makeChanged() => throw StateError('Never execute project code');
                """);
        var literals = Map.of("String", "'option'", "int", "1", "double", "1.0",
                "num", "2.5", "bool", "true", "Object", "'object'");
        for (String type : List.of("String", "int", "double", "num", "bool", "Object")) {
            declarations.append(type).append(" _").append(type).append("Value = ").append(literals.get(type)).append(";\n")
                    .append(type).append("? _").append(type).append("Nullable;\n")
                    .append("void _").append(type).append("Changed(").append(type).append("? value) {}\n");
        }
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
        addPackage(packages, "radio_group_probe", project, "3.10");
        addPackage(packages, "flutter", sdk.resolve("packages/flutter"), "3.10");
        addPackage(packages, "sky_engine", sdk.resolve("bin/cache/pkg/sky_engine"), "3.10");
        for (var dependency : Map.of("characters", "1.4.1", "collection", "1.19.1",
                "material_color_utilities", "0.13.0", "meta", "1.18.0", "vector_math", "2.2.0").entrySet()) {
            addPackage(packages, dependency.getKey(), cache.resolve("hosted/pub.dev")
                    .resolve(dependency.getKey() + "-" + dependency.getValue()), "3.4");
        }
        Path tool = Files.createDirectories(project.resolve(".dart_tool"));
        Files.writeString(tool.resolve("package_config.json"), JSON.writeValueAsString(config), StandardCharsets.UTF_8);
        Files.writeString(project.resolve("pubspec.yaml"), "name: radio_group_probe\nenvironment:\n  sdk: ^3.10.0\n"
                + "dependencies:\n  flutter:\n    sdk: flutter\nflutter:\n  uses-material-design: true\n", StandardCharsets.UTF_8);
    }

    private static void addPackage(ArrayNode packages, String name, Path root, String language) {
        assertTrue(Files.isDirectory(root), "Pinned package missing: " + root);
        packages.addObject().put("name", name).put("rootUri", root.toUri().toString())
                .put("packageUri", "lib/").put("languageVersion", language);
    }
}
