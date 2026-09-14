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
import dev.flutter.netbeans.plugin.designer.properties.RadioListTilePropertyContractTest;
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

/** Exact RadioListTile<T> constructor, generic/callback proofs and immutable pair-save candidates. */
class RadioListTileCandidateRealSdkTest {
    @TempDir Path workspace;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final StableId DOCUMENT_ID = StableId.parse("78000000-0000-4000-8000-000000000001");
    private static final WidgetTypeId RADIO = new WidgetTypeId("flutter.material.RadioListTile");
    private static final List<String> STATES = List.of("Default", "Disabled", "Error", "Dragged",
            "Pressed", "Selected", "ScrolledUnder", "Hovered", "Focused");

    @Test
    void denseStandardAndAdaptiveConstructorsCoverEveryLocalStateFamily() throws Exception {
        var cases = new ArrayList<Candidate>();
        for (String variant : List.of("standard", "adaptive")) {
            var values = new LinkedHashMap<>(RadioListTilePropertyContractTest.full());
            values.put(p("variant"), string(variant));
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
            cases.add(valid("enum-" + nullable, values));
        }
        var imported = base();
        imported.put(p("valueType"), external("RemoteChoice"));
        imported.put(p("value"), externalMember("RemoteChoice", "second"));
        imported.put(p("groupValue"), external("remoteChoice"));
        imported.put(p("onChanged"), external("remoteChanged"));
        cases.add(valid("imported-enum", imported));
        for (String type : List.of("Selection", "SelectionList")) {
            var values = base();
            values.put(p("valueType"), reference(type));
            values.put(p("value"), reference("_" + type + "Value"));
            values.put(p("groupValue"), reference("_" + type + "Nullable"));
            values.put(p("onChanged"), reference("_" + type + "Changed"));
            cases.add(valid("named-" + type, values));
        }
        validate(cases);
    }

