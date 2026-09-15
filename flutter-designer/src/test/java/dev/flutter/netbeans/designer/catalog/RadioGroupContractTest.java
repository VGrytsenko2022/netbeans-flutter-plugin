package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.generation.*;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import java.util.*;
import org.junit.jupiter.api.Test;
import static dev.flutter.netbeans.designer.catalog.RadioTestValues.*;
import static org.junit.jupiter.api.Assertions.*;

class RadioGroupContractTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE = RadioGroupWidgetPropertySchema.RADIO_GROUP_TYPE;
    private static final SlotName CHILD = new SlotName("child");

    @Test
    void completeConstructorHasFourPropertiesAndOnlyTwoCreationDefaults() {
        assertEquals(List.of("groupValue", "onChanged", "valueType", "nullableValueType"),
                RadioGroupWidgetPropertySchema.definitions().keySet().stream().toList());
        assertEquals(List.of(0, 1, 3, 4), definition().properties().stream().map(value -> value.parameter().order()).toList());
        assertEquals(4, RadioGroupWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(1, RadioGroupWidgetPropertySchema.SLOT_COUNT);
        assertEquals("package:flutter/widgets.dart", definition().dartLibraryUri());
        assertEquals(new PaletteMetadata("flutter.material", 100, 240, "RadioGroup"), definition().palette());
        assertTrue(definition().constConstructor());
        assertTrue(definition().namedConstructor().isEmpty());
        assertTrue(definition().traits().isEmpty());
        assertEquals(Map.of(p("valueType"), s("String"), p("onChanged"), s("noop")), node(Map.of()).properties());
        assertEquals(List.of("onChanged", "valueType"), definition().properties().stream()
                .filter(value -> value.parameter().required()).map(value -> value.name().value()).toList());
        assertEquals(2, definition().slot(CHILD).orElseThrow().parameter().order());
        assertEquals(17, DesignerDocument.SCHEMA_VERSION);
        assertEquals(16, WidgetCatalog.API_VERSION);
        assertEquals(1024, FdCodecLimits.defaults().maxPropertiesPerWidget());
    }

    @Test
    void independentProjectionMatchesSevenRecordsAndRejectsWeakenedRequiredCallback() throws Exception {
        assertTrue(BuiltInWidgetCapabilityCatalog.canvasProjection(definition()).isPresent());
        String all = BuiltInWidgetCapabilityCatalog.reviewedCanvasSchemaContract();
        int start = all.indexOf("W|flutter.widgets.RadioGroup\n");
        int end = all.indexOf("W|", start + 2);
        String contract = all.substring(start, end < 0 ? all.length() : end);
        assertEquals(7, contract.lines().count());
        java.nio.file.Files.writeString(java.nio.file.Path.of("target/radio-group-contract.txt"), contract);
        var changedProperties = definition().properties().stream().map(value -> value.name().equals(p("onChanged"))
                ? new PropertyDefinition(value.name(), DartParameter.named(1, false), value.constraints(), value.creationDefault()) : value).toList();
        var changed = new WidgetDefinition(TYPE, definition().dartClassName(), Optional.empty(), true,
                definition().dartLibraryUri(), definition().importUris(), Set.of(), definition().palette(), changedProperties, definition().slots());
        assertTrue(BuiltInWidgetCapabilityCatalog.canvasProjection(changed).isEmpty());
    }

    @Test
    void allTwelveBuiltinTypesUseExactCoreIdentityAndPreserveChild() throws Exception {
        for (String type : RadioGroupWidgetPropertySchema.valueTypes()) {
            for (boolean nullable : List.of(false, true)) {
                PropertyValue value = switch (type) {
                    case "int", "num" -> i(1);
                    case "double" -> d("1.5");
                    case "bool" -> b(true);
                    default -> s("option");
                };
                WidgetNode root = node(Map.of(p("valueType"), s(type), p("nullableValueType"), b(nullable), p("groupValue"), value));
                var output = generated(root);
                assertTrue(output.build().payload().contains("RadioGroup<" + type + (nullable ? "?" : "") + ">("));
                assertTrue(output.build().payload().contains("onChanged: (_) {}"));
                assertFalse(output.imports().payload().contains("dart:core"));
                var core = output.symbolOccurrences().stream().filter(value1 -> value1.id().endsWith(":radio-group-core-value-type")).findFirst().orElseThrow();
                assertEquals("dart:core", core.libraryUri());
                assertEquals(type, core.symbolName());
                assertEquals("Type", core.staticTypeRequirement().orElseThrow().expectedDartType());
                assertEquals(Optional.of(type + (nullable ? "?" : "")), core.staticTypeRequirement().orElseThrow().sourceTypeOverride());
                assertSymbols(output);
                roundTrip(root);
            }
        }
    }

    @Test
    void selectedNullOmissionAndSpecialNumericIdentitiesRemainExact() throws Exception {
        WidgetNode omitted = node(Map.of());
        WidgetNode explicit = node(Map.of(p("groupValue"), nil()));
        assertFalse(generated(omitted).build().payload().contains("groupValue:"));
        assertTrue(generated(explicit).build().payload().contains("groupValue: null"));
        assertNotEquals(omitted.properties(), explicit.properties());
        roundTrip(omitted);
        roundTrip(explicit);
        for (String type : List.of("double", "num", "Object")) {
            for (String member : List.of("infinity", "negativeInfinity", "nan")) {
                WidgetNode root = node(Map.of(p("valueType"), s(type), p("groupValue"), new PropertyValue.EnumValue("double", member)));
                assertTrue(generated(root).build().payload().contains(member.equals("nan") ? "(0.0 / 0.0)"
                        : member.equals("infinity") ? "(1.0 / 0.0)" : "(-1.0 / 0.0)"));
                roundTrip(root);
            }
        }
    }

    @Test
    void nonnullRequiredCallbackSupportsEveryReferenceFormAndConstInvocationCandidates() throws Exception {
        for (boolean imported : List.of(false, true)) {
            for (boolean invocation : List.of(false, true)) {
                for (boolean constant : List.of(false, true)) {
                    if (!invocation && constant) continue;
                    var reference = reference(imported, invocation, invocation ? constant : null, "Handlers", Optional.of("changed"));
                    var root = node(Map.of(p("onChanged"), reference));
                    var output = generated(root);
                    var callback = output.symbolOccurrences().stream().filter(symbol -> symbol.modelPath().endsWith("/onChanged/member"))
                            .filter(symbol -> symbol.staticTypeRequirement().isPresent()).findFirst().orElseThrow();
                    assertEquals("ValueChanged<Object?>", callback.staticTypeRequirement().orElseThrow().expectedDartType());
                    assertEquals(Optional.of("String"), callback.staticTypeRequirement().orElseThrow().sourceTypeOverride());
                    assertEquals(constant, output.build().payload().contains("const RadioGroup<String>("));
                    assertSymbols(output);
                    roundTrip(root);
                }
            }
        }
        assertFalse(valid(RadioTestValues.without(node(Map.of()), "onChanged")));
        assertFalse(valid(node(Map.of(p("onChanged"), nil()))));
        assertFalse(valid(node(Map.of(p("onChanged"), new PropertyValue.CallbackValue("legacy")))));
    }

    @Test
    void customTypeRootsAndNullableGroupFactoriesCarryOnlyTheSelectedTypeProof() throws Exception {
        for (boolean imported : List.of(false, true)) {
            for (boolean nullable : List.of(false, true)) {
                var type = reference(imported, false, null, "Choice", Optional.empty());
                var root = node(Map.of(p("valueType"), type, p("nullableValueType"), b(nullable),
                        p("groupValue"), reference(imported, true, false, "selectedChoice", Optional.empty()),
                        p("onChanged"), RadioTestValues.reference("changed")));
                var output = generated(root);
                var typed = output.symbolOccurrences().stream().filter(symbol -> symbol.staticTypeRequirement().isPresent()).toList();
                assertEquals(Set.of("Type", "Object?", "ValueChanged<Object?>"),
                        new HashSet<>(typed.stream().map(symbol -> symbol.staticTypeRequirement().orElseThrow().expectedDartType()).toList()));
                assertEquals(1, typed.stream().map(symbol -> symbol.staticTypeRequirement().orElseThrow().sourceTypeOverride()).distinct().count());
                assertTrue(typed.stream().anyMatch(symbol -> symbol.modelPath().endsWith("/valueType/rootSymbol")));
                assertFalse(output.build().payload().contains("??"));
                assertSymbols(output);
                roundTrip(root);
            }
        }
    }

    @Test
    void invalidTypeReferencesLiteralsAndUnsupportedConstructorBranchesFailBeforeGeneration() {
        for (var entry : Map.of("String", i(1), "int", d("1.0"), "bool", s("false")).entrySet()) {
            assertFalse(valid(node(Map.of(p("valueType"), s(entry.getKey()), p("groupValue"), entry.getValue()))));
        }
        for (PropertyValue invalid : List.of(i(9007199254740992L), d("1e400"), new PropertyValue.EnumValue("double", "maxFinite"),
                new PropertyValue.DartExpressionValue("unsafe()"))) assertFalse(valid(node(Map.of(p("valueType"), s("Object"), p("groupValue"), invalid))));
        for (var invalid : List.of(reference(false, false, null, "Choice", Optional.of("first")),
                reference(false, true, false, "Choice", Optional.empty()))) {
            assertFalse(valid(node(Map.of(p("valueType"), invalid))));
        }
        assertThrows(IllegalArgumentException.class, () -> reference(false, false, true, "Choice", Optional.empty()));
        for (String unsupported : List.of("variant", "enabled", "value", "groupRegistry", "focusNode", "autofocus", "toggleable", "semanticLabel")) {
            assertFalse(valid(node(Map.of(p(unsupported), b(true)))), unsupported);
        }
        assertTrue(RadioGroupWidgetPropertySchema.valueTypeError(node(Map.of(p("valueType"), s("bool"), p("groupValue"), s("x"))))
                .orElseThrow().contains("RadioGroup"));
    }

    @Test
    void childIsRequiredAnyWidgetWithoutFabricationOrStaticDescendantSelectionRestrictions() {
        var prototype = WidgetNodePrototypeFactory.create(definition(), StableId.random());
        assertFalse(valid(prototype));
        assertTrue(((WidgetSlot.SingleSlot) prototype.slots().get(CHILD)).child().isEmpty());
        assertEquals(CHILD, WidgetPlacementRules.requiredAnyWidgetWrapperSlot(definition()).orElseThrow().name());
        assertEquals(WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD, WidgetPlacementRules.creationMode(definition()));
        var slot = definition().slot(CHILD).orElseThrow();
        assertEquals(161, CATALOG.definitions().stream().filter(child -> WidgetPlacementRules.accepts(definition(), slot, child)).count());
        assertFalse(valid(new WidgetNode(prototype.id(), TYPE, prototype.properties(), Map.of())));
        assertFalse(valid(new WidgetNode(prototype.id(), TYPE, prototype.properties(),
                Map.of(CHILD, new WidgetSlot.ListSlot(List.of(text()))))));
        var duplicate = RadioTestValues.node(Map.of(p("groupValue"), s("option")));
        var row = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Row"), Map.of(), Map.of(new SlotName("children"),
                new WidgetSlot.ListSlot(List.of(duplicate, RadioTestValues.node(Map.of(p("enabled"), b(false)))))));
        assertTrue(valid(node(Map.of(p("groupValue"), s("option")), row)), "Only mounted SDK clients determine duplicate-selection validity");
        assertTrue(valid(node(Map.of(p("valueType"), s("int")), RadioTestValues.node(Map.of()))), "Different exact T remains a legal independent subtree");
        assertTrue(valid(node(Map.of(), node(Map.of()))), "Nested same-type groups are legal");
    }

    @Test
    void aggregateCountsAndBudgetsRemainExecutable() {
        var definitions = CATALOG.definitions();
        assertEquals(217, definitions.size());
        assertEquals(182, definitions.stream().filter(WidgetDefinition::constConstructor).count());
        assertEquals(7436, definitions.stream().mapToInt(value -> value.properties().size()).sum());
        assertEquals(679, definitions.stream().flatMap(value -> value.properties().stream()).filter(value -> value.acceptedKinds().equals(Set.of(PropertyValueKind.BOOLEAN))).count());
        assertEquals(53, definitions.stream().flatMap(value -> value.properties().stream()).filter(value -> value.acceptedKinds().equals(Set.of(PropertyValueKind.BOOLEAN, PropertyValueKind.NULL))).count());
        assertEquals(6, definitions.stream().flatMap(value -> value.properties().stream()).filter(value -> value.acceptedKinds().containsAll(Set.of(PropertyValueKind.NULL,
                PropertyValueKind.STRING, PropertyValueKind.BOOLEAN, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE))).count());
        assertEquals(32, definitions.stream().filter(value -> WidgetPlacementRules.evaluateRoot(value).accepted()
                && WidgetPlacementRules.requiredAnyWidgetWrapperSlot(value).isPresent()).count());
    }

    private static WidgetDefinition definition() { return CATALOG.find(TYPE).orElseThrow(); }
    static WidgetNode node(Map<PropertyName, PropertyValue> properties) { return node(properties, text()); }
    static WidgetNode node(Map<PropertyName, PropertyValue> properties, WidgetNode child) {
        var values = new LinkedHashMap<>(WidgetNodePrototypeFactory.create(definition(), StableId.random()).properties());
        values.putAll(properties);
        return new WidgetNode(StableId.random(), TYPE, values, Map.of(CHILD, new WidgetSlot.SingleSlot(Optional.of(child))));
    }
    private static WidgetNode text() { return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"), Map.of(p("data"), s("Preserved child")), Map.of()); }
    private static PropertyValue.DartObjectReferenceValue reference(boolean imported, boolean invocation, Boolean constant, String root, Optional<String> member) {
        return new PropertyValue.DartObjectReferenceValue(imported ? Optional.of("package:radio_types/values.dart") : Optional.empty(), root, member,
                invocation ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION : PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.ofNullable(constant));
    }
    private static boolean valid(WidgetNode root) { return new WidgetTreeValidator().validate(document(root), CATALOG).valid(); }
    private static GeneratedDartRegions generated(WidgetNode root) {
        var result = new DartRegionGenerator().generate(document(root), CATALOG);
        assertTrue(result.successful(), result.modelValidation().issues() + " " + result.diagnostics());
        return result.generated().orElseThrow();
    }
    private static void roundTrip(WidgetNode root) throws Exception {
        var codec = new FdDocumentCodec();
        assertEquals(document(root), assertInstanceOf(FdDecodeResult.Current.class, codec.decode(codec.encode(document(root)))).document());
    }
    private static void assertSymbols(GeneratedDartRegions output) {
        for (var symbol : output.symbolOccurrences()) assertEquals(symbol.symbolName(), output.build().payload().substring(symbol.offset(), symbol.endOffset()));
        assertEquals(output.symbolOccurrences().size(), output.symbolOccurrences().stream().map(GeneratedDartSymbolOccurrence::id).distinct().count());
    }
}
