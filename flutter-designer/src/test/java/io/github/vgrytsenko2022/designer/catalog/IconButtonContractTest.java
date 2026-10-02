package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.codec.*;
import io.github.vgrytsenko2022.designer.command.PatchProperties;
import io.github.vgrytsenko2022.designer.generation.*;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.validation.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import org.junit.jupiter.api.Test;
import static io.github.vgrytsenko2022.designer.catalog.IconButtonTestValues.*;
import static org.junit.jupiter.api.Assertions.*;

class IconButtonContractTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();

    @Test
    void complete524RowsKeepExactOrdinalsAndRequiredIconWrapperWithoutMetadataLimitLeak() {
        assertEquals(524, definition().properties().size());
        assertEquals(524, IconButtonWidgetPropertySchema.FLATTENED_PROPERTY_COUNT);
        assertEquals(498, IconButtonWidgetPropertySchema.localStyleProperties().size());
        assertEquals(25, IconButtonWidgetPropertySchema.DIRECT_PROPERTY_COUNT);
        assertEquals(9, IconButtonWidgetPropertySchema.STATE_COUNT);
        assertEquals(54, IconButtonWidgetPropertySchema.STATE_PROPERTY_COUNT);
        assertEquals(12, IconButtonWidgetPropertySchema.COMMON_STYLE_PROPERTY_COUNT);
        assertEquals(new PaletteMetadata("flutter.material", 100, 180, "IconButton"), definition().palette());
        assertTrue(definition().constConstructor());
        assertTrue(definition().traits().isEmpty());
        Set<Integer> orders = new HashSet<>();
        for (var property : definition().properties()) {
            var binding = IconButtonWidgetPropertySchema.find(property.name()).orElseThrow();
            assertEquals(binding.dartOrder(), property.parameter().order(), property.name().value());
            assertTrue(orders.add(binding.dartOrder()));
            assertEquals(Set.of("variant", "enabled").contains(property.name().value()), property.parameter().required());
            assertEquals(property.parameter().required(), property.creationDefault().isPresent());
            assertFalse(binding.description().isBlank());
        }
        for (var slot : definition().slots()) assertTrue(orders.add(slot.parameter().order()));
        assertEquals(526, orders.size());
        assertEquals(0, Collections.min(orders));
        assertEquals(525, Collections.max(orders));
        var prototype = WidgetNodePrototypeFactory.create(definition(), StableId.random());
        assertEquals(2, prototype.properties().size());
        assertFalse(valid(prototype));
        assertEquals("icon", WidgetPlacementRules.requiredAnyWidgetWrapperSlot(definition()).orElseThrow().name().value());
        assertEquals(WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD, WidgetPlacementRules.creationMode(definition()));
        assertEquals(45, CATALOG.definitions().stream().filter(d -> WidgetPlacementRules.creationMode(d)
                == WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD).count());
        assertEquals(1024, WidgetDefinition.MAX_PROPERTIES);
        assertEquals(1024, ValidationLimits.defaults().maxPropertiesPerWidget());
        assertEquals(1024, FdCodecLimits.defaults().maxPropertiesPerWidget());
        assertEquals(17, DesignerDocument.SCHEMA_VERSION);
        assertEquals(16, WidgetCatalog.API_VERSION);
    }

    @Test
    void allFourConstConstructorsRetainBothIconSlotsAndNullableSelection() throws Exception {
        for (String variant : IconButtonWidgetPropertySchema.variants()) {
            for (PropertyValue selection : List.of(new PropertyValue.NullValue(),
                    new PropertyValue.BooleanValue(false), new PropertyValue.BooleanValue(true))) {
                var original = node(Map.of(p("variant"), s(variant), p("enabled"), new PropertyValue.BooleanValue(false),
                        p("isSelected"), selection));
                var slots = new LinkedHashMap<>(original.slots());
                slots.put(new SlotName("selectedIcon"), WidgetSlot.SingleSlot.of(text("Selected")));
                var root = new WidgetNode(original.id(), original.type(), original.properties(), slots);
                var output = generated(root);
                String member = variant.equals("standard") ? "" : "." + variant;
                assertTrue(output.build().payload().contains("const IconButton" + member + "("));
                assertTrue(output.build().payload().contains("icon: const Text('Icon')"));
                assertTrue(output.build().payload().contains("selectedIcon: const Text('Selected')"));
                assertTrue(output.build().payload().contains("isSelected: " +
                        (selection instanceof PropertyValue.NullValue ? "null" : ((PropertyValue.BooleanValue) selection).value())));
                assertFalse(output.build().payload().contains("variant:"));
                assertFalse(output.build().payload().contains("enabled:"));
                assertSymbols(output);
                assertEquals(!member.isEmpty(), output.symbolOccurrences().stream().anyMatch(symbol ->
                        symbol.symbolName().equals(variant) && symbol.modelPath().equals("/root/properties/variant")));
                roundTrip(root);
            }
        }
        assertFalse(generated(node(Map.of())).build().payload().contains("isSelected:"));
    }

    @Test
    void activationEnablesLongPressWithBenignPressedAndDoesNotEmitDisabledStoredReferences() {
        var fresh = generated(node(Map.of()));
        assertTrue(fresh.build().payload().contains("onPressed: () {}"));
        assertFalse(fresh.build().payload().contains("const IconButton("));
        var longOnly = generated(node(Map.of(p("onLongPress"), reference("longPress"))));
        assertTrue(longOnly.build().payload().contains("onPressed: () {}"));
        assertTrue(longOnly.build().payload().contains("buttonValues.longPress"));
        var disabled = generated(node(Map.of(p("enabled"), new PropertyValue.BooleanValue(false),
                p("onPressed"), reference("pressed"), p("onLongPress"), reference("longPress"))));
        assertTrue(disabled.build().payload().contains("const IconButton("));
        assertTrue(disabled.build().payload().contains("onPressed: null"));
        assertTrue(disabled.build().payload().contains("onLongPress: null"));
        assertFalse(disabled.build().payload().contains("buttonValues"));
        assertFalse(disabled.imports().payload().contains("package:buttons"));
        assertTrue(disabled.symbolOccurrences().stream().noneMatch(symbol ->
                symbol.modelPath().contains("onPressed") || symbol.modelPath().contains("onLongPress")));
        assertTrue(generated(node(Map.of(p("enabled"), new PropertyValue.BooleanValue(false),
                p("onHover"), reference("hover")))).build().payload().contains("buttonValues.hover"));
    }

    @Test
    void every524FieldParticipatesInDense505FamiliesWithExactSymbolsAndRoundTrips() throws Exception {
        Set<PropertyName> covered = new HashSet<>();
        int largest = 0;
        for (String variant : IconButtonWidgetPropertySchema.variants()) {
            for (boolean paint : List.of(false, true)) for (boolean circle : List.of(false, true)) {
                var values = full(variant, paint, circle);
                covered.addAll(values.keySet());
                largest = Math.max(largest, values.size());
                var root = node(values);
                var output = generated(root);
                assertSymbols(output);
                String dart = output.build().payload();
                assertTrue(dart.contains("IconButtonTheme.of(context)"));
                assertTrue(dart.contains("frameworkDefaults = ButtonStyle("));
                assertFalse(dart.contains(".defaultStyleOf(context)"));
                for (String name : List.of("ElevatedButton", "FilledButton", "OutlinedButton", "TextButton")) {
                    assertFalse(dart.contains(name), name);
                }
                assertTrue(output.symbolOccurrences().size() <= 2048, "Exact probes: " + output.symbolOccurrences().size());
                roundTrip(root);
            }
        }
        covered.add(p("style"));
        assertEquals(524, covered.size());
        assertEquals(505, largest);
    }

    @Test
    void sparseCompoundProjectionUsesOnlyPinnedPublicDefaultsAndOwnTheme() {
        for (String variant : IconButtonWidgetPropertySchema.variants()) {
            var output = generated(node(Map.of(p("variant"), s(variant),
                    p("styleMinimumWidth"), d("20"), p("stylePressedSideWidth"), d("2"),
                    p("styleShapeKind"), s("stadium"), p("styleVisualDensityHorizontal"), d("-1"))));
            String dart = output.build().payload();
            assertTrue(dart.contains("Size(40.0, 40.0)"));
            assertTrue(dart.contains("Size.infinite"));
            assertTrue(dart.contains("StadiumBorder()"));
            assertTrue(dart.contains("IconButtonTheme.of(context).style?."));
            assertFalse(dart.contains("IconTheme.of"));
            assertFalse(dart.contains(".defaultStyleOf"));
            assertEquals(variant.equals("outlined"), dart.contains("colorScheme.onSurface.withOpacity(0.12)"));
            assertEquals(variant.equals("outlined"), dart.contains("colorScheme.outline"));
            assertSymbols(output);
        }
    }

    @Test
    void partialSizesAndDensityCompleteFromDirectFieldsBeforeThemeOnlyWhenTheirCompositeIsActive() {
        var output = generated(node(Map.of(p("constraints"), value("constraints"),
                p("visualDensityVertical"), d("-2"), p("stylePressedMinimumWidth"), d("80"),
                p("styleVisualDensityHorizontal"), d("1"))));
        String dart = output.build().payload();
        assertTrue(dart.contains(").minWidth"));
        assertTrue(dart.contains(").maxHeight"));
        assertTrue(dart.contains("return null;"));
        assertTrue(dart.contains("final inherited = const VisualDensity(vertical: -2.0)"), dart);
        assertFalse(dart.contains("IconButtonTheme.of(context).style?.visualDensity"));
        assertSymbols(output);
        var direct = generated(node(Map.of(p("visualDensityHorizontal"), d("-1"))));
        assertTrue(direct.build().payload().contains("visualDensity: const VisualDensity(horizontal: -1.0)"));
        assertFalse(direct.build().payload().contains("visualDensityVertical:"));
        assertFalse(direct.build().payload().contains("ButtonStyle("));
        var scalar = generated(node(Map.of(p("color"), new PropertyValue.ColorValue(0xff112233L),
                p("styleHoveredForegroundColor"), new PropertyValue.ColorValue(0xff778899L))));
        assertTrue(scalar.build().payload().contains("WidgetState.disabled: null"));
        assertFalse(scalar.build().payload().contains("frameworkDefaults"));
    }

    @Test
    void numericDomainsPreserveSignedSizePositiveSplashAndFixedInfinityWithoutCoreScopeChanges() throws Exception {
        for (String name : List.of("iconSize", "splashRadius")) {
            for (PropertyValue number : List.of(d("0.0001"), d("1e308"), infinity(),
                    new PropertyValue.IntegerValue(BigInteger.valueOf(42)))) {
                assertTrue(accepts(name, number));
                var output = generated(node(Map.of(p("enabled"), new PropertyValue.BooleanValue(false), p(name), number)));
                assertFalse(output.imports().payload().contains("dart:core"));
                assertTrue(output.symbolOccurrences().stream().noneMatch(symbol -> symbol.libraryUri().equals("dart:core")));
                if (number.equals(infinity())) assertTrue(output.build().payload().contains("(1.0 / 0.0)"));
                roundTrip(node(Map.of(p(name), number)));
            }
            for (PropertyValue bad : List.of(d("1e400"), new PropertyValue.NullValue(), s("infinity"),
                    new PropertyValue.EnumValue("double", "maxFinite"))) assertFalse(accepts(name, bad));
        }
        for (PropertyValue signed : List.of(d("-2"), d("0"), new PropertyValue.IntegerValue(BigInteger.valueOf(-4)))) {
            assertTrue(accepts("iconSize", signed));
            assertFalse(accepts("splashRadius", signed));
        }
        for (String name : List.of("visualDensityHorizontal", "visualDensityVertical")) {
            assertTrue(accepts(name, d("-4")));
            assertTrue(accepts(name, d("4")));
            assertFalse(accepts(name, d("4.001")));
            assertFalse(accepts(name, infinity()));
        }
    }

    @Test
    void all41PresetCursorOwnersAndMembersHaveExactSourceProvenance() {
        assertEquals(41, IconButtonWidgetPropertySchema.mouseCursorPresets().size());
        for (String preset : IconButtonWidgetPropertySchema.mouseCursorPresets()) {
            var output = generated(node(Map.of(p("mouseCursor"), s(preset))));
            String owner = Set.of("defer", "uncontrolled").contains(preset) ? "MouseCursor"
                    : Set.of("clickable", "adaptiveClickable", "textable").contains(preset) ? "WidgetStateMouseCursor" : "SystemMouseCursors";
            assertTrue(output.build().payload().contains(owner + "." + preset));
            assertTrue(output.symbolOccurrences().stream().anyMatch(symbol -> symbol.symbolName().equals(owner)));
            assertTrue(output.symbolOccurrences().stream().anyMatch(symbol -> symbol.symbolName().equals(preset)));
            assertSymbols(output);
        }
        assertFalse(accepts("mouseCursor", s("rawCursor()")));
        assertTrue(accepts("mouseCursor", reference("cursor")));
    }

    @Test
    void wholeProjectStyleExcludesAll498LeavesButNotDirectSdkProperties() {
        for (String name : IconButtonWidgetPropertySchema.localStyleProperties()) {
            assertFalse(valid(node(Map.of(p("style"), reference("whole"), p(name), value(name)))), name);
        }
        var output = generated(node(Map.of(p("style"), reference("whole"), p("color"), value("color"),
                p("constraints"), value("constraints"), p("visualDensityVertical"), d("1"))));
        assertTrue(output.build().payload().contains("buttonValues.whole"));
        assertFalse(output.build().payload().contains("frameworkDefaults"));
        assertFalse(output.build().payload().contains("ButtonStyle("));
        assertSymbols(output);
    }

    @Test
    void allNineStatesRetainIndependentPaintTextShapeAndDimensionRelations() {
        for (String prefix : IconButtonWidgetPropertySchema.statePrefixes()) {
            assertFalse(valid(node(Map.of(p(prefix + "TextBackground"), value(prefix + "TextBackground"),
                    p(prefix + "TextBackgroundColor"), value(prefix + "TextBackgroundColor")))));
            assertFalse(valid(node(Map.of(p(prefix + "MinimumWidth"), d("120"), p(prefix + "MaximumWidth"), d("80")))));
            assertFalse(valid(node(Map.of(p(prefix + "ShapeCircleEccentricity"), d("0.5")))));
            assertFalse(valid(node(Map.of(p(prefix + "TextTheme"), value(prefix + "TextTheme")))));
        }
        assertTrue(valid(node(Map.of(p("styleMinimumWidth"), d("40"), p("styleMaximumWidth"), d("80"),
                p("styleFixedWidth"), d("120")))), "SDK clamps fixed size; do not invent a cross-bound rejection.");
        assertTrue(valid(node(Map.of(p("styleTextFontFamilyFallback"), s("Base"),
                p("styleErrorTextFontFamilyFallback"), s(""), p("styleErrorTextPackage"), s("pkg")))));
    }

    @Test
    void iconIsAlwaysRequiredAndSelectedIconAlwaysOptionalAcrossFourVariants() {
        for (String variant : IconButtonWidgetPropertySchema.variants()) {
            var base = node(Map.of(p("variant"), s(variant)));
            assertFalse(valid(new WidgetNode(base.id(), base.type(), base.properties(), Map.of())));
            assertFalse(valid(new WidgetNode(base.id(), base.type(), base.properties(),
                    Map.of(new SlotName("icon"), WidgetSlot.SingleSlot.empty()))));
            var slots = new LinkedHashMap<>(base.slots());
            slots.put(new SlotName("selectedIcon"), WidgetSlot.SingleSlot.of(text("Selected")));
            assertTrue(valid(new WidgetNode(base.id(), base.type(), base.properties(), slots)));
        }
        for (String name : List.of("child", "label", "clipBehavior", "isSemanticButton", "onFocusChange", "iconAlignment", "key")) {
            assertFalse(valid(node(Map.of(p(name), s("unsupported")))), name);
        }
        for (String required : List.of("enabled", "variant")) {
            var base = node(Map.of());
            var properties = new LinkedHashMap<>(base.properties());
            properties.remove(p(required));
            assertFalse(valid(new WidgetNode(base.id(), base.type(), properties, base.slots())));
        }
    }

    @Test
    void metadataAndPersisted1024KeepAtomicPatch512AndExplicitLowerValidationBudgets() throws Exception {
        var properties = new ArrayList<PropertyDefinition>();
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        for (int index = 0; index < 1024; index++) {
            properties.add(new PropertyDefinition(p("p" + index), DartParameter.named(index, false),
                    List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.STRING)), Optional.empty()));
            values.put(p("p" + index), s("v"));
        }
        var schema = new WidgetDefinition(new WidgetTypeId("example.Dense"), "Dense", Optional.empty(), true,
                "package:example/dense.dart", List.of("package:example/dense.dart"), Set.of(),
                new PaletteMetadata("example", 1, 1, "Dense"), properties, List.of());
        var catalog = WidgetCatalog.strict(List.of(schema));
        var root = new WidgetNode(StableId.random(), schema.typeId(), values, Map.of());
        assertTrue(new WidgetTreeValidator().validate(document(root), catalog).valid());
        roundTrip(root);
        var patches = new ArrayList<PatchProperties.Patch>();
        values.entrySet().stream().limit(512).forEach(entry -> patches.add(new PatchProperties.SetPatch(entry.getKey(), entry.getValue())));
        assertEquals(512, new PatchProperties(root.id(), patches).patches().size());
        patches.add(new PatchProperties.SetPatch(p("p512"), s("v")));
        assertThrows(IllegalArgumentException.class, () -> new PatchProperties(root.id(), patches));
        values.put(p("p1024"), s("v"));
        var oversized = new WidgetNode(root.id(), schema.typeId(), values, Map.of());
        assertFalse(new WidgetTreeValidator().validate(document(oversized), catalog).valid());
        assertThrows(Exception.class, () -> new FdDocumentCodec().encode(document(oversized)));
        var smaller = new ValidationLimits(256, 10_000, 511, 128, 1000);
        assertFalse(new WidgetTreeValidator(smaller).validate(document(root), catalog).valid());
    }

    @Test
    void familyAndNamedConstructorProvenanceSurviveProjectSymbolCollisions() {
        for (String collision : List.of("IconButton", "IconButtonTheme", "ButtonStyle", "StadiumBorder")) {
            var ref = new PropertyValue.DartObjectReferenceValue(Optional.empty(), collision, Optional.of("pressed"),
                    PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
            var output = generated(node(Map.of(p("variant"), s("outlined"), p("onPressed"), ref,
                    p("styleMinimumWidth"), d("24"), p("stylePressedSideWidth"), d("1"))));
            assertSymbols(output);
            assertTrue(output.build().payload().contains(collision + ".pressed"));
            assertTrue(output.symbolOccurrences().stream().anyMatch(symbol ->
                    symbol.symbolName().equals("outlined") && symbol.modelPath().equals("/root/properties/variant")));
        }
    }

    @Test
    void genericWrapperDiscoveryUsesUniqueRequiredSlotAndFailsClosedOnOtherPrerequisites() {
        var icon = definition();
        assertEquals(List.of("C|flutter.material.IconButton|paletteCreate|wrapExistingChild|icon"),
                WidgetPlacementRules.capabilityFingerprintLines(icon));
        var requiredIcon = icon.slot(new SlotName("icon")).orElseThrow();
        var optional = icon.slot(new SlotName("selectedIcon")).orElseThrow();
        for (SlotDefinition additional : List.of(
                new SlotDefinition(optional.name(), DartParameter.named(23, true), SlotCardinality.SINGLE, 1, 1, optional.acceptance()),
                new SlotDefinition(optional.name(), DartParameter.named(23, true), SlotCardinality.SINGLE, 0, 1, optional.acceptance()),
                new SlotDefinition(optional.name(), DartParameter.named(23, false), SlotCardinality.SINGLE, 1, 1, optional.acceptance()))) {
            var altered = copyDefinition(icon, icon.properties(), List.of(requiredIcon, additional));
            assertTrue(WidgetPlacementRules.requiredAnyWidgetWrapperSlot(altered).isEmpty());
        }
        var missingDefault = icon.properties().stream().map(property -> property.name().value().equals("enabled")
                ? new PropertyDefinition(property.name(), property.parameter(), property.constraints(), Optional.empty()) : property).toList();
        assertTrue(WidgetPlacementRules.requiredAnyWidgetWrapperSlot(copyDefinition(icon, missingDefault, icon.slots())).isEmpty());
        assertEquals("child", WidgetPlacementRules.requiredAnyWidgetWrapperSlot(
                CATALOG.find(TextButtonWidgetPropertySchema.TEXT_BUTTON_TYPE).orElseThrow()).orElseThrow().name().value());
        assertTrue(WidgetPlacementRules.requiredAnyWidgetWrapperSlot(
                CATALOG.find(FilledButtonWidgetPropertySchema.FILLED_BUTTON_TYPE).orElseThrow()).isEmpty());
    }

    private static WidgetDefinition copyDefinition(WidgetDefinition original,
            List<PropertyDefinition> properties, List<SlotDefinition> slots) {
        return new WidgetDefinition(original.typeId(), original.dartClassName(), original.namedConstructor(),
                original.constConstructor(), original.dartLibraryUri(), original.importUris(), original.traits(),
                original.palette(), properties, slots);
    }

    private static boolean accepts(String name, PropertyValue value) {
        return definition().property(p(name)).orElseThrow().constraints().stream().anyMatch(c -> c.accepts(value));
    }

    private static boolean valid(WidgetNode root) {
        return new WidgetTreeValidator().validate(document(root), CATALOG).valid();
    }

    private static GeneratedDartRegions generated(WidgetNode root) {
        var result = new DartRegionGenerator().generate(document(root), CATALOG);
        assertTrue(result.successful(), result.diagnostics().toString());
        return result.generated().orElseThrow();
    }

    private static void roundTrip(WidgetNode root) throws Exception {
        var codec = new FdDocumentCodec();
        assertEquals(document(root), assertInstanceOf(FdDecodeResult.Current.class,
                codec.decode(codec.encode(document(root)))).document());
    }

    private static void assertSymbols(GeneratedDartRegions output) {
        String source = output.build().payload();
        for (var symbol : output.symbolOccurrences()) {
            assertEquals(symbol.symbolName(), source.substring(symbol.offset(), symbol.endOffset()), symbol.toString());
        }
        assertEquals(output.symbolOccurrences().size(),
                output.symbolOccurrences().stream().map(GeneratedDartSymbolOccurrence::id).distinct().count());
    }
}
