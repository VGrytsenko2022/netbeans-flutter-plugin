package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.codec.*;
import io.github.vgrytsenko2022.designer.generation.*;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.validation.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import org.junit.jupiter.api.Test;
import static io.github.vgrytsenko2022.designer.catalog.SwitchTestValues.*;
import static org.junit.jupiter.api.Assertions.*;

class SwitchContractTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();

    @Test
    void complete201RowsHaveOnlyThreeCreationDefaultsAndNoSlotsOrFormatChanges() {
        assertEquals(201, definition().properties().size());
        assertEquals(201, SwitchWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(30, SwitchWidgetPropertySchema.DIRECT_PROPERTY_COUNT);
        assertEquals("Constructor", SwitchWidgetPropertySchema.find("variant").orElseThrow().displayName());
        assertEquals(new PaletteMetadata("flutter.material", 100, 200, "Switch"), definition().palette());
        assertTrue(definition().constConstructor());
        assertTrue(definition().slots().isEmpty());
        assertTrue(definition().traits().isEmpty());
        for (int index = 0; index < 201; index++) {
            var property = definition().properties().get(index);
            assertEquals(index, property.parameter().order());
            assertEquals(index, SwitchWidgetPropertySchema.find(property.name()).orElseThrow().dartOrder());
            assertEquals(Set.of("value", "variant", "enabled").contains(property.name().value()), property.parameter().required());
            assertEquals(property.parameter().required(), property.creationDefault().isPresent());
        }
        var prototype = WidgetNodePrototypeFactory.create(definition(), StableId.random());
        assertEquals(3, prototype.properties().size());
        assertEquals(b(false), prototype.properties().get(p("value")));
        assertTrue(valid(prototype));
        assertTrue(WidgetPlacementRules.requiredAnyWidgetWrapperSlot(definition()).isEmpty());
        assertEquals(17, DesignerDocument.SCHEMA_VERSION);
        assertEquals(16, WidgetCatalog.API_VERSION);
        assertEquals(1024, FdCodecLimits.defaults().maxPropertiesPerWidget());
        assertEquals(1024, ValidationLimits.defaults().maxPropertiesPerWidget());
    }

    @Test
    void independentlyReviewedCapabilitiesRejectDomainDriftAndMatchAllRows() {
        assertTrue(BuiltInWidgetCapabilityCatalog.canvasProjection(definition()).isPresent());
        for (String name : List.of("thumbIconDefaultData", "trackOutlineWidthDefault", "padding", "onActiveThumbImageError", "applyCupertinoTheme")) {
            var properties = definition().properties().stream().map(property -> property.name().value().equals(name)
                    ? new PropertyDefinition(property.name(), property.parameter(), List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.STRING)), property.creationDefault())
                    : property).toList();
            var changed = new WidgetDefinition(definition().typeId(), definition().dartClassName(), definition().namedConstructor(),
                    true, definition().dartLibraryUri(), definition().importUris(), definition().traits(), definition().palette(), properties, List.of());
            assertTrue(BuiltInWidgetCapabilityCatalog.canvasProjection(changed).isEmpty(), name);
        }
        String contract = BuiltInWidgetCapabilityCatalog.reviewedCanvasSchemaContract();
        assertTrue(contract.contains("W|flutter.material.Switch\n"));
        assertTrue(contract.contains("P|applyCupertinoTheme|boolean,null|0|-|-|boolean:any;null:any"));
    }

    @Test
    void bothConstructorsAreConstWithDisabledCallbackAndAdaptiveFlagCannotLeakToStandard() throws Exception {
        for (String variant : SwitchWidgetPropertySchema.variants()) {
            var root = node(Map.of(p("variant"), s(variant), p("enabled"), b(false)));
            var output = generated(root);
            assertTrue(output.build().payload().contains("const Switch" + (variant.equals("adaptive") ? ".adaptive" : "") + "("));
            assertFalse(output.build().payload().contains("variant:"));
            assertFalse(output.build().payload().contains("enabled:"));
            assertSymbols(output);
            roundTrip(root);
        }
        for (PropertyValue value : List.of(b(false), b(true), nil())) {
            assertFalse(valid(node(Map.of(p("applyCupertinoTheme"), value))));
            var root = node(Map.of(p("variant"), s("adaptive"), p("applyCupertinoTheme"), value));
            assertTrue(valid(root), new WidgetTreeValidator().validate(document(root), CATALOG).issues().toString());
            assertTrue(generated(root).build().payload().contains("applyCupertinoTheme:"));
        }
        assertFalse(valid(node(Map.of(p("value"), nil()))));
        var prototype = node(Map.of());
        for (String name : List.of("value", "variant", "enabled")) {
            var properties = new LinkedHashMap<>(prototype.properties());
            properties.remove(p(name));
            assertFalse(valid(new WidgetNode(prototype.id(), prototype.type(), properties, Map.of())));
        }
    }

    @Test
    void all201FieldsAcrossDenseLocalAndWholeReferenceFamiliesRoundTripWithExactProvenance() throws Exception {
        Set<PropertyName> covered = new HashSet<>();
        for (String variant : SwitchWidgetPropertySchema.variants()) {
            var properties = full(variant);
            covered.addAll(properties.keySet());
            var root = node(properties);
            assertTrue(valid(root), new WidgetTreeValidator().validate(document(root), CATALOG).issues().toString());
            var output = generated(root);
            assertSymbols(output);
            assertFalse(output.build().payload().contains("thumbIconDefaultData:"));
            assertFalse(output.build().payload().contains("thumbIconDefaultMode:"));
            for (String leaf : List.of("blendMode:", "fontWeight:", "semanticLabel:", "textDirection:", "applyTextScaling:")) {
                assertTrue(output.build().payload().contains(leaf), leaf);
            }
            roundTrip(root);
            var refs = new LinkedHashMap<PropertyName, PropertyValue>();
            refs.put(p("variant"), s(variant));
            for (String family : List.of("thumbColor", "trackColor", "trackOutlineColor", "overlayColor", "trackOutlineWidth", "thumbIcon")) {
                refs.put(p(family), reference(family));
            }
            covered.addAll(refs.keySet());
            assertSymbols(generated(node(refs)));
            roundTrip(node(refs));
        }
        assertEquals(201, covered.size());
    }

    @Test
    void numericMapsUseDownwardInferenceWithoutCoreImportsOrShadowableTypeNames() {
        for (String variant : SwitchWidgetPropertySchema.variants()) {
            for (PropertyValue value : List.of(nil(), d("-2.5"), new PropertyValue.IntegerValue(BigInteger.TWO), new PropertyValue.EnumValue("double", "infinity"))) {
                var output = generated(node(Map.of(p("variant"), s(variant), p("enabled"), b(false),
                        p("trackOutlineWidthDefault"), value, p("trackOutlineWidthSelected"), nil())));
                String source = output.build().payload();
                assertTrue(source.contains("trackOutlineWidth: const WidgetStateProperty.fromMap({"), source);
                assertFalse(source.contains("<double"));
                assertFalse(output.imports().payload().contains("dart:core"));
                assertTrue(output.symbolOccurrences().stream().noneMatch(symbol -> symbol.symbolName().equals("double")));
                assertSymbols(output);
            }
        }
    }

    @Test
    void allColorFamiliesAndExplicitNullWinFirstMatchWithoutInventingDisabledEntries() {
        for (String family : SwitchWidgetPropertySchema.colorFamilies()) {
            var base = generated(node(Map.of(p("enabled"), b(false), p(family + "Default"), color())));
            assertFalse(base.build().payload().contains("WidgetState.disabled"));
            for (String state : SwitchWidgetPropertySchema.statePrefixes().subList(1, 9)) {
                var output = generated(node(Map.of(p("enabled"), b(false), p(family + "Default"), color(), p(family + state), nil())));
                String source = output.build().payload();
                String stateName = Character.toLowerCase(state.charAt(0)) + state.substring(1);
                assertTrue(source.indexOf("WidgetState." + stateName + ": null") < source.indexOf("WidgetState.any:"));
                assertSymbols(output);
            }
            assertFalse(generated(node(Map.of(p("enabled"), b(false), p(family + "Default"), theme())))
                    .build().payload().contains("const Switch("));
        }
    }

    @Test
    void iconNullObjectInheritAndAbsentAreDistinctAndRetainAllThirteenIconFields() {
        for (String state : SwitchWidgetPropertySchema.thumbIconStates()) {
            String prefix = "thumbIcon" + state;
            assertFalse(generated(node(Map.of())).build().payload().contains("thumbIcon:"));
            String emptyIcon = generated(node(Map.of(p(prefix + "Mode"), s("icon")))).build().payload();
            assertTrue(emptyIcon.contains("const Icon(null)"));
            String inherited = generated(node(Map.of(p(prefix + "Mode"), s("inherit")))).build().payload();
            assertFalse(inherited.contains("const Icon(null)"));
            assertTrue(inherited.contains(": null"));
            assertFalse(valid(node(Map.of(p(prefix + "Mode"), s("inherit"), p(prefix + "Data"), value(prefix + "Data")))));
            var leafOnly = generated(node(Map.of(p(prefix + "Size"), d("32"))));
            assertTrue(leafOnly.build().payload().contains("const Icon(null, size: 32.0)"));
            assertSymbols(leafOnly);
        }
    }

    @Test
    void typedFamiliesRejectLocalConflictsAndImageErrorsRequireCorrespondingProviders() {
        for (String family : List.of("thumbColor", "trackColor", "trackOutlineColor", "overlayColor", "trackOutlineWidth", "thumbIcon")) {
            String local = family + "Default" + (family.equals("thumbIcon") ? "Mode" : "");
            assertFalse(valid(node(Map.of(p(family), reference(family), p(local), value(local)))));
            assertTrue(valid(node(Map.of(p(family), reference(family)))));
        }
        for (String state : List.of("Active", "Inactive")) {
            String callback = "on" + state + "ThumbImageError";
            String image = Character.toLowerCase(state.charAt(0)) + state.substring(1) + "ThumbImage";
            assertFalse(valid(node(Map.of(p(callback), reference(callback)))));
            var output = generated(node(Map.of(p(callback), reference(callback), p(image), PropertyValue.ImageProviderValue.unresolved())));
            assertSymbols(output);
            assertTrue(output.symbolOccurrences().stream().anyMatch(symbol -> symbol.modelPath().contains("/" + callback)
                    && symbol.staticTypeRequirement().map(type -> type.expectedDartType().equals("ImageErrorListener")).orElse(false)), output.symbolOccurrences().toString());
        }
    }

    @Test
    void disabledCallbackIsNotImportedOrProvedButFocusCallbackRemainsIndependent() {
        assertTrue(generated(node(Map.of())).build().payload().contains("onChanged: (_) {}"));
        var disabled = generated(node(Map.of(p("enabled"), b(false), p("onChanged"), reference("changed"))));
        assertFalse(disabled.imports().payload().contains("package:buttons"));
        assertTrue(disabled.symbolOccurrences().stream().noneMatch(symbol -> symbol.modelPath().endsWith("/onChanged")));
        var focused = generated(node(Map.of(p("enabled"), b(false), p("onFocusChange"), reference("focusChanged"))));
        assertTrue(focused.build().payload().contains("buttonValues.focusChanged"));
        assertSymbols(focused);
    }

    @Test
    void sameThemeTokenDirectAndIconShadowMaintainSeparateOccurrencesAndConstness() {
        var shadows = new PropertyValue.ShadowListValue(List.of(new PropertyValue.ShadowListValue.Shadow(
                StableId.random(), new ColorSource.Theme(theme().token()), BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.TWO)));
        var output = generated(node(Map.of(p("enabled"), b(false), p("thumbColorDefault"), theme(),
                p("thumbIconDefaultColor"), theme(), p("thumbIconDefaultShadows"), shadows)));
        assertEquals(3, output.symbolOccurrences().stream().filter(symbol -> symbol.symbolName().equals("Theme")).count());
        assertFalse(output.build().payload().contains("const Switch("));
        assertSymbols(output);
    }

    @Test
    void numericIconPaddingAndClosedReferenceDomainsRejectUnsupportedValues() {
        assertTrue(accepts("trackOutlineWidthDefault", d("-10")));
        assertTrue(accepts("trackOutlineWidthDefault", new PropertyValue.EnumValue("double", "infinity")));
        assertTrue(accepts("splashRadius", d("-10")));
        assertTrue(accepts("splashRadius", new PropertyValue.EnumValue("double", "infinity")));
        assertFalse(accepts("trackOutlineWidthDefault", d("1e400")));
        assertFalse(accepts("thumbIconDefaultSize", d("-1")));
        assertFalse(accepts("thumbIconDefaultFill", d("2")));
        assertFalse(accepts("thumbIconDefaultWeight", d("32768")));
        assertFalse(accepts("padding", new PropertyValue.EdgeInsetsValue(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE.negate())));
        assertFalse(accepts("padding", new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ONE.negate(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)));
        for (String name : List.of("key", "child", "semanticLabel", "constraints", "onTap", "applyTheme")) {
            assertFalse(valid(node(Map.of(p(name), b(true)))));
        }
        for (String name : SwitchWidgetPropertySchema.definitions().keySet()) {
            assertFalse(accepts(name, new PropertyValue.DartExpressionValue("arbitrary()")), name);
        }
    }

    @Test
    void allTypedProviderBranchesInBothImageFieldsKeepExactProvenanceAndRoundTrips() throws Exception {
        var providers = new ArrayList<PropertyValue.ImageProviderValue>();
        providers.add(PropertyValue.ImageProviderValue.unresolved());
        for (var kind : PropertyValue.ImageProviderValue.ProviderKind.values()) {
            for (boolean packaged : List.of(false, true)) {
                var resizes = new ArrayList<Optional<PropertyValue.ImageProviderValue.ResizeImageConfig>>();
                resizes.add(Optional.empty());
                for (int dimensions = 1; dimensions <= 3; dimensions++) {
                    for (var policy : PropertyValue.ImageProviderValue.ResizePolicy.values()) {
                        for (boolean upscale : List.of(false, true)) {
                            resizes.add(Optional.of(new PropertyValue.ImageProviderValue.ResizeImageConfig(
                                    (dimensions & 1) != 0 ? Optional.of(1) : Optional.empty(),
                                    (dimensions & 2) != 0 ? Optional.of(16384) : Optional.empty(), policy, upscale)));
                        }
                    }
                }
                for (var resize : resizes) {
                    providers.add(new PropertyValue.ImageProviderValue(kind, "assets/icon.png",
                            packaged ? Optional.of("reviewed_icons") : Optional.empty(),
                            kind == PropertyValue.ImageProviderValue.ProviderKind.EXACT_ASSET
                                    ? Optional.of(new BigDecimal("2.5")) : Optional.empty(), resize));
                }
            }
        }
        assertEquals(53, providers.size());
        for (var provider : providers) {
            for (String variant : SwitchWidgetPropertySchema.variants()) {
                var root = node(Map.of(p("variant"), s(variant), p("activeThumbImage"), provider, p("inactiveThumbImage"), provider,
                        p("onActiveThumbImageError"), reference("activeError"), p("onInactiveThumbImageError"), reference("inactiveError")));
                var output = generated(root);
                assertSymbols(output);
                assertEquals(2, output.symbolOccurrences().stream().filter(symbol -> symbol.staticTypeRequirement()
                        .map(requirement -> requirement.expectedDartType().equals("ImageErrorListener")).orElse(false)).count());
                roundTrip(root);
            }
        }
        assertFalse(accepts("activeThumbImage", nil()));
        assertFalse(accepts("inactiveThumbImage", s("https://example.com/unreviewed.png")));
    }

    @Test
    void allCursorPresetsAndConstructorIconMapAliasCollisionsHaveExactTokenEvidence() {
        for (String cursor : SwitchWidgetPropertySchema.mouseCursorPresets()) {
            var output = generated(node(Map.of(p("mouseCursor"), s(cursor))));
            assertSymbols(output);
        }
        for (String collision : List.of("Switch", "WidgetStateProperty", "Icon", "WidgetState", "Color")) {
            var reference = new PropertyValue.DartObjectReferenceValue(Optional.empty(), collision, Optional.of("changed"),
                    PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
            var root = node(Map.of(p("variant"), s("adaptive"), p("thumbIconDefaultData"), value("thumbIconDefaultData"),
                    p("thumbColorDefault"), color(), p("trackOutlineWidthDefault"), d("2"), p("onChanged"), reference));
            var output = generated(root);
            assertSymbols(output);
            assertTrue(output.symbolOccurrences().stream().anyMatch(symbol -> symbol.symbolName().equals("adaptive")));
            assertTrue(output.symbolOccurrences().stream().anyMatch(symbol -> symbol.symbolName().equals("fromMap")));
        }
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
        assertEquals(document(root), assertInstanceOf(FdDecodeResult.Current.class, codec.decode(codec.encode(document(root)))).document());
    }

    private static void assertSymbols(GeneratedDartRegions output) {
        String source = output.build().payload();
        for (var symbol : output.symbolOccurrences()) {
            assertEquals(symbol.symbolName(), source.substring(symbol.offset(), symbol.endOffset()), symbol.toString());
        }
        assertEquals(output.symbolOccurrences().size(), output.symbolOccurrences().stream().map(GeneratedDartSymbolOccurrence::id).distinct().count());
    }
}