    @Test
    void nullableDynamicWrongTypesAndCallbackMismatchesFailBeforeSave() throws Exception {
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
            for (String name : List.of("fillColor", "overlayColor", "radioBackgroundColor", "radioInnerRadius",
                    "radioSide", "focusNode", "mouseCursor", "visualDensity", "shape", "contentPadding", "statesController", "onFocusChange", "activeColor")) {
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
            for (String family : List.of("fillColor", "overlayColor", "radioBackgroundColor", "radioInnerRadius")) {
                nulls.put(p(family + state), nil());
            }
        }
        cases.add(valid("all-state-null", nulls));
        for (String member : List.of("infinity", "negativeInfinity")) {
            var values = base();
            values.put(p("splashRadius"), new PropertyValue.EnumValue("double", member));
            for (String state : STATES) values.put(p("radioInnerRadius" + state), new PropertyValue.EnumValue("double", member));
            cases.add(valid("radii-" + member, values));
        }
        var extreme = base();
        extreme.put(p("splashRadius"), new PropertyValue.DoubleValue(new BigDecimal("-1e308")));
        extreme.put(p("radioInnerRadiusSelected"), new PropertyValue.DoubleValue(new BigDecimal("1e308")));
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
        for (String family : List.of("fillColor", "overlayColor", "radioBackgroundColor", "radioInnerRadius",
                "radioSide", "focusNode", "mouseCursor", "visualDensity", "shape", "contentPadding", "statesController", "onFocusChange", "activeColor")) {
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

    @Test
    void constructorSlotsNullableTileFieldsScaleAndGeometryMatrixHasExactSdkProofs() throws Exception {
        var tiles = new ArrayList<WidgetNode>();
        for (String variant : List.of("standard", "adaptive")) {
            for (int mask = 0; mask < 8; mask++) {
                var values = base(); values.put(p("variant"), string(variant));
                if ((mask & 2) != 0) values.put(p("isThreeLine"), bool(true));
                tiles.add(new WidgetNode(StableId.random(), RADIO, values, slots(mask)));
            }
            for (String name : List.of("isThreeLine", "dense", "enableFeedback", "enabled")) {
                var values = base(); values.put(p("variant"), string(variant)); values.put(p(name), nil());
                values.put(p("onChanged"), nil()); values.put(p("onFocusChange"), nil());
                tiles.add(new WidgetNode(StableId.random(), RADIO, values, slots(7)));
            }
        }
        for (String affinity : List.of("leading", "trailing", "platform")) {
            for (String alignment : List.of("threeLine", "titleHeight", "top", "center", "bottom")) {
                var values = base();
                values.put(p("controlAffinity"), new PropertyValue.EnumValue("ListTileControlAffinity", affinity));
                values.put(p("titleAlignment"), new PropertyValue.EnumValue("ListTileTitleAlignment", alignment));
                tiles.add(new WidgetNode(StableId.random(), RADIO, values, slots(7)));
            }
        }
        for (String name : List.of("horizontalTitleGap", "minVerticalPadding", "minLeadingWidth", "minTileHeight", "splashRadius", "radioScaleFactor")) {
            var variants = new ArrayList<PropertyValue>(List.of(integer(-2), integer(0),
                    new PropertyValue.EnumValue("double", "infinity"), new PropertyValue.EnumValue("double", "negativeInfinity"),
                    new PropertyValue.EnumValue("double", "nan")));
            if (!name.equals("radioScaleFactor")) variants.add(nil());
            for (var value : variants) {
                var values = base(); values.put(p(name), value);
                tiles.add(new WidgetNode(StableId.random(), RADIO, values, slots(0)));
            }
        }
        assertEquals(74, tiles.size());
        var root = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(tiles)));
        validate(List.of(new Candidate("constructor-slot-geometry-matrix", Map.of(), "", false, 0, root)));
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
        Path project = Files.createDirectories(workspace.resolve("radio_list_tile_probe"));
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
                new dev.flutter.netbeans.designer.validation.ValidationResult(List.of()), catalog,
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
            assertTrue(source.contains("RadioListTile<"), source);
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

    private static Map<SlotName, WidgetSlot> slots(int mask) {
        var slots = new LinkedHashMap<SlotName, WidgetSlot>();
        int bit = 0;
        for (String name : List.of("title", "subtitle", "secondary")) {
            if ((mask & (1 << bit++)) != 0) slots.put(new SlotName(name), WidgetSlot.SingleSlot.of(text(name)));
        }
        return slots;
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
        return new PropertyValue.DartObjectReferenceValue(Optional.of("package:radio_list_tile_probe/types.dart"), name, Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }
    private static PropertyValue.DartObjectReferenceValue externalMember(String name, String member) {
        return new PropertyValue.DartObjectReferenceValue(Optional.of("package:radio_list_tile_probe/types.dart"), name, Optional.of(member),
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
                void _onChanged(String? value) {}
                void _onFocusChange(bool hasFocus) {}
                const _shape = RoundedRectangleBorder(borderRadius: BorderRadius.all(Radius.circular(8)));
                ShapeBorder _makeShape() => _shape;
                const _contentPadding = EdgeInsetsDirectional.fromSTEB(-2, 4, 10, 6);
                EdgeInsetsGeometry _makeContentPadding() => _contentPadding;
                final _statesController = WidgetStatesController();
                WidgetStatesController _makeStatesController() => _statesController;
                ValueChanged<bool> _makeOnFocusChange() => _onFocusChange;
                const Color _activeColor = Color(0xff123456);
                Color _makeActiveColor() => _activeColor;
                dynamic _dynamicValue = 'option';
                dynamic _dynamicChanged = _StringChanged;
                ValueChanged<String?>? _nullableChanged = _StringChanged;
                const _notAType = 1;
                String? _makeNullableString() => null;
                const _fillColor = WidgetStatePropertyAll<Color?>(Color(0xff123456));
                const _overlayColor = WidgetStatePropertyAll<Color?>(Color(0xff345678));
                const _radioBackgroundColor = WidgetStatePropertyAll<Color?>(Color(0xff567890));
                const _radioInnerRadius = WidgetStatePropertyAll<double?>(-2);
                const _radioSide = BorderSide(width: 2);
                final _focusNode = FocusNode();
                final _mouseCursor = WidgetStateMouseCursor.clickable;
                const _visualDensity = VisualDensity(horizontal: 1);
                WidgetStateProperty<Color?> _makeFillColor() => _fillColor;
                WidgetStateProperty<Color?> _makeOverlayColor() => _overlayColor;
                WidgetStateProperty<Color?> _makeRadioBackgroundColor() => _radioBackgroundColor;
                WidgetStateProperty<double?> _makeRadioInnerRadius() => _radioInnerRadius;
                BorderSide _makeRadioSide() => _radioSide;
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
        var referenceTypes = Map.ofEntries(
                Map.entry("fillColor", "WidgetStateProperty<Color?>"), Map.entry("overlayColor", "WidgetStateProperty<Color?>"),
                Map.entry("radioBackgroundColor", "WidgetStateProperty<Color?>"), Map.entry("radioInnerRadius", "WidgetStateProperty<double?>"),
                Map.entry("radioSide", "BorderSide"), Map.entry("focusNode", "FocusNode"), Map.entry("mouseCursor", "MouseCursor"),
                Map.entry("visualDensity", "VisualDensity"), Map.entry("shape", "ShapeBorder"), Map.entry("contentPadding", "EdgeInsetsGeometry"),
                Map.entry("statesController", "WidgetStatesController"), Map.entry("onFocusChange", "ValueChanged<bool>"), Map.entry("activeColor", "Color"));
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
        addPackage(packages, "radio_list_tile_probe", project, "3.10");
        addPackage(packages, "flutter", sdk.resolve("packages/flutter"), "3.10");
        addPackage(packages, "sky_engine", sdk.resolve("bin/cache/pkg/sky_engine"), "3.10");
        for (var dependency : Map.of("characters", "1.4.1", "collection", "1.19.1",
                "material_color_utilities", "0.13.0", "meta", "1.18.0", "vector_math", "2.2.0").entrySet()) {
            addPackage(packages, dependency.getKey(), cache.resolve("hosted/pub.dev")
                    .resolve(dependency.getKey() + "-" + dependency.getValue()), "3.4");
        }
        Path tool = Files.createDirectories(project.resolve(".dart_tool"));
        Files.writeString(tool.resolve("package_config.json"), JSON.writeValueAsString(config), StandardCharsets.UTF_8);
        Files.writeString(project.resolve("pubspec.yaml"), "name: radio_list_tile_probe\nenvironment:\n  sdk: ^3.10.0\n"
                + "dependencies:\n  flutter:\n    sdk: flutter\nflutter:\n  uses-material-design: true\n", StandardCharsets.UTF_8);
    }

    private static void addPackage(ArrayNode packages, String name, Path root, String language) {
        assertTrue(Files.isDirectory(root), "Pinned package missing: " + root);
        packages.addObject().put("name", name).put("rootUri", root.toUri().toString())
                .put("packageUri", "lib/").put("languageVersion", language);
    }
}
