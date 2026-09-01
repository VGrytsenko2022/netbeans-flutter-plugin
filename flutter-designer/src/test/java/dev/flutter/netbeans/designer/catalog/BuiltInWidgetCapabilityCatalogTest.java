package dev.flutter.netbeans.designer.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.SlotCardinality;
import dev.flutter.netbeans.designer.model.SlotName;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BuiltInWidgetCapabilityCatalogTest {
    private static final List<String> CANVAS_ORDER = List.of(
            "flutter.material.Scaffold",
            "flutter.material.AppBar",
            "flutter.material.ElevatedButton",
            "flutter.widgets.Column",
            "flutter.widgets.Row",
            "flutter.widgets.Padding",
            "flutter.widgets.Center",
            "flutter.widgets.SizedBox",
            "flutter.widgets.AspectRatio",
            "flutter.widgets.Container",
            "flutter.widgets.Opacity",
            "flutter.widgets.Align",
            "flutter.widgets.FractionallySizedBox",
            "flutter.widgets.Text",
            "flutter.widgets.Icon");

    private static final List<String> PROPERTIES_ORDER = List.of(
            "flutter.material.Scaffold",
            "flutter.material.AppBar",
            "flutter.material.ElevatedButton",
            "flutter.widgets.Column",
            "flutter.widgets.Row",
            "flutter.widgets.Padding",
            "flutter.widgets.Center",
            "flutter.widgets.SizedBox",
            "flutter.widgets.AspectRatio",
            "flutter.widgets.Container",
            "flutter.widgets.Opacity",
            "flutter.widgets.Align",
            "flutter.widgets.FractionallySizedBox",
            "flutter.widgets.Text",
            "flutter.widgets.Icon");

    @Test
    void exposesTheExactReviewedInteractiveSurfacesInPaletteOrder() {
        assertEquals(CANVAS_ORDER, types(WidgetCapability.CANVAS));
        assertEquals(CANVAS_ORDER, types(WidgetCapability.CREATE));
        assertEquals(CANVAS_ORDER, types(WidgetCapability.DND));
        assertEquals(PROPERTIES_ORDER, types(WidgetCapability.PROPERTIES));
    }

    @Test
    void exactDndCapabilityMatrixHasFifteenSourcesAndNineteenDestinations() {
        List<WidgetDefinition> sources =
                BuiltInWidgetCapabilityCatalog.definitionsSupporting(
                        WidgetCapability.DND);
        List<SlotDefinition> destinations =
                BuiltInWidgetCapabilityCatalog.definitionsSupporting(
                                WidgetCapability.DND).stream()
                        .flatMap(definition -> definition.slots().stream())
                        .toList();

        long accepted = 0;
        for (WidgetDefinition source : sources) {
            for (SlotDefinition destination : destinations) {
                if (destination.acceptance().accepts(source)) {
                    accepted++;
                }
            }
        }
        long candidates = (long) sources.size() * destinations.size();

        assertEquals(15, sources.size());
        assertEquals(19, destinations.size());
        assertEquals(285, candidates);
        assertEquals(257, accepted);
        assertEquals(28, candidates - accepted);
    }

    @Test
    void canvasProjectionExactlyMatchesEveryReviewedCanonicalSchema() {
        for (WidgetDefinition definition
                : BuiltInWidgetCapabilityCatalog.definitionsSupporting(
                        WidgetCapability.CANVAS)) {
            var projection = BuiltInWidgetCapabilityCatalog
                    .canvasProjection(definition).orElseThrow();
            assertEquals(
                    definition.properties().stream().collect(
                            java.util.stream.Collectors.toMap(
                                    PropertyDefinition::name,
                                    PropertyDefinition::acceptedKinds)),
                    projection.properties(), definition.typeId().value());
            assertEquals(
                    definition.slots().stream().map(SlotDefinition::name)
                            .collect(java.util.stream.Collectors.toSet()),
                    projection.slots(), definition.typeId().value());
        }
    }

    @Test
    void elevatedButtonIsFullyReviewedAndProjected() {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(ElevatedButtonWidgetPropertySchema.ELEVATED_BUTTON_TYPE)
                .orElseThrow();
        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(ElevatedButtonWidgetPropertySchema.FLATTENED_PROPERTY_COUNT,
                projection.propertyContracts().size());
        assertEquals(Set.of(new SlotName("child")), projection.slots());
    }

    @Test
    void aspectRatioHasTheExactStaticEditableCapabilityAndIndependentProjection() {
        WidgetDefinition definition = definition("flutter.widgets.AspectRatio");

        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(Set.of(new PropertyName("aspectRatio")),
                projection.properties().keySet());
        assertEquals(Set.of(new SlotName("child")), projection.slots());

        var ratio = projection.propertyContracts().get(
                new PropertyName("aspectRatio"));
        assertTrue(ratio.required());
        assertEquals(Set.of(PropertyValueKind.DOUBLE), ratio.acceptedKinds());
        assertEquals(Optional.of("double:1"),
                ratio.creationDefaultFingerprint());
        assertEquals("0:0:*:1", ratio.numericBounds()
                .get(PropertyValueKind.DOUBLE).fingerprint());
        assertEquals("range:0:0:*:1", ratio.constraintFingerprints()
                .get(PropertyValueKind.DOUBLE));
        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.SINGLE, false, 0, 1),
                projection.slotContracts().get(new SlotName("child")));
    }

    @Test
    void opacityHasTheExactStaticEditableCapabilityAndIndependentProjection() {
        WidgetDefinition definition = definition("flutter.widgets.Opacity");

        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(Set.of(
                        new PropertyName("opacity"),
                        new PropertyName("alwaysIncludeSemantics")),
                projection.properties().keySet());
        assertEquals(Set.of(new SlotName("child")), projection.slots());

        var alpha = projection.propertyContracts().get(new PropertyName("opacity"));
        assertTrue(alpha.required());
        assertEquals(Set.of(PropertyValueKind.DOUBLE), alpha.acceptedKinds());
        assertEquals(Optional.of("double:1"), alpha.creationDefaultFingerprint());
        assertEquals("0:1:1:1", alpha.numericBounds()
                .get(PropertyValueKind.DOUBLE).fingerprint());
        assertEquals("range:0:1:1:1", alpha.constraintFingerprints()
                .get(PropertyValueKind.DOUBLE));

        var semantics = projection.propertyContracts().get(
                new PropertyName("alwaysIncludeSemantics"));
        assertFalse(semantics.required());
        assertEquals(Set.of(PropertyValueKind.BOOLEAN), semantics.acceptedKinds());
        assertTrue(semantics.creationDefaultFingerprint().isEmpty());
        assertEquals("any", semantics.constraintFingerprints()
                .get(PropertyValueKind.BOOLEAN));
        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.SINGLE, false, 0, 1),
                projection.slotContracts().get(new SlotName("child")));
    }

    @Test
    void alignHasTheExactStaticEditableCapabilityAndIndependentProjection() {
        WidgetDefinition definition = definition("flutter.widgets.Align");

        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(Set.of(
                        new PropertyName("alignment"),
                        new PropertyName("widthFactor"),
                        new PropertyName("heightFactor")),
                projection.properties().keySet());
        assertEquals(Set.of(new SlotName("child")), projection.slots());

        var alignment = projection.propertyContracts().get(
                new PropertyName("alignment"));
        assertFalse(alignment.required());
        assertEquals(Set.of(PropertyValueKind.ALIGNMENT_GEOMETRY),
                alignment.acceptedKinds());
        assertTrue(alignment.creationDefaultFingerprint().isEmpty());
        assertEquals("alignmentGeometry", alignment.constraintFingerprints()
                .get(PropertyValueKind.ALIGNMENT_GEOMETRY));
        assertTrue(alignment.numericBounds().isEmpty());

        for (String name : List.of("widthFactor", "heightFactor")) {
            var factor = projection.propertyContracts().get(new PropertyName(name));
            assertFalse(factor.required(), name);
            assertEquals(Set.of(PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                    factor.acceptedKinds(), name);
            assertTrue(factor.creationDefaultFingerprint().isEmpty(), name);
            assertEquals("0:1:9007199254740991:1", factor.numericBounds()
                    .get(PropertyValueKind.INTEGER).fingerprint(), name);
            assertEquals("0:1:*:1", factor.numericBounds()
                    .get(PropertyValueKind.DOUBLE).fingerprint(), name);
            assertEquals("range:0:1:9007199254740991:1",
                    factor.constraintFingerprints().get(PropertyValueKind.INTEGER), name);
            assertEquals("range:0:1:*:1",
                    factor.constraintFingerprints().get(PropertyValueKind.DOUBLE), name);
        }

        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.SINGLE, false, 0, 1),
                projection.slotContracts().get(new SlotName("child")));
    }

    @Test
    void fractionallySizedBoxHasTheExactStaticEditableCapabilityAndIndependentProjection() {
        WidgetDefinition definition = definition(
                "flutter.widgets.FractionallySizedBox");

        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(Set.of(
                        new PropertyName("alignment"),
                        new PropertyName("widthFactor"),
                        new PropertyName("heightFactor")),
                projection.properties().keySet());
        assertEquals(Set.of(new SlotName("child")), projection.slots());

        var alignment = projection.propertyContracts().get(
                new PropertyName("alignment"));
        assertFalse(alignment.required());
        assertEquals(Set.of(PropertyValueKind.ALIGNMENT_GEOMETRY),
                alignment.acceptedKinds());
        assertTrue(alignment.creationDefaultFingerprint().isEmpty());
        assertEquals("alignmentGeometry", alignment.constraintFingerprints()
                .get(PropertyValueKind.ALIGNMENT_GEOMETRY));
        assertTrue(alignment.numericBounds().isEmpty());

        for (String name : List.of("widthFactor", "heightFactor")) {
            var factor = projection.propertyContracts().get(new PropertyName(name));
            assertFalse(factor.required(), name);
            assertEquals(Set.of(PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                    factor.acceptedKinds(), name);
            assertTrue(factor.creationDefaultFingerprint().isEmpty(), name);
            assertEquals("0:1:9007199254740991:1", factor.numericBounds()
                    .get(PropertyValueKind.INTEGER).fingerprint(), name);
            assertEquals("0:1:*:1", factor.numericBounds()
                    .get(PropertyValueKind.DOUBLE).fingerprint(), name);
            assertEquals("range:0:1:9007199254740991:1",
                    factor.constraintFingerprints().get(PropertyValueKind.INTEGER), name);
            assertEquals("range:0:1:*:1",
                    factor.constraintFingerprints().get(PropertyValueKind.DOUBLE), name);
        }

        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.SINGLE, false, 0, 1),
                projection.slotContracts().get(new SlotName("child")));
    }

    @Test
    void containerHasExactStructuredCanvasContractsAndOptionalChild() {
        WidgetDefinition definition = definition("flutter.widgets.Container");
        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));

        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(ContainerWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT,
                projection.propertyContracts().size());
        assertEquals(Set.of(new SlotName("child")), projection.slots());
        assertEquals("alignmentGeometry", projection.propertyContracts()
                .get(new PropertyName("alignment"))
                .constraintFingerprints().get(PropertyValueKind.ALIGNMENT_GEOMETRY));
        assertEquals("boxConstraints", projection.propertyContracts()
                .get(new PropertyName("constraints"))
                .constraintFingerprints().get(PropertyValueKind.BOX_CONSTRAINTS));
        assertEquals("matrix4", projection.propertyContracts()
                .get(new PropertyName("transform"))
                .constraintFingerprints().get(PropertyValueKind.MATRIX4));
        String decorationFingerprint = projection.propertyContracts()
                .get(new PropertyName("decoration"))
                .constraintFingerprints().get(PropertyValueKind.BOX_DECORATION);
        assertTrue(decorationFingerprint.startsWith(
                "boxDecoration:v2:imageProvider:v1:asset,exactAsset:"));
        assertTrue(decorationFingerprint.contains(
                "resize(1..16384,exact,fit,allowUpscaling)"));
        assertTrue(decorationFingerprint.contains(
                "colorFilter(mode,matrix20,linearToSrgbGamma,srgbToLinearGamma,saturation)"));
        assertTrue(decorationFingerprint.contains(
                ":theme=material.colorScheme.error,"));
        assertTrue(decorationFingerprint.contains(
                "material.colorScheme.surfaceTint"));
        assertEquals("0:1:*:1", projection.propertyContracts()
                .get(new PropertyName("margin")).numericBounds()
                .get(PropertyValueKind.EDGE_INSETS).fingerprint());
        assertTrue(projection.propertyContracts().values().stream()
                .allMatch(value -> value.creationDefaultFingerprint().isEmpty()));
        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.SINGLE, false, 0, 1),
                projection.slotContracts().get(new SlotName("child")));
    }

    @Test
    void scaffoldIsFullyReviewedAndProjectedWithoutExpandingSlots() {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(ScaffoldWidgetPropertySchema.SCAFFOLD_TYPE).orElseThrow();
        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(ScaffoldWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT,
                projection.propertyContracts().size());
        assertEquals(Set.of(
                        new SlotName("appBar"), new SlotName("body"),
                        new SlotName("floatingActionButton")),
                projection.slots());
        assertEquals("callbackReference", projection.propertyContracts()
                .get(new PropertyName("onDrawerChanged"))
                .constraintFingerprints().get(PropertyValueKind.CALLBACK));
        assertEquals("0:1:*:1", projection.propertyContracts()
                .get(new PropertyName("drawerEdgeDragWidth"))
                .numericBounds().get(PropertyValueKind.DOUBLE).fingerprint());
    }

    @Test
    void alteredDefinitionCannotBorrowAReviewedBuiltInTypeId() {
        WidgetDefinition canonical = BuiltInWidgetCatalog.getDefault()
                .find(new dev.flutter.netbeans.designer.model.WidgetTypeId(
                        "flutter.widgets.SizedBox"))
                .orElseThrow();
        WidgetDefinition altered = new WidgetDefinition(
                canonical.typeId(),
                canonical.dartClassName(),
                canonical.namedConstructor(),
                canonical.constConstructor(),
                canonical.dartLibraryUri(),
                canonical.importUris(),
                canonical.traits(),
                new PaletteMetadata(
                        canonical.palette().categoryId(),
                        canonical.palette().categoryOrder(),
                        canonical.palette().itemOrder(),
                        "Altered SizedBox"),
                canonical.properties(),
                canonical.slots());

        assertEquals(Set.of(),
                BuiltInWidgetCapabilityCatalog.capabilities(altered));
        assertFalse(BuiltInWidgetCapabilityCatalog.supports(
                altered, WidgetCapability.CANVAS));
        assertEquals(Optional.empty(),
                BuiltInWidgetCapabilityCatalog.canvasProjection(altered));
    }

    @Test
    void exactCanvasContractsIncludeDefaultsBoundsConstraintsAndSlots() {
        var padding = projection("flutter.widgets.Padding");
        var paddingProperty = padding.propertyContracts().get(
                new PropertyName("padding"));
        assertTrue(paddingProperty.required());
        assertEquals(Optional.of("edgeInsets:16,16,16,16"),
                paddingProperty.creationDefaultFingerprint());
        assertEquals("0:1:*:1", paddingProperty.numericBounds().get(
                PropertyValueKind.EDGE_INSETS).fingerprint());
        assertEquals("edgeInsets:1:0:1:*:1",
                paddingProperty.constraintFingerprints().get(
                        PropertyValueKind.EDGE_INSETS));

        var text = projection("flutter.widgets.Text");
        var data = text.propertyContracts().get(new PropertyName("data"));
        assertTrue(data.required());
        assertEquals(Optional.of("string:VGV4dA"),
                data.creationDefaultFingerprint());
        assertEquals("any", data.constraintFingerprints().get(
                PropertyValueKind.STRING));
        assertTrue(text.propertyContracts().get(new PropertyName("textAlign"))
                .constraintFingerprints().get(PropertyValueKind.ENUM)
                .endsWith(":TextAlign:center,end,justify,left,right,start"));
        assertTrue(text.propertyContracts().get(new PropertyName("styleForeground"))
                .constraintFingerprints().get(PropertyValueKind.PAINT)
                .startsWith("paintTokens:material.colorScheme.error,"));
        assertEquals("fontVariationList", text.propertyContracts()
                .get(new PropertyName("styleFontVariations"))
                .constraintFingerprints().get(
                        PropertyValueKind.FONT_VARIATION_LIST));

        var centerWidth = projection("flutter.widgets.Center")
                .propertyContracts().get(new PropertyName("widthFactor"));
        assertEquals(Set.of(PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                centerWidth.acceptedKinds());
        assertEquals("0:1:9007199254740991:1",
                centerWidth.numericBounds().get(
                        PropertyValueKind.INTEGER).fingerprint());
        assertEquals("0:1:*:1", centerWidth.numericBounds().get(
                PropertyValueKind.DOUBLE).fingerprint());

        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.LIST, false, 0, 10_000),
                projection("flutter.widgets.Column").slotContracts().get(
                        new SlotName("children")));
        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.SINGLE, false, 0, 1),
                projection("flutter.widgets.SizedBox").slotContracts().get(
                        new SlotName("child")));

        var aspectRatio = projection("flutter.widgets.AspectRatio")
                .propertyContracts().get(new PropertyName("aspectRatio"));
        assertTrue(aspectRatio.required());
        assertEquals(Optional.of("double:1"),
                aspectRatio.creationDefaultFingerprint());
        assertEquals("0:0:*:1", aspectRatio.numericBounds()
                .get(PropertyValueKind.DOUBLE).fingerprint());

        var icon = projection("flutter.widgets.Icon");
        var iconData = icon.propertyContracts().get(new PropertyName("icon"));
        assertTrue(iconData.required());
        assertEquals(Set.of(PropertyValueKind.ICON_DATA), iconData.acceptedKinds());
        assertEquals(Optional.of(
                "iconData:58873:TWF0ZXJpYWxJY29ucw:-:0:-"),
                iconData.creationDefaultFingerprint());
        assertEquals(
                "materialIcons:3.44.8:058e0af2c2:8825:"
                + "ba88e3e23962ada6537523aa113811d9719b988412815bf084f50a0aa78137f0",
                iconData.constraintFingerprints().get(PropertyValueKind.ICON_DATA));
        assertEquals("0:1:1:1", icon.propertyContracts()
                .get(new PropertyName("fill")).numericBounds()
                .get(PropertyValueKind.DOUBLE).fingerprint());
        assertEquals("0:0:32768:0", icon.propertyContracts()
                .get(new PropertyName("weight")).numericBounds()
                .get(PropertyValueKind.DOUBLE).fingerprint());
        assertEquals("-32768:1:32768:0", icon.propertyContracts()
                .get(new PropertyName("grade")).numericBounds()
                .get(PropertyValueKind.DOUBLE).fingerprint());
        assertEquals("0:0:32768:0", icon.propertyContracts()
                .get(new PropertyName("opticalSize")).numericBounds()
                .get(PropertyValueKind.DOUBLE).fingerprint());
        assertTrue(icon.slotContracts().isEmpty());

        var appBar = projection("flutter.material.AppBar");
        assertEquals(120, appBar.propertyContracts().size());
        assertEquals(5, appBar.slotContracts().size());
        assertEquals("pattern:KD86ZGVmYXVsdHxkZXB0aFplcm98YWxsKQ",
                appBar.propertyContracts().get(new PropertyName("notificationPredicate"))
                        .constraintFingerprints().get(PropertyValueKind.STRING));
        assertEquals("*:1:*:1", appBar.propertyContracts()
                .get(new PropertyName("shapeSideStrokeAlign"))
                .numericBounds().get(PropertyValueKind.DOUBLE).fingerprint());
        assertEquals("0:0:32768:0", appBar.propertyContracts()
                .get(new PropertyName("iconThemeWeight"))
                .numericBounds().get(PropertyValueKind.DOUBLE).fingerprint());
        String preferredSize = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(BuiltInWidgetCatalog.PREFERRED_SIZE_WIDGET_TRAIT
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals("trait:" + preferredSize,
                appBar.slotContracts().get(new SlotName("bottom"))
                        .acceptanceFingerprint());
        assertEquals("trait:" + preferredSize,
                projection("flutter.material.Scaffold").slotContracts()
                        .get(new SlotName("appBar")).acceptanceFingerprint());

        var elevated = projection("flutter.material.ElevatedButton");
        assertEquals(286, elevated.propertyContracts().size());
        var enabled = elevated.propertyContracts().get(new PropertyName("enabled"));
        assertFalse(enabled.required());
        assertEquals(Optional.of("boolean:true"),
                enabled.creationDefaultFingerprint());
        assertEquals("callbackReference", elevated.propertyContracts()
                .get(new PropertyName("onPressed"))
                .constraintFingerprints().get(PropertyValueKind.CALLBACK));
        assertEquals("-4:1:4:1", elevated.propertyContracts()
                .get(new PropertyName("styleVisualDensityHorizontal"))
                .numericBounds().get(PropertyValueKind.DOUBLE).fingerprint());
        assertEquals("*:1:*:1", elevated.propertyContracts()
                .get(new PropertyName("stylePressedSideStrokeAlign"))
                .numericBounds().get(PropertyValueKind.DOUBLE).fingerprint());
        assertTrue(elevated.propertyContracts()
                .get(new PropertyName("styleTapTargetSize"))
                .constraintFingerprints().get(PropertyValueKind.ENUM)
                .startsWith("enum:cGFja2FnZTpmbHV0dGVyL21hdGVyaWFsLmRhcnQ:"));
        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.SINGLE, true, 0, 1),
                elevated.slotContracts().get(new SlotName("child")));
    }

    @Test
    void canonicalContractExpandsBothEdgeInsetsWireVariants() {
        String contract = BuiltInWidgetCapabilityCatalog
                .reviewedCanvasSchemaContract();
        assertTrue(contract.contains(
                "P|padding|edgeInsets,edgeInsetsDirectional|1|"
                + "edgeInsets:16,16,16,16|"
                + "edgeInsets:0:1:*:1;edgeInsetsDirectional:0:1:*:1|"
                + "edgeInsets:edgeInsets:1:0:1:*:1;"
                + "edgeInsetsDirectional:edgeInsets:1:0:1:*:1\n"));
        assertTrue(contract.contains(
                "P|styleFontFeatures|fontFeatureList|0|-|-|"
                + "fontFeatureList:any\n"));
        assertTrue(contract.contains(
                "S|children|list|0|0|10000|any\n"));
        assertTrue(contract.contains(
                "W|flutter.widgets.Align\n"
                + "P|alignment|alignmentGeometry|0|-|-|"
                + "alignmentGeometry:alignmentGeometry\n"
                + "P|heightFactor|double,integer|0|-|"
                + "double:0:1:*:1;integer:0:1:9007199254740991:1|"
                + "double:range:0:1:*:1;"
                + "integer:range:0:1:9007199254740991:1\n"
                + "P|widthFactor|double,integer|0|-|"
                + "double:0:1:*:1;integer:0:1:9007199254740991:1|"
                + "double:range:0:1:*:1;"
                + "integer:range:0:1:9007199254740991:1\n"
                + "S|child|single|0|0|1|any\n"));
        assertTrue(contract.contains(
                "W|flutter.widgets.FractionallySizedBox\n"
                + "P|alignment|alignmentGeometry|0|-|-|"
                + "alignmentGeometry:alignmentGeometry\n"
                + "P|heightFactor|double,integer|0|-|"
                + "double:0:1:*:1;integer:0:1:9007199254740991:1|"
                + "double:range:0:1:*:1;"
                + "integer:range:0:1:9007199254740991:1\n"
                + "P|widthFactor|double,integer|0|-|"
                + "double:0:1:*:1;integer:0:1:9007199254740991:1|"
                + "double:range:0:1:*:1;"
                + "integer:range:0:1:9007199254740991:1\n"
                + "S|child|single|0|0|1|any\n"));
    }

    @Test
    void appBarFullReviewedProjectionHasStableFingerprint() throws Exception {
        String contract = BuiltInWidgetCapabilityCatalog.reviewedCanvasSchemaContract();
        int start = contract.indexOf("W|flutter.material.AppBar\n");
        int end = contract.indexOf("W|", start + 2);
        String appBar = contract.substring(start, end);

        assertEquals(50_905, appBar.getBytes(StandardCharsets.UTF_8).length);
        assertEquals(
                "efcbcdee37b660a8ec4f8cb85b152aa92009033b6ae498b6c25861d7efbdb3be",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                        .digest(appBar.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void slotAcceptanceFingerprintsAreCanonicalAndFailClosed() {
        String text = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("flutter.widgets.Text".getBytes(StandardCharsets.UTF_8));
        String icon = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("flutter.widgets.Icon".getBytes(StandardCharsets.UTF_8));
        String types = java.util.stream.Stream.of(text, icon).sorted()
                .collect(java.util.stream.Collectors.joining(","));

        assertEquals("types:" + types,
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.LIST, false, 0, 10,
                        "types:" + types).acceptanceFingerprint());
        assertThrows(IllegalArgumentException.class,
                () -> new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.SINGLE, false, 0, 1, "trait:Zg=="));
        assertThrows(IllegalArgumentException.class,
                () -> new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.SINGLE, false, 0, 1, "trait:_w"));
        assertThrows(IllegalArgumentException.class,
                () -> new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.LIST, false, 0, 10,
                        "types:" + String.join(",", types.split(",")[1],
                                types.split(",")[0])));
        assertThrows(IllegalArgumentException.class,
                () -> new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.LIST, false, 0, 10,
                        "types:" + text + ',' + text));
        assertThrows(IllegalArgumentException.class,
                () -> new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.LIST, false, 0, 10, "types:"));
    }

    @Test
    void belowTypeSchemaDriftFailsClosed() {
        WidgetDefinition center = definition("flutter.widgets.Center");
        var projection = projection("flutter.widgets.Center");
        LinkedHashMap<PropertyName,
                BuiltInWidgetCapabilityCatalog.CanvasPropertyContract> properties =
                new LinkedHashMap<>(projection.propertyContracts());
        var current = properties.get(new PropertyName("widthFactor"));
        var integerBound = new BuiltInWidgetCapabilityCatalog.CanvasNumericBounds(
                BigDecimal.ZERO, true, BigDecimal.TEN, true);
        var doubleBound = current.numericBounds().get(PropertyValueKind.DOUBLE);
        properties.put(new PropertyName("widthFactor"),
                new BuiltInWidgetCapabilityCatalog.CanvasPropertyContract(
                        current.acceptedKinds(), current.required(),
                        current.creationDefaultFingerprint(),
                        Map.of(
                                PropertyValueKind.INTEGER, integerBound,
                                PropertyValueKind.DOUBLE, doubleBound),
                        Map.of(
                                PropertyValueKind.INTEGER,
                                "range:" + integerBound.fingerprint(),
                                PropertyValueKind.DOUBLE,
                                "range:" + doubleBound.fingerprint())));
        var drifted = BuiltInWidgetCapabilityCatalog.CanvasProjection.of(
                properties, projection.slotContracts());

        assertThrows(ExceptionInInitializerError.class,
                () -> BuiltInWidgetCapabilityCatalog.requireCanvasSchemaParity(
                        center, drifted));

        WidgetDefinition column = definition("flutter.widgets.Column");
        var columnProjection = projection("flutter.widgets.Column");
        LinkedHashMap<SlotName,
                BuiltInWidgetCapabilityCatalog.CanvasSlotContract> slots =
                new LinkedHashMap<>(columnProjection.slotContracts());
        slots.put(new SlotName("children"),
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.LIST, false, 0, 9_999));
        var slotDrifted = BuiltInWidgetCapabilityCatalog.CanvasProjection.of(
                columnProjection.propertyContracts(), slots);
        assertThrows(ExceptionInInitializerError.class,
                () -> BuiltInWidgetCapabilityCatalog.requireCanvasSchemaParity(
                        column, slotDrifted));

        WidgetDefinition scaffold = definition("flutter.material.Scaffold");
        var scaffoldProjection = projection("flutter.material.Scaffold");
        slots = new LinkedHashMap<>(scaffoldProjection.slotContracts());
        var appBarSlot = slots.get(new SlotName("appBar"));
        slots.put(new SlotName("appBar"),
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        appBarSlot.cardinality(), appBarSlot.required(),
                        appBarSlot.minimumChildren(), appBarSlot.maximumChildren(),
                        "any"));
        var acceptanceDrifted = BuiltInWidgetCapabilityCatalog.CanvasProjection.of(
                scaffoldProjection.propertyContracts(), slots);
        assertThrows(ExceptionInInitializerError.class,
                () -> BuiltInWidgetCapabilityCatalog.requireCanvasSchemaParity(
                        scaffold, acceptanceDrifted));

        WidgetDefinition text = definition("flutter.widgets.Text");
        var textProjection = projection("flutter.widgets.Text");
        LinkedHashMap<PropertyName,
                BuiltInWidgetCapabilityCatalog.CanvasPropertyContract> textProperties =
                new LinkedHashMap<>(textProjection.propertyContracts());
        var data = textProperties.get(new PropertyName("data"));
        textProperties.put(new PropertyName("data"),
                new BuiltInWidgetCapabilityCatalog.CanvasPropertyContract(
                        data.acceptedKinds(), data.required(),
                        Optional.of("string:RHJpZnRlZA"), data.numericBounds(),
                        data.constraintFingerprints()));
        var defaultDrifted = BuiltInWidgetCapabilityCatalog.CanvasProjection.of(
                textProperties, textProjection.slotContracts());
        assertThrows(ExceptionInInitializerError.class,
                () -> BuiltInWidgetCapabilityCatalog.requireCanvasSchemaParity(
                        text, defaultDrifted));

        var align = textProperties.get(new PropertyName("textAlign"));
        textProperties = new LinkedHashMap<>(textProjection.propertyContracts());
        textProperties.put(new PropertyName("textAlign"),
                new BuiltInWidgetCapabilityCatalog.CanvasPropertyContract(
                        align.acceptedKinds(), align.required(),
                        align.creationDefaultFingerprint(), align.numericBounds(),
                        Map.of(PropertyValueKind.ENUM,
                                "enum:drifted:TextAlign:start")));
        var constraintDrifted = BuiltInWidgetCapabilityCatalog.CanvasProjection.of(
                textProperties, textProjection.slotContracts());
        assertThrows(ExceptionInInitializerError.class,
                () -> BuiltInWidgetCapabilityCatalog.requireCanvasSchemaParity(
                        text, constraintDrifted));
    }

    private static WidgetDefinition definition(String type) {
        return BuiltInWidgetCatalog.getDefault().find(
                new dev.flutter.netbeans.designer.model.WidgetTypeId(type))
                .orElseThrow();
    }

    private static BuiltInWidgetCapabilityCatalog.CanvasProjection projection(
            String type) {
        return BuiltInWidgetCapabilityCatalog.canvasProjection(definition(type))
                .orElseThrow();
    }

    private static List<String> types(WidgetCapability capability) {
        return BuiltInWidgetCapabilityCatalog.definitionsSupporting(capability)
                .stream().map(definition -> definition.typeId().value()).toList();
    }
}
