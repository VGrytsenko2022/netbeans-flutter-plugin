package dev.flutter.netbeans.designer.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
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
            "flutter.material.TextField",
            "flutter.material.Divider",
            "flutter.material.VerticalDivider",
            "flutter.material.Card",
            "flutter.material.Badge",
            "flutter.material.CircleAvatar",
            "flutter.material.LinearProgressIndicator",
            "flutter.material.CircularProgressIndicator",
            "flutter.material.RefreshProgressIndicator",
            "flutter.material.RefreshIndicator",
            "flutter.material.TextButton",
            "flutter.widgets.Column",
            "flutter.widgets.Row",
            "flutter.widgets.Wrap",
            "flutter.widgets.Padding",
            "flutter.widgets.Center",
            "flutter.widgets.SizedBox",
            "flutter.widgets.AspectRatio",
            "flutter.widgets.Container",
            "flutter.widgets.Opacity",
            "flutter.widgets.Align",
            "flutter.widgets.FractionallySizedBox",
            "flutter.widgets.FittedBox",
            "flutter.widgets.ConstrainedBox",
            "flutter.widgets.UnconstrainedBox",
            "flutter.widgets.LimitedBox",
            "flutter.widgets.OverflowBox",
            "flutter.widgets.Stack",
            "flutter.widgets.IndexedStack",
            "flutter.widgets.Expanded",
            "flutter.widgets.Flexible",
            "flutter.widgets.Spacer",
            "flutter.widgets.Baseline",
            "flutter.widgets.IntrinsicHeight",
            "flutter.widgets.IntrinsicWidth",
            "flutter.widgets.Offstage",
            "flutter.widgets.SizedOverflowBox",
            "flutter.widgets.Transform",
            "flutter.widgets.RotatedBox",
            "flutter.widgets.ListBody",
            "flutter.widgets.OverflowBar",
            "flutter.widgets.SafeArea",
            "flutter.widgets.ListView",
            "flutter.widgets.GridView",
            "flutter.widgets.SingleChildScrollView",
            "flutter.widgets.Text",
            "flutter.widgets.Icon",
            "flutter.widgets.Image",
            "flutter.widgets.ColoredBox",
            "flutter.widgets.Placeholder",
            "flutter.widgets.Directionality",
            "flutter.widgets.DecoratedBox",
            "flutter.widgets.ClipRect",
            "flutter.widgets.ClipOval",
            "flutter.widgets.ClipRRect",
            "flutter.widgets.ClipPath",
            "flutter.widgets.ClipRSuperellipse",
            "flutter.widgets.PhysicalModel",
            "flutter.widgets.PhysicalShape",
            "flutter.widgets.RepaintBoundary",
            "flutter.widgets.IgnorePointer",
            "flutter.widgets.AbsorbPointer",
            "flutter.widgets.Visibility",
            "flutter.widgets.TickerMode",
            "flutter.widgets.DefaultTextHeightBehavior",
            "flutter.widgets.DefaultSelectionStyle",
            "flutter.widgets.IconTheme",
            "flutter.widgets.ImageIcon",
            "flutter.widgets.ExcludeSemantics",
            "flutter.widgets.BlockSemantics",
            "flutter.widgets.MergeSemantics",
            "flutter.widgets.IndexedSemantics",
            "flutter.widgets.ExcludeFocus",
            "flutter.widgets.ExcludeFocusTraversal");

    private static final List<String> PROPERTIES_ORDER = List.of(
            "flutter.material.Scaffold",
            "flutter.material.AppBar",
            "flutter.material.ElevatedButton",
            "flutter.material.TextField",
            "flutter.material.Divider",
            "flutter.material.VerticalDivider",
            "flutter.material.Card",
            "flutter.material.Badge",
            "flutter.material.CircleAvatar",
            "flutter.material.LinearProgressIndicator",
            "flutter.material.CircularProgressIndicator",
            "flutter.material.RefreshProgressIndicator",
            "flutter.material.RefreshIndicator",
            "flutter.material.TextButton",
            "flutter.widgets.Column",
            "flutter.widgets.Row",
            "flutter.widgets.Wrap",
            "flutter.widgets.Padding",
            "flutter.widgets.Center",
            "flutter.widgets.SizedBox",
            "flutter.widgets.AspectRatio",
            "flutter.widgets.Container",
            "flutter.widgets.Opacity",
            "flutter.widgets.Align",
            "flutter.widgets.FractionallySizedBox",
            "flutter.widgets.FittedBox",
            "flutter.widgets.ConstrainedBox",
            "flutter.widgets.UnconstrainedBox",
            "flutter.widgets.LimitedBox",
            "flutter.widgets.OverflowBox",
            "flutter.widgets.Stack",
            "flutter.widgets.IndexedStack",
            "flutter.widgets.Expanded",
            "flutter.widgets.Flexible",
            "flutter.widgets.Spacer",
            "flutter.widgets.Baseline",
            "flutter.widgets.IntrinsicWidth",
            "flutter.widgets.Offstage",
            "flutter.widgets.SizedOverflowBox",
            "flutter.widgets.Transform",
            "flutter.widgets.RotatedBox",
            "flutter.widgets.ListBody",
            "flutter.widgets.OverflowBar",
            "flutter.widgets.SafeArea",
            "flutter.widgets.ListView",
            "flutter.widgets.GridView",
            "flutter.widgets.SingleChildScrollView",
            "flutter.widgets.Text",
            "flutter.widgets.Icon",
            "flutter.widgets.Image",
            "flutter.widgets.ColoredBox",
            "flutter.widgets.Placeholder",
            "flutter.widgets.Directionality",
            "flutter.widgets.DecoratedBox",
            "flutter.widgets.ClipRect",
            "flutter.widgets.ClipOval",
            "flutter.widgets.ClipRRect",
            "flutter.widgets.ClipPath",
            "flutter.widgets.ClipRSuperellipse",
            "flutter.widgets.PhysicalModel",
            "flutter.widgets.PhysicalShape",
            "flutter.widgets.IgnorePointer",
            "flutter.widgets.AbsorbPointer",
            "flutter.widgets.Visibility",
            "flutter.widgets.TickerMode",
            "flutter.widgets.DefaultTextHeightBehavior",
            "flutter.widgets.DefaultSelectionStyle",
            "flutter.widgets.IconTheme",
            "flutter.widgets.ImageIcon",
            "flutter.widgets.ExcludeSemantics",
            "flutter.widgets.BlockSemantics",
            "flutter.widgets.IndexedSemantics",
            "flutter.widgets.ExcludeFocus",
            "flutter.widgets.ExcludeFocusTraversal");

    @Test
    void exposesTheExactReviewedInteractiveSurfacesInPaletteOrder() {
        assertEquals(CANVAS_ORDER, types(WidgetCapability.CANVAS));
        assertEquals(CANVAS_ORDER, types(WidgetCapability.CREATE));
        assertEquals(CANVAS_ORDER, types(WidgetCapability.DND));
        assertEquals(PROPERTIES_ORDER, types(WidgetCapability.PROPERTIES));
    }

    @Test
    void exactDndCapabilityMatrixHasSeventySevenSourcesAndSixtyTwoInsertableDestinations() {
        List<WidgetDefinition> sources =
                BuiltInWidgetCapabilityCatalog.definitionsSupporting(
                        WidgetCapability.DND);
        List<Destination> destinations =
                BuiltInWidgetCapabilityCatalog.definitionsSupporting(
                                WidgetCapability.DND).stream()
                        .flatMap(definition -> definition.slots().stream()
                                .filter(slot -> slot.minChildren() == 0)
                                .map(slot -> new Destination(definition, slot)))
                        .toList();

        long accepted = 0;
        for (WidgetDefinition source : sources) {
            for (Destination destination : destinations) {
                if (WidgetPlacementRules.accepts(
                        destination.owner(), destination.slot(), source)) {
                    accepted++;
                }
            }
        }
        long candidates = (long) sources.size() * destinations.size();

        assertEquals(77, sources.size());
        assertEquals(62, destinations.size());
        assertEquals(60, destinations.stream()
                .filter(destination -> destination.slot().acceptance()
                        instanceof SlotAcceptance.AnyWidget)
                .count());
        assertEquals(2, destinations.stream()
                .filter(destination -> destination.slot().acceptance()
                        instanceof SlotAcceptance.HasTrait)
                .count());
        assertEquals(4774, candidates);
        assertEquals(4448, accepted);
        assertEquals(326, candidates - accepted);
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
    void coloredBoxHasExactStaticEditableCapabilityAndIndependentProjection() {
        WidgetDefinition definition = definition("flutter.widgets.ColoredBox");

        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(2, projection.propertyContracts().size());
        assertEquals(Set.of(new SlotName("child")), projection.slots());

        var color = projection.propertyContracts().get(new PropertyName("color"));
        assertTrue(color.required());
        assertEquals(Set.of(PropertyValueKind.COLOR, PropertyValueKind.THEME_TOKEN),
                color.acceptedKinds());
        assertEquals(Optional.of("color:0xFF2196F3"),
                color.creationDefaultFingerprint());
        assertEquals("any",
                color.constraintFingerprints().get(PropertyValueKind.COLOR));
        assertTrue(color.constraintFingerprints().get(PropertyValueKind.THEME_TOKEN)
                .startsWith("tokens:material.colorScheme."));

        var antiAlias = projection.propertyContracts()
                .get(new PropertyName("isAntiAlias"));
        assertFalse(antiAlias.required());
        assertEquals(Set.of(PropertyValueKind.BOOLEAN), antiAlias.acceptedKinds());
        assertTrue(antiAlias.creationDefaultFingerprint().isEmpty());

        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.SINGLE, false, 0, 1),
                projection.slotContracts().get(new SlotName("child")));

        String contract = BuiltInWidgetCapabilityCatalog.reviewedCanvasSchemaContract();
        assertTrue(contract.contains(
                "W|flutter.widgets.ColoredBox\n"
                + "P|color|color,themeToken|1|color:0xFF2196F3|-|"
                + "color:any;themeToken:tokens:material.colorScheme."), contract);
        assertTrue(contract.contains(
                "P|isAntiAlias|boolean|0|-|-|boolean:any\n"
                + "S|child|single|0|0|1|any\n"), contract);
    }

    @Test
    void placeholderHasExactOptionalDefaultsNumericBoundsAndChildProjection() {
        WidgetDefinition definition = definition("flutter.widgets.Placeholder");

        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(PlaceholderWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT,
                projection.propertyContracts().size());
        assertEquals(Set.of(new SlotName("child")), projection.slots());

        var color = projection.propertyContracts().get(new PropertyName("color"));
        assertFalse(color.required());
        assertEquals(Set.of(PropertyValueKind.COLOR, PropertyValueKind.THEME_TOKEN),
                color.acceptedKinds());
        assertTrue(color.creationDefaultFingerprint().isEmpty());
        assertEquals("any",
                color.constraintFingerprints().get(PropertyValueKind.COLOR));
        assertTrue(color.constraintFingerprints().get(PropertyValueKind.THEME_TOKEN)
                .startsWith("tokens:material.colorScheme."));

        for (String name : List.of(
                "strokeWidth", "fallbackWidth", "fallbackHeight")) {
            var numeric = projection.propertyContracts().get(new PropertyName(name));
            assertFalse(numeric.required(), name);
            assertEquals(Set.of(PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                    numeric.acceptedKinds(), name);
            assertTrue(numeric.creationDefaultFingerprint().isEmpty(), name);
            assertEquals("0:1:9007199254740991:1",
                    numeric.numericBounds().get(PropertyValueKind.INTEGER)
                            .fingerprint(), name);
            assertEquals("0:1:*:1",
                    numeric.numericBounds().get(PropertyValueKind.DOUBLE)
                            .fingerprint(), name);
        }

        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.SINGLE, false, 0, 1),
                projection.slotContracts().get(new SlotName("child")));
        String contract = BuiltInWidgetCapabilityCatalog.reviewedCanvasSchemaContract();
        assertTrue(contract.contains(
                "W|flutter.widgets.Placeholder\n"
                + "P|color|color,themeToken|0|-|-|"
                + "color:any;themeToken:tokens:material.colorScheme."), contract);
        assertTrue(contract.contains(
                "P|strokeWidth|double,integer|0|-|"
                + "double:0:1:*:1;integer:0:1:9007199254740991:1|"), contract);
        assertTrue(contract.contains("S|child|single|0|0|1|any\n"), contract);
    }

    @Test
    void safeAreaHasPhysicalInsetsRequiredChildAndAtomicWrapperFingerprint() {
        WidgetDefinition definition = definition("flutter.widgets.SafeArea");

        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(6, projection.propertyContracts().size());
        assertEquals(Set.of(new SlotName("child")), projection.slots());

        for (String name : List.of(
                "left", "top", "right", "bottom",
                "maintainBottomViewPadding")) {
            var property = projection.propertyContracts().get(new PropertyName(name));
            assertFalse(property.required(), name);
            assertEquals(Set.of(PropertyValueKind.BOOLEAN),
                    property.acceptedKinds(), name);
            assertTrue(property.creationDefaultFingerprint().isEmpty(), name);
        }
        var minimum = projection.propertyContracts()
                .get(new PropertyName("minimum"));
        assertFalse(minimum.required());
        assertEquals(Set.of(PropertyValueKind.EDGE_INSETS), minimum.acceptedKinds());
        assertEquals("*:1:*:1", minimum.numericBounds()
                .get(PropertyValueKind.EDGE_INSETS).fingerprint());
        assertEquals("edgeInsetsPhysical:0:*:1:*:1",
                minimum.constraintFingerprints().get(PropertyValueKind.EDGE_INSETS));

        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.SINGLE, true, 1, 1),
                projection.slotContracts().get(new SlotName("child")));

        String contract = BuiltInWidgetCapabilityCatalog.reviewedCanvasSchemaContract();
        assertTrue(contract.contains(
                "W|flutter.widgets.SafeArea\n"
                + "P|bottom|boolean|0|-|-|boolean:any\n"
                + "P|left|boolean|0|-|-|boolean:any\n"
                + "P|maintainBottomViewPadding|boolean|0|-|-|boolean:any\n"
                + "P|minimum|edgeInsets|0|-|edgeInsets:*:1:*:1|"
                + "edgeInsets:edgeInsetsPhysical:0:*:1:*:1\n"
                + "P|right|boolean|0|-|-|boolean:any\n"
                + "P|top|boolean|0|-|-|boolean:any\n"
                + "S|child|single|1|1|1|any\n"
                + "C|flutter.widgets.SafeArea|paletteCreate|"
                + "wrapExistingChild|child\n"), contract);
        assertFalse(contract.contains(
                "W|flutter.widgets.SafeArea\nP|minimum|edgeInsetsDirectional"), contract);
    }

    @Test
    void directionalityHasRequiredEnumChildAndAtomicWrapperFingerprint() {
        WidgetDefinition definition = definition("flutter.widgets.Directionality");

        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(1, projection.propertyContracts().size());
        assertEquals(Set.of(new SlotName("child")), projection.slots());

        var direction = projection.propertyContracts()
                .get(new PropertyName("textDirection"));
        assertTrue(direction.required());
        assertEquals(Set.of(PropertyValueKind.ENUM), direction.acceptedKinds());
        assertEquals(Optional.of("enum:TextDirection:ltr"),
                direction.creationDefaultFingerprint());
        assertEquals(
                "enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:"
                + "TextDirection:ltr,rtl",
                direction.constraintFingerprints().get(PropertyValueKind.ENUM));
        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.SINGLE, true, 1, 1),
                projection.slotContracts().get(new SlotName("child")));

        String contract = BuiltInWidgetCapabilityCatalog.reviewedCanvasSchemaContract();
        assertTrue(contract.contains(
                "W|flutter.widgets.Directionality\n"
                + "P|textDirection|enum|1|enum:TextDirection:ltr|-|"
                + "enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:"
                + "TextDirection:ltr,rtl\n"
                + "S|child|single|1|1|1|any\n"
                + "C|flutter.widgets.Directionality|paletteCreate|"
                + "wrapExistingChild|child\n"), contract);
    }

    @Test
    void decoratedBoxHasExactRequiredDecorationPositionAndOptionalChildProjection() {
        WidgetDefinition definition = definition("flutter.widgets.DecoratedBox");

        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(DecoratedBoxWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT,
                projection.propertyContracts().size());
        assertEquals(Set.of(new SlotName("child")), projection.slots());

        var decoration = projection.propertyContracts()
                .get(new PropertyName("decoration"));
        assertTrue(decoration.required());
        assertEquals(Set.of(PropertyValueKind.BOX_DECORATION),
                decoration.acceptedKinds());
        assertEquals(Optional.of("boxDecoration:empty"),
                decoration.creationDefaultFingerprint());
        String decorationConstraint = decoration.constraintFingerprints()
                .get(PropertyValueKind.BOX_DECORATION);
        assertTrue(decorationConstraint.startsWith(
                "boxDecoration:v2:imageProvider:v1:asset,exactAsset:"),
                decorationConstraint);
        assertTrue(decorationConstraint.contains(
                ":theme=material.colorScheme.error,"), decorationConstraint);
        assertTrue(decorationConstraint.contains(
                "material.colorScheme.surfaceTint"), decorationConstraint);

        var position = projection.propertyContracts()
                .get(new PropertyName("position"));
        assertFalse(position.required());
        assertEquals(Set.of(PropertyValueKind.ENUM), position.acceptedKinds());
        assertTrue(position.creationDefaultFingerprint().isEmpty());
        assertEquals(
                "enum:cGFja2FnZTpmbHV0dGVyL3JlbmRlcmluZy5kYXJ0:"
                + "DecorationPosition:background,foreground",
                position.constraintFingerprints().get(PropertyValueKind.ENUM));

        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.SINGLE, false, 0, 1),
                projection.slotContracts().get(new SlotName("child")));

        String contract = BuiltInWidgetCapabilityCatalog.reviewedCanvasSchemaContract();
        int start = contract.indexOf("W|flutter.widgets.DecoratedBox\n");
        int end = contract.indexOf("W|", start + 2);
        if (end < 0) {
            end = contract.length();
        }
        String decoratedBoxContract = contract.substring(start, end);
        assertTrue(decoratedBoxContract.contains(
                "P|decoration|boxDecoration|1|boxDecoration:empty|-|"
                + "boxDecoration:boxDecoration:v2:"), decoratedBoxContract);
        assertTrue(decoratedBoxContract.contains(
                "P|position|enum|0|-|-|enum:enum:"
                + "cGFja2FnZTpmbHV0dGVyL3JlbmRlcmluZy5kYXJ0:"
                + "DecorationPosition:background,foreground\n"),
                decoratedBoxContract);
        assertTrue(decoratedBoxContract.endsWith(
                "S|child|single|0|0|1|any\n"), decoratedBoxContract);
        assertFalse(decoratedBoxContract.contains("\nC|"), decoratedBoxContract);
    }

    @Test
    void dartObjectReferenceFingerprintCoversBothInvocationConstnessValues() {
        WidgetDefinition definition = definition("flutter.widgets.ClipRRect");
        PropertyValueConstraint constraint = definition
                .property(new PropertyName("clipper"))
                .orElseThrow()
                .constraints()
                .stream()
                .filter(value -> value.kind()
                        == PropertyValueKind.DART_OBJECT_REFERENCE)
                .findFirst()
                .orElseThrow();
        String fingerprint = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition)
                .orElseThrow()
                .propertyContracts()
                .get(new PropertyName("clipper"))
                .constraintFingerprints()
                .get(PropertyValueKind.DART_OBJECT_REFERENCE);

        assertEquals(
                "dartObjectReference:v1:CustomClipper<RRect>:currentOrPackage:"
                + "root,optionalMember:reference,zeroArgumentInvocation:"
                + "requiredConstnessBoolean(false,true)",
                fingerprint);
        for (boolean constant : List.of(false, true)) {
            assertTrue(constraint.accepts(
                    new PropertyValue.DartObjectReferenceValue(
                            Optional.empty(), "RoundedClipper", Optional.empty(),
                            PropertyValue.DartObjectReferenceValue.Access
                                    .ZERO_ARGUMENT_INVOCATION,
                            Optional.of(constant))),
                    () -> "fingerprint must admit invocation constant=" + constant);
        }
    }

    @Test
    void clipRectHasExactTypedClipperClipBehaviorAndOptionalChildProjection() {
        WidgetDefinition definition = definition("flutter.widgets.ClipRect");

        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(ClipRectWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT,
                projection.propertyContracts().size());
        assertEquals(Set.of(new SlotName("child")), projection.slots());

        var clipper = projection.propertyContracts()
                .get(new PropertyName("clipper"));
        assertFalse(clipper.required());
        assertEquals(Set.of(PropertyValueKind.DART_OBJECT_REFERENCE),
                clipper.acceptedKinds());
        assertTrue(clipper.creationDefaultFingerprint().isEmpty());
        assertEquals(
                "dartObjectReference:v1:CustomClipper<Rect>:currentOrPackage:"
                + "root,optionalMember:reference,zeroArgumentInvocation:"
                + "requiredConstnessBoolean(false,true)",
                clipper.constraintFingerprints()
                        .get(PropertyValueKind.DART_OBJECT_REFERENCE));

        var clipBehavior = projection.propertyContracts()
                .get(new PropertyName("clipBehavior"));
        assertFalse(clipBehavior.required());
        assertEquals(Set.of(PropertyValueKind.ENUM), clipBehavior.acceptedKinds());
        assertTrue(clipBehavior.creationDefaultFingerprint().isEmpty());
        assertEquals(
                "enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:"
                + "Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none",
                clipBehavior.constraintFingerprints().get(PropertyValueKind.ENUM));
        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.SINGLE, false, 0, 1),
                projection.slotContracts().get(new SlotName("child")));

        String contract = BuiltInWidgetCapabilityCatalog.reviewedCanvasSchemaContract();
        assertTrue(contract.contains(
                "W|flutter.widgets.ClipRect\n"
                + "P|clipBehavior|enum|0|-|-|enum:enum:"
                + "cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:"
                + "Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none\n"
                + "P|clipper|dartObjectReference|0|-|-|dartObjectReference:"
                + "dartObjectReference:v1:CustomClipper<Rect>:currentOrPackage:"
                + "root,optionalMember:reference,zeroArgumentInvocation:"
                + "requiredConstnessBoolean(false,true)\n"
                + "S|child|single|0|0|1|any\n"), contract);
    }

    @Test
    void clipOvalHasExactTypedClipperClipBehaviorAndOptionalChildProjection() {
        WidgetDefinition definition = definition("flutter.widgets.ClipOval");

        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(ClipOvalWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT,
                projection.propertyContracts().size());
        assertEquals(Set.of(new SlotName("child")), projection.slots());

        var clipper = projection.propertyContracts()
                .get(new PropertyName("clipper"));
        assertFalse(clipper.required());
        assertEquals(Set.of(PropertyValueKind.DART_OBJECT_REFERENCE),
                clipper.acceptedKinds());
        assertTrue(clipper.creationDefaultFingerprint().isEmpty());
        assertEquals(
                "dartObjectReference:v1:CustomClipper<Rect>:currentOrPackage:"
                + "root,optionalMember:reference,zeroArgumentInvocation:"
                + "requiredConstnessBoolean(false,true)",
                clipper.constraintFingerprints()
                        .get(PropertyValueKind.DART_OBJECT_REFERENCE));

        var clipBehavior = projection.propertyContracts()
                .get(new PropertyName("clipBehavior"));
        assertFalse(clipBehavior.required());
        assertEquals(Set.of(PropertyValueKind.ENUM), clipBehavior.acceptedKinds());
        assertTrue(clipBehavior.creationDefaultFingerprint().isEmpty());
        assertEquals(
                "enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:"
                + "Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none",
                clipBehavior.constraintFingerprints().get(PropertyValueKind.ENUM));
        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.SINGLE, false, 0, 1),
                projection.slotContracts().get(new SlotName("child")));

        String contract = BuiltInWidgetCapabilityCatalog.reviewedCanvasSchemaContract();
        assertTrue(contract.contains(
                "W|flutter.widgets.ClipOval\n"
                + "P|clipBehavior|enum|0|-|-|enum:enum:"
                + "cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:"
                + "Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none\n"
                + "P|clipper|dartObjectReference|0|-|-|dartObjectReference:"
                + "dartObjectReference:v1:CustomClipper<Rect>:currentOrPackage:"
                + "root,optionalMember:reference,zeroArgumentInvocation:"
                + "requiredConstnessBoolean(false,true)\n"
                + "S|child|single|0|0|1|any\n"), contract);
    }

    @Test
    void clipRRectHasExactTypedRadiusClipBehaviorAndOptionalChildProjection() {
        WidgetDefinition definition = definition("flutter.widgets.ClipRRect");

        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(ClipRRectWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT,
                projection.propertyContracts().size());
        assertEquals(Set.of(new SlotName("child")), projection.slots());

        var borderRadius = projection.propertyContracts()
                .get(new PropertyName("borderRadius"));
        assertFalse(borderRadius.required());
        assertEquals(Set.of(PropertyValueKind.BORDER_RADIUS),
                borderRadius.acceptedKinds());
        assertTrue(borderRadius.creationDefaultFingerprint().isEmpty());
        assertTrue(borderRadius.numericBounds().isEmpty());
        assertEquals(
                "borderRadius:v1:physical,directional:finiteNonNegative",
                borderRadius.constraintFingerprints()
                        .get(PropertyValueKind.BORDER_RADIUS));

        var clipper = projection.propertyContracts()
                .get(new PropertyName("clipper"));
        assertFalse(clipper.required());
        assertEquals(Set.of(PropertyValueKind.DART_OBJECT_REFERENCE),
                clipper.acceptedKinds());
        assertTrue(clipper.creationDefaultFingerprint().isEmpty());
        assertTrue(clipper.numericBounds().isEmpty());
        assertEquals(
                "dartObjectReference:v1:CustomClipper<RRect>:currentOrPackage:"
                + "root,optionalMember:reference,zeroArgumentInvocation:"
                + "requiredConstnessBoolean(false,true)",
                clipper.constraintFingerprints()
                        .get(PropertyValueKind.DART_OBJECT_REFERENCE));

        var clipBehavior = projection.propertyContracts()
                .get(new PropertyName("clipBehavior"));
        assertFalse(clipBehavior.required());
        assertEquals(Set.of(PropertyValueKind.ENUM), clipBehavior.acceptedKinds());
        assertTrue(clipBehavior.creationDefaultFingerprint().isEmpty());
        assertEquals(
                "enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:"
                + "Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none",
                clipBehavior.constraintFingerprints().get(PropertyValueKind.ENUM));
        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.SINGLE, false, 0, 1),
                projection.slotContracts().get(new SlotName("child")));

        String contract = BuiltInWidgetCapabilityCatalog.reviewedCanvasSchemaContract();
        assertTrue(contract.contains(
                "W|flutter.widgets.ClipRRect\n"
                + "P|borderRadius|borderRadius|0|-|-|borderRadius:"
                + "borderRadius:v1:physical,directional:finiteNonNegative\n"
                + "P|clipBehavior|enum|0|-|-|enum:enum:"
                + "cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:"
                + "Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none\n"
                + "P|clipper|dartObjectReference|0|-|-|dartObjectReference:"
                + "dartObjectReference:v1:CustomClipper<RRect>:currentOrPackage:"
                + "root,optionalMember:reference,zeroArgumentInvocation:"
                + "requiredConstnessBoolean(false,true)\n"
                + "S|child|single|0|0|1|any\n"), contract);
    }

    @Test
    void clipRSuperellipseHasExactTypedRadiusClipBehaviorAndOptionalChildProjection() {
        WidgetDefinition definition = definition("flutter.widgets.ClipRSuperellipse");

        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(ClipRSuperellipseWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT,
                projection.propertyContracts().size());
        assertEquals(Set.of(new SlotName("child")), projection.slots());

        var borderRadius = projection.propertyContracts()
                .get(new PropertyName("borderRadius"));
        assertFalse(borderRadius.required());
        assertEquals(Set.of(PropertyValueKind.BORDER_RADIUS),
                borderRadius.acceptedKinds());
        assertTrue(borderRadius.creationDefaultFingerprint().isEmpty());
        assertTrue(borderRadius.numericBounds().isEmpty());
        assertEquals(
                "borderRadius:v1:physical,directional:finiteNonNegative",
                borderRadius.constraintFingerprints()
                        .get(PropertyValueKind.BORDER_RADIUS));

        var clipper = projection.propertyContracts()
                .get(new PropertyName("clipper"));
        assertFalse(clipper.required());
        assertEquals(Set.of(PropertyValueKind.DART_OBJECT_REFERENCE),
                clipper.acceptedKinds());
        assertTrue(clipper.creationDefaultFingerprint().isEmpty());
        assertTrue(clipper.numericBounds().isEmpty());
        assertEquals(
                "dartObjectReference:v1:CustomClipper<RSuperellipse>:currentOrPackage:"
                + "root,optionalMember:reference,zeroArgumentInvocation:"
                + "requiredConstnessBoolean(false,true)",
                clipper.constraintFingerprints()
                        .get(PropertyValueKind.DART_OBJECT_REFERENCE));

        var clipBehavior = projection.propertyContracts()
                .get(new PropertyName("clipBehavior"));
        assertFalse(clipBehavior.required());
        assertEquals(Set.of(PropertyValueKind.ENUM), clipBehavior.acceptedKinds());
        assertTrue(clipBehavior.creationDefaultFingerprint().isEmpty());
        assertEquals(
                "enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:"
                + "Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none",
                clipBehavior.constraintFingerprints().get(PropertyValueKind.ENUM));
        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.SINGLE, false, 0, 1),
                projection.slotContracts().get(new SlotName("child")));

        String contract = BuiltInWidgetCapabilityCatalog.reviewedCanvasSchemaContract();
        assertTrue(contract.contains(
                "W|flutter.widgets.ClipRSuperellipse\n"
                + "P|borderRadius|borderRadius|0|-|-|borderRadius:"
                + "borderRadius:v1:physical,directional:finiteNonNegative\n"
                + "P|clipBehavior|enum|0|-|-|enum:enum:"
                + "cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:"
                + "Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none\n"
                + "P|clipper|dartObjectReference|0|-|-|dartObjectReference:"
                + "dartObjectReference:v1:CustomClipper<RSuperellipse>:currentOrPackage:"
                + "root,optionalMember:reference,zeroArgumentInvocation:"
                + "requiredConstnessBoolean(false,true)\n"
                + "S|child|single|0|0|1|any\n"), contract);
    }

    @Test
    void physicalModelHasExactRequiredColorShapeClippingRadiusElevationAndShadowProjection() {
        var definition = definition("flutter.widgets.PhysicalModel");
        assertEquals(Set.of(WidgetCapability.PROPERTIES, WidgetCapability.CANVAS,
                WidgetCapability.CREATE, WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog.canvasProjection(definition).orElseThrow();
        assertEquals(6, projection.propertyContracts().size());
        assertEquals("borderRadius:v1:physical:finiteNonNegative", projection.propertyContracts()
                .get(new PropertyName("borderRadius")).constraintFingerprints().get(PropertyValueKind.BORDER_RADIUS));
        assertEquals(Optional.of("color:0xFF2196F3"), projection.propertyContracts()
                .get(new PropertyName("color")).creationDefaultFingerprint());
        assertTrue(projection.propertyContracts().get(new PropertyName("color")).required());
        for (String property : List.of("color", "shadowColor")) {
            assertEquals(Set.of(PropertyValueKind.COLOR, PropertyValueKind.THEME_TOKEN),
                    projection.propertyContracts().get(new PropertyName(property)).acceptedKinds());
        }
        assertFalse(projection.propertyContracts().get(new PropertyName("shadowColor")).required());
        assertEquals(Set.of(PropertyValueKind.DOUBLE, PropertyValueKind.INTEGER), projection.propertyContracts()
                .get(new PropertyName("elevation")).acceptedKinds());
        assertEquals(BigDecimal.ZERO, projection.propertyContracts().get(new PropertyName("elevation"))
                .numericBounds().get(PropertyValueKind.DOUBLE).minimum());
        assertTrue(projection.propertyContracts().get(new PropertyName("shape"))
                .constraintFingerprints().get(PropertyValueKind.ENUM).endsWith(":BoxShape:circle,rectangle"));
        assertEquals(new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(SlotCardinality.SINGLE, false, 0, 1),
                projection.slotContracts().get(new SlotName("child")));
        var changed = definition.properties().stream().map(property -> property.name().value().equals("borderRadius")
                ? new PropertyDefinition(property.name(), property.parameter(),
                        List.of(new PropertyValueConstraint.BorderRadiusValues()), property.creationDefault()) : property).toList();
        var broadened = new WidgetDefinition(definition.typeId(), definition.dartClassName(),
                definition.namedConstructor(), definition.constConstructor(), definition.dartLibraryUri(),
                definition.importUris(), definition.traits(), definition.palette(), changed, definition.slots());
        assertTrue(BuiltInWidgetCapabilityCatalog.canvasProjection(broadened).isEmpty(),
                "Directional broadening must fail the exact reviewed Canvas contract");
    }

    @Test
    void excludeSemanticsHasExactOptionalBooleanAndChildProjection() {
        WidgetDefinition definition = definition(
                "flutter.widgets.ExcludeSemantics");

        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(ExcludeSemanticsWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT,
                projection.propertyContracts().size());
        assertEquals(Set.of(new SlotName("child")), projection.slots());

        var excluding = projection.propertyContracts()
                .get(new PropertyName("excluding"));
        assertFalse(excluding.required());
        assertEquals(Set.of(PropertyValueKind.BOOLEAN), excluding.acceptedKinds());
        assertTrue(excluding.creationDefaultFingerprint().isEmpty());
        assertEquals("any", excluding.constraintFingerprints()
                .get(PropertyValueKind.BOOLEAN));

        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.SINGLE, false, 0, 1),
                projection.slotContracts().get(new SlotName("child")));

        String contract = BuiltInWidgetCapabilityCatalog.reviewedCanvasSchemaContract();
        int start = contract.indexOf("W|flutter.widgets.ExcludeSemantics\n");
        int end = contract.indexOf("W|", start + 2);
        if (end < 0) {
            end = contract.length();
        }
        String excludeSemanticsContract = contract.substring(start, end);
        assertEquals(
                "W|flutter.widgets.ExcludeSemantics\n"
                + "P|excluding|boolean|0|-|-|boolean:any\n"
                + "S|child|single|0|0|1|any\n",
                excludeSemanticsContract);
    }

    @Test
    void gridViewCountHasExactStaticEditableCapabilityAndIndependentProjection() {
        WidgetDefinition definition = definition("flutter.widgets.GridView");

        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(21, projection.propertyContracts().size());
        assertEquals(Set.of(new SlotName("children")), projection.slots());

        var crossAxisCount = projection.propertyContracts()
                .get(new PropertyName("crossAxisCount"));
        assertTrue(crossAxisCount.required());
        assertEquals(Set.of(PropertyValueKind.INTEGER),
                crossAxisCount.acceptedKinds());
        assertEquals(Optional.of("integer:2"),
                crossAxisCount.creationDefaultFingerprint());
        assertEquals("1:1:9007199254740991:1",
                crossAxisCount.numericBounds().get(PropertyValueKind.INTEGER)
                        .fingerprint());
        for (String name : List.of(
                "mainAxisSpacing", "crossAxisSpacing", "mainAxisExtent")) {
            assertEquals("0:1:*:1", projection.propertyContracts()
                    .get(new PropertyName(name)).numericBounds()
                    .get(PropertyValueKind.DOUBLE).fingerprint(), name);
        }
        assertEquals("0:0:*:1", projection.propertyContracts()
                .get(new PropertyName("childAspectRatio")).numericBounds()
                .get(PropertyValueKind.DOUBLE).fingerprint());
        assertTrue(projection.propertyContracts().get(new PropertyName("physics"))
                .constraintFingerprints().get(PropertyValueKind.STRING)
                .startsWith("pattern:"));
        assertTrue(projection.propertyContracts()
                .get(new PropertyName("dragStartBehavior"))
                .constraintFingerprints().get(PropertyValueKind.ENUM)
                .startsWith("enum:cGFja2FnZTpmbHV0dGVyL2dlc3R1cmVzLmRhcnQ:"));
        assertTrue(projection.propertyContracts()
                .get(new PropertyName("hitTestBehavior"))
                .constraintFingerprints().get(PropertyValueKind.ENUM)
                .startsWith("enum:cGFja2FnZTpmbHV0dGVyL3JlbmRlcmluZy5kYXJ0:"));
        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.LIST, false, 0, 10_000),
                projection.slotContracts().get(new SlotName("children")));
    }

    @Test
    void singleChildScrollViewHasExactStaticEditableCapabilityAndIndependentProjection() {
        WidgetDefinition definition = definition(
                "flutter.widgets.SingleChildScrollView");

        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(10, projection.propertyContracts().size());
        assertEquals(Set.of(new SlotName("child")), projection.slots());
        assertTrue(projection.propertyContracts().get(new PropertyName("physics"))
                .constraintFingerprints().get(PropertyValueKind.STRING)
                .startsWith("pattern:"));
        assertTrue(projection.propertyContracts()
                .get(new PropertyName("dragStartBehavior"))
                .constraintFingerprints().get(PropertyValueKind.ENUM)
                .startsWith("enum:cGFja2FnZTpmbHV0dGVyL2dlc3R1cmVzLmRhcnQ:"));
        assertTrue(projection.propertyContracts()
                .get(new PropertyName("hitTestBehavior"))
                .constraintFingerprints().get(PropertyValueKind.ENUM)
                .startsWith("enum:cGFja2FnZTpmbHV0dGVyL3JlbmRlcmluZy5kYXJ0:"));
        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.SINGLE, false, 0, 1),
                projection.slotContracts().get(new SlotName("child")));
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
    void wrapHasTheExactStaticEditableProjection() {
        WidgetDefinition definition = definition("flutter.widgets.Wrap");

        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(Set.of(
                        new PropertyName("direction"),
                        new PropertyName("alignment"),
                        new PropertyName("spacing"),
                        new PropertyName("runAlignment"),
                        new PropertyName("runSpacing"),
                        new PropertyName("crossAxisAlignment"),
                        new PropertyName("textDirection"),
                        new PropertyName("verticalDirection"),
                        new PropertyName("clipBehavior")),
                projection.properties().keySet());
        assertEquals(Set.of(new SlotName("children")), projection.slots());
        assertEquals("*:1:*:1", projection.propertyContracts()
                .get(new PropertyName("spacing")).numericBounds()
                .get(PropertyValueKind.DOUBLE).fingerprint());
        assertEquals("*:1:*:1", projection.propertyContracts()
                .get(new PropertyName("runSpacing")).numericBounds()
                .get(PropertyValueKind.DOUBLE).fingerprint());
        assertTrue(projection.propertyContracts()
                .get(new PropertyName("alignment")).constraintFingerprints()
                .get(PropertyValueKind.ENUM)
                .endsWith(":WrapAlignment:center,end,spaceAround,spaceBetween,spaceEvenly,start"));
        assertTrue(projection.propertyContracts()
                .get(new PropertyName("crossAxisAlignment")).constraintFingerprints()
                .get(PropertyValueKind.ENUM)
                .endsWith(":WrapCrossAlignment:center,end,start"));
        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.LIST, false, 0, 10_000),
                projection.slotContracts().get(new SlotName("children")));
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
    void fittedBoxHasTheExactStaticEditableCapabilityAndIndependentProjection() {
        WidgetDefinition definition = definition("flutter.widgets.FittedBox");

        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(Set.of(
                        new PropertyName("fit"),
                        new PropertyName("alignment"),
                        new PropertyName("clipBehavior")),
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

        String widgetsLibrary =
                "cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA";
        assertEquals("enum:" + widgetsLibrary
                        + ":BoxFit:contain,cover,fill,fitHeight,fitWidth,none,scaleDown",
                projection.propertyContracts().get(new PropertyName("fit"))
                        .constraintFingerprints().get(PropertyValueKind.ENUM));
        assertEquals("enum:" + widgetsLibrary
                        + ":Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none",
                projection.propertyContracts().get(new PropertyName("clipBehavior"))
                        .constraintFingerprints().get(PropertyValueKind.ENUM));
        assertTrue(projection.propertyContracts().values().stream()
                .allMatch(value -> !value.required()
                        && value.creationDefaultFingerprint().isEmpty()));
        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.SINGLE, false, 0, 1),
                projection.slotContracts().get(new SlotName("child")));
    }

    @Test
    void constrainedBoxHasRequiredCompleteConstraintsAndOptionalChild() {
        WidgetDefinition definition = definition("flutter.widgets.ConstrainedBox");

        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(Set.of(new PropertyName("constraints")),
                projection.properties().keySet());
        assertEquals(Set.of(new SlotName("child")), projection.slots());

        var constraints = projection.propertyContracts().get(
                new PropertyName("constraints"));
        assertTrue(constraints.required());
        assertEquals(Set.of(PropertyValueKind.BOX_CONSTRAINTS),
                constraints.acceptedKinds());
        assertEquals(Optional.of("boxConstraints:0,inf,0,inf"),
                constraints.creationDefaultFingerprint());
        assertEquals("boxConstraints:v2:finiteOrPositiveInfinity",
                constraints.constraintFingerprints()
                        .get(PropertyValueKind.BOX_CONSTRAINTS));
        assertTrue(constraints.numericBounds().isEmpty());
        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.SINGLE, false, 0, 1),
                projection.slotContracts().get(new SlotName("child")));
    }

    @Test
    void unconstrainedBoxHasExactStaticEditableCapabilityAndIndependentProjection() {
        WidgetDefinition definition = definition("flutter.widgets.UnconstrainedBox");

        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(Set.of(
                        new PropertyName("textDirection"),
                        new PropertyName("alignment"),
                        new PropertyName("constrainedAxis"),
                        new PropertyName("clipBehavior")),
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

        String widgetsLibrary =
                "cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA";
        assertEquals("enum:" + widgetsLibrary + ":TextDirection:ltr,rtl",
                projection.propertyContracts().get(new PropertyName("textDirection"))
                        .constraintFingerprints().get(PropertyValueKind.ENUM));
        assertEquals("enum:" + widgetsLibrary + ":Axis:horizontal,vertical",
                projection.propertyContracts().get(new PropertyName("constrainedAxis"))
                        .constraintFingerprints().get(PropertyValueKind.ENUM));
        assertEquals("enum:" + widgetsLibrary
                        + ":Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none",
                projection.propertyContracts().get(new PropertyName("clipBehavior"))
                        .constraintFingerprints().get(PropertyValueKind.ENUM));
        assertTrue(projection.propertyContracts().values().stream()
                .allMatch(value -> !value.required()
                        && value.creationDefaultFingerprint().isEmpty()));
        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.SINGLE, false, 0, 1),
                projection.slotContracts().get(new SlotName("child")));
    }

    @Test
    void limitedBoxHasExactStaticEditableCapabilityAndIndependentProjection() {
        WidgetDefinition definition = definition("flutter.widgets.LimitedBox");

        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(Set.of(
                        new PropertyName("maxWidth"),
                        new PropertyName("maxHeight")),
                projection.properties().keySet());
        assertEquals(Set.of(new SlotName("child")), projection.slots());

        for (String name : List.of("maxWidth", "maxHeight")) {
            var dimension = projection.propertyContracts().get(
                    new PropertyName(name));
            assertFalse(dimension.required(), name);
            assertEquals(Set.of(PropertyValueKind.DOUBLE),
                    dimension.acceptedKinds(), name);
            assertTrue(dimension.creationDefaultFingerprint().isEmpty(), name);
            assertEquals("0:1:*:1", dimension.numericBounds()
                    .get(PropertyValueKind.DOUBLE).fingerprint(), name);
            assertEquals("range:0:1:*:1",
                    dimension.constraintFingerprints().get(PropertyValueKind.DOUBLE),
                    name);
        }
        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.SINGLE, false, 0, 1),
                projection.slotContracts().get(new SlotName("child")));
    }

    @Test
    void overflowBoxHasExactStaticEditableCapabilityAndIndependentProjection() {
        WidgetDefinition definition = definition("flutter.widgets.OverflowBox");

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
                        new PropertyName("minWidth"),
                        new PropertyName("maxWidth"),
                        new PropertyName("minHeight"),
                        new PropertyName("maxHeight"),
                        new PropertyName("fit")),
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

        for (String name : List.of(
                "minWidth", "maxWidth", "minHeight", "maxHeight")) {
            var dimension = projection.propertyContracts().get(
                    new PropertyName(name));
            assertFalse(dimension.required(), name);
            assertEquals(Set.of(PropertyValueKind.DOUBLE),
                    dimension.acceptedKinds(), name);
            assertTrue(dimension.creationDefaultFingerprint().isEmpty(), name);
            assertEquals("0:1:*:1", dimension.numericBounds()
                    .get(PropertyValueKind.DOUBLE).fingerprint(), name);
            assertEquals("range:0:1:*:1",
                    dimension.constraintFingerprints().get(PropertyValueKind.DOUBLE),
                    name);
        }

        var fit = projection.propertyContracts().get(new PropertyName("fit"));
        assertFalse(fit.required());
        assertEquals(Set.of(PropertyValueKind.ENUM), fit.acceptedKinds());
        assertTrue(fit.creationDefaultFingerprint().isEmpty());
        assertEquals(
                "enum:cGFja2FnZTpmbHV0dGVyL3JlbmRlcmluZy5kYXJ0:"
                + "OverflowBoxFit:deferToChild,max",
                fit.constraintFingerprints().get(PropertyValueKind.ENUM));
        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.SINGLE, false, 0, 1),
                projection.slotContracts().get(new SlotName("child")));
    }

    @Test
    void stackHasTheExactStaticEditableCapabilityAndIndependentProjection() {
        WidgetDefinition definition = definition("flutter.widgets.Stack");

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
                        new PropertyName("textDirection"),
                        new PropertyName("fit"),
                        new PropertyName("clipBehavior")),
                projection.properties().keySet());
        assertEquals(Set.of(new SlotName("children")), projection.slots());

        var alignment = projection.propertyContracts().get(
                new PropertyName("alignment"));
        assertFalse(alignment.required());
        assertEquals(Set.of(PropertyValueKind.ALIGNMENT_GEOMETRY),
                alignment.acceptedKinds());
        assertTrue(alignment.creationDefaultFingerprint().isEmpty());
        assertEquals("alignmentGeometry", alignment.constraintFingerprints()
                .get(PropertyValueKind.ALIGNMENT_GEOMETRY));
        assertTrue(alignment.numericBounds().isEmpty());

        String widgetsLibrary =
                "cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA";
        Map<String, String> enumFingerprints = Map.of(
                "textDirection", "enum:" + widgetsLibrary
                        + ":TextDirection:ltr,rtl",
                "fit", "enum:" + widgetsLibrary
                        + ":StackFit:expand,loose,passthrough",
                "clipBehavior", "enum:" + widgetsLibrary
                        + ":Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none");
        for (Map.Entry<String, String> entry : enumFingerprints.entrySet()) {
            var property = projection.propertyContracts().get(
                    new PropertyName(entry.getKey()));
            assertFalse(property.required(), entry.getKey());
            assertEquals(Set.of(PropertyValueKind.ENUM),
                    property.acceptedKinds(), entry.getKey());
            assertTrue(property.creationDefaultFingerprint().isEmpty(), entry.getKey());
            assertTrue(property.numericBounds().isEmpty(), entry.getKey());
            assertEquals(entry.getValue(), property.constraintFingerprints()
                    .get(PropertyValueKind.ENUM), entry.getKey());
        }

        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.LIST, false, 0, 10_000),
                projection.slotContracts().get(new SlotName("children")));
    }

    @Test
    void expandedHasExactStaticEditableCapabilityAndIndependentProjection() {
        WidgetDefinition definition = definition("flutter.widgets.Expanded");

        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(Set.of(new PropertyName("flex")), projection.properties().keySet());
        assertEquals(Set.of(new SlotName("child")), projection.slots());

        var flex = projection.propertyContracts().get(new PropertyName("flex"));
        assertFalse(flex.required());
        assertEquals(Set.of(PropertyValueKind.INTEGER), flex.acceptedKinds());
        assertTrue(flex.creationDefaultFingerprint().isEmpty());
        assertEquals("0:1:9007199254740991:1", flex.numericBounds()
                .get(PropertyValueKind.INTEGER).fingerprint());
        assertEquals("range:0:1:9007199254740991:1",
                flex.constraintFingerprints().get(PropertyValueKind.INTEGER));
        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.SINGLE, true, 1, 1),
                projection.slotContracts().get(new SlotName("child")));
    }

    @Test
    void baselineHasRequiredVisibleDefaultsAndIndependentFiniteProjection() {
        WidgetDefinition definition = definition("flutter.widgets.Baseline");

        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(Set.of(
                        new PropertyName("baseline"),
                        new PropertyName("baselineType")),
                projection.properties().keySet());
        assertEquals(Set.of(new SlotName("child")), projection.slots());

        var distance = projection.propertyContracts().get(
                new PropertyName("baseline"));
        assertTrue(distance.required());
        assertEquals(Set.of(PropertyValueKind.DOUBLE), distance.acceptedKinds());
        assertEquals(Optional.of("double:24"),
                distance.creationDefaultFingerprint());
        assertEquals("*:1:*:1", distance.numericBounds()
                .get(PropertyValueKind.DOUBLE).fingerprint());
        assertEquals("range:*:1:*:1", distance.constraintFingerprints()
                .get(PropertyValueKind.DOUBLE));

        var type = projection.propertyContracts().get(
                new PropertyName("baselineType"));
        assertTrue(type.required());
        assertEquals(Set.of(PropertyValueKind.ENUM), type.acceptedKinds());
        assertEquals(Optional.of("enum:TextBaseline:alphabetic"),
                type.creationDefaultFingerprint());
        assertTrue(type.numericBounds().isEmpty());
        assertTrue(type.constraintFingerprints().get(PropertyValueKind.ENUM)
                .endsWith(":TextBaseline:alphabetic,ideographic"));

        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.SINGLE, false, 0, 1),
                projection.slotContracts().get(new SlotName("child")));
    }

    @Test
    void intrinsicHeightHasStructuralCapabilitiesAndIndependentEmptyProjection() {
        WidgetDefinition definition = definition(
                "flutter.widgets.IntrinsicHeight");

        assertEquals(Set.of(
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        assertFalse(BuiltInWidgetCapabilityCatalog.supports(
                definition, WidgetCapability.PROPERTIES));

        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertTrue(projection.properties().isEmpty());
        assertEquals(Set.of(new SlotName("child")), projection.slots());
        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.SINGLE, false, 0, 1),
                projection.slotContracts().get(new SlotName("child")));
    }

    @Test
    void intrinsicWidthHasStaticEditableCapabilitiesAndIndependentProjection() {
        WidgetDefinition definition = definition(
                "flutter.widgets.IntrinsicWidth");

        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));

        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(Set.of(
                        new PropertyName("stepWidth"),
                        new PropertyName("stepHeight")),
                projection.properties().keySet());
        for (String name : List.of("stepWidth", "stepHeight")) {
            var step = projection.propertyContracts().get(new PropertyName(name));
            assertFalse(step.required(), name);
            assertEquals(Set.of(PropertyValueKind.DOUBLE), step.acceptedKinds(), name);
            assertTrue(step.creationDefaultFingerprint().isEmpty(), name);
            assertEquals("0:1:*:1", step.numericBounds()
                    .get(PropertyValueKind.DOUBLE).fingerprint(), name);
            assertEquals("range:0:1:*:1", step.constraintFingerprints()
                    .get(PropertyValueKind.DOUBLE), name);
        }
        assertEquals(Set.of(new SlotName("child")), projection.slots());
        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.SINGLE, false, 0, 1),
                projection.slotContracts().get(new SlotName("child")));
    }

    @Test
    void offstageHasStaticEditableCapabilitiesAndIndependentProjection() {
        WidgetDefinition definition = definition("flutter.widgets.Offstage");

        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));

        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(Set.of(new PropertyName("offstage")),
                projection.properties().keySet());
        var hidden = projection.propertyContracts().get(
                new PropertyName("offstage"));
        assertFalse(hidden.required());
        assertEquals(Set.of(PropertyValueKind.BOOLEAN), hidden.acceptedKinds());
        assertTrue(hidden.creationDefaultFingerprint().isEmpty());
        assertTrue(hidden.numericBounds().isEmpty());
        assertEquals("any", hidden.constraintFingerprints()
                .get(PropertyValueKind.BOOLEAN));
        assertEquals(Set.of(new SlotName("child")), projection.slots());
        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.SINGLE, false, 0, 1),
                projection.slotContracts().get(new SlotName("child")));
    }

    @Test
    void sizedOverflowBoxHasStaticEditableCapabilitiesAndIndependentProjection() {
        WidgetDefinition definition = definition(
                "flutter.widgets.SizedOverflowBox");

        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));

        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(Set.of(
                        new PropertyName("size"),
                        new PropertyName("alignment")),
                projection.properties().keySet());

        var size = projection.propertyContracts().get(new PropertyName("size"));
        assertTrue(size.required());
        assertEquals(Set.of(PropertyValueKind.SIZE), size.acceptedKinds());
        assertEquals(Optional.of("size:100,100"),
                size.creationDefaultFingerprint());
        assertTrue(size.numericBounds().isEmpty());
        assertEquals("size:finiteNonNegative",
                size.constraintFingerprints().get(PropertyValueKind.SIZE));

        var alignment = projection.propertyContracts().get(
                new PropertyName("alignment"));
        assertFalse(alignment.required());
        assertEquals(Set.of(PropertyValueKind.ALIGNMENT_GEOMETRY),
                alignment.acceptedKinds());
        assertTrue(alignment.creationDefaultFingerprint().isEmpty());
        assertTrue(alignment.numericBounds().isEmpty());
        assertEquals("alignmentGeometry", alignment.constraintFingerprints()
                .get(PropertyValueKind.ALIGNMENT_GEOMETRY));

        assertEquals(Set.of(new SlotName("child")), projection.slots());
        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.SINGLE, false, 0, 1),
                projection.slotContracts().get(new SlotName("child")));
    }

    @Test
    void transformHasStaticEditableCapabilitiesAndExactV14Projection() {
        WidgetDefinition definition = definition("flutter.widgets.Transform");

        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));

        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(Set.of(
                        new PropertyName("transform"),
                        new PropertyName("origin"),
                        new PropertyName("alignment"),
                        new PropertyName("transformHitTests"),
                        new PropertyName("filterQuality")),
                projection.properties().keySet());

        var matrix = projection.propertyContracts().get(
                new PropertyName("transform"));
        assertTrue(matrix.required());
        assertEquals(Set.of(PropertyValueKind.MATRIX4), matrix.acceptedKinds());
        assertEquals(Optional.of(
                        "matrix4:1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1"),
                matrix.creationDefaultFingerprint());
        assertTrue(matrix.numericBounds().isEmpty());
        assertEquals("matrix4", matrix.constraintFingerprints()
                .get(PropertyValueKind.MATRIX4));

        var origin = projection.propertyContracts().get(new PropertyName("origin"));
        assertFalse(origin.required());
        assertEquals(Set.of(PropertyValueKind.OFFSET), origin.acceptedKinds());
        assertTrue(origin.creationDefaultFingerprint().isEmpty());
        assertTrue(origin.numericBounds().isEmpty());
        assertEquals("offset:finiteSigned", origin.constraintFingerprints()
                .get(PropertyValueKind.OFFSET));

        var alignment = projection.propertyContracts().get(
                new PropertyName("alignment"));
        assertEquals("alignmentGeometry", alignment.constraintFingerprints()
                .get(PropertyValueKind.ALIGNMENT_GEOMETRY));

        var hitTests = projection.propertyContracts().get(
                new PropertyName("transformHitTests"));
        assertFalse(hitTests.required());
        assertEquals(Set.of(PropertyValueKind.BOOLEAN), hitTests.acceptedKinds());
        assertTrue(hitTests.creationDefaultFingerprint().isEmpty());
        assertEquals("any", hitTests.constraintFingerprints()
                .get(PropertyValueKind.BOOLEAN));

        var quality = projection.propertyContracts().get(
                new PropertyName("filterQuality"));
        assertFalse(quality.required());
        assertEquals(Set.of(PropertyValueKind.ENUM), quality.acceptedKinds());
        assertTrue(quality.creationDefaultFingerprint().isEmpty());
        assertTrue(quality.constraintFingerprints().get(PropertyValueKind.ENUM)
                .endsWith(":FilterQuality:high,low,medium,none"));

        assertEquals(Set.of(new SlotName("child")), projection.slots());
        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.SINGLE, false, 0, 1),
                projection.slotContracts().get(new SlotName("child")));
    }

    @Test
    void rotatedBoxHasStaticEditableCapabilitiesAndExactV14Projection() {
        WidgetDefinition definition = definition("flutter.widgets.RotatedBox");

        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));

        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(Set.of(new PropertyName("quarterTurns")),
                projection.properties().keySet());
        var quarterTurns = projection.propertyContracts().get(
                new PropertyName("quarterTurns"));
        assertTrue(quarterTurns.required());
        assertEquals(Set.of(PropertyValueKind.INTEGER),
                quarterTurns.acceptedKinds());
        assertEquals(Optional.of("integer:1"),
                quarterTurns.creationDefaultFingerprint());
        assertEquals(
                new BigDecimal(DartNumericLiterals.MIN_PORTABLE_INTEGER),
                quarterTurns.numericBounds().get(PropertyValueKind.INTEGER).minimum());
        assertEquals(
                new BigDecimal(DartNumericLiterals.MAX_PORTABLE_INTEGER),
                quarterTurns.numericBounds().get(PropertyValueKind.INTEGER).maximum());
        assertEquals("range:-9007199254740991:1:9007199254740991:1",
                quarterTurns.constraintFingerprints().get(PropertyValueKind.INTEGER));

        assertEquals(Set.of(new SlotName("child")), projection.slots());
        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.SINGLE, false, 0, 1),
                projection.slotContracts().get(new SlotName("child")));
    }

    @Test
    void listBodyHasStaticEditableCapabilitiesAndExactV14Projection() {
        WidgetDefinition definition = definition("flutter.widgets.ListBody");

        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));

        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(Set.of(
                        new PropertyName("mainAxis"),
                        new PropertyName("reverse")),
                projection.properties().keySet());

        var mainAxis = projection.propertyContracts().get(
                new PropertyName("mainAxis"));
        assertFalse(mainAxis.required());
        assertEquals(Set.of(PropertyValueKind.ENUM), mainAxis.acceptedKinds());
        assertTrue(mainAxis.creationDefaultFingerprint().isEmpty());
        assertEquals(
                "enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:"
                + "Axis:horizontal,vertical",
                mainAxis.constraintFingerprints().get(PropertyValueKind.ENUM));

        var reverse = projection.propertyContracts().get(
                new PropertyName("reverse"));
        assertFalse(reverse.required());
        assertEquals(Set.of(PropertyValueKind.BOOLEAN), reverse.acceptedKinds());
        assertTrue(reverse.creationDefaultFingerprint().isEmpty());
        assertEquals("any",
                reverse.constraintFingerprints().get(PropertyValueKind.BOOLEAN));

        assertEquals(Set.of(new SlotName("children")), projection.slots());
        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.LIST, false, 0, 10_000),
                projection.slotContracts().get(new SlotName("children")));
    }

    @Test
    void overflowBarHasStaticEditableCapabilitiesAndExactV14Projection() {
        WidgetDefinition definition = definition("flutter.widgets.OverflowBar");

        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));

        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(Set.of(
                        new PropertyName("spacing"),
                        new PropertyName("alignment"),
                        new PropertyName("overflowSpacing"),
                        new PropertyName("overflowAlignment"),
                        new PropertyName("overflowDirection"),
                        new PropertyName("textDirection")),
                projection.properties().keySet());

        for (String name : List.of("spacing", "overflowSpacing")) {
            var spacing = projection.propertyContracts().get(new PropertyName(name));
            assertFalse(spacing.required(), name);
            assertEquals(Set.of(PropertyValueKind.DOUBLE),
                    spacing.acceptedKinds(), name);
            assertTrue(spacing.creationDefaultFingerprint().isEmpty(), name);
            assertEquals("*:1:*:1", spacing.numericBounds()
                    .get(PropertyValueKind.DOUBLE).fingerprint(), name);
            assertEquals("range:*:1:*:1",
                    spacing.constraintFingerprints().get(PropertyValueKind.DOUBLE), name);
        }

        assertTrue(projection.propertyContracts()
                .get(new PropertyName("alignment")).constraintFingerprints()
                .get(PropertyValueKind.ENUM)
                .endsWith(":MainAxisAlignment:center,end,spaceAround,"
                        + "spaceBetween,spaceEvenly,start"));
        assertTrue(projection.propertyContracts()
                .get(new PropertyName("overflowAlignment")).constraintFingerprints()
                .get(PropertyValueKind.ENUM)
                .endsWith(":OverflowBarAlignment:center,end,start"));
        assertTrue(projection.propertyContracts()
                .get(new PropertyName("overflowDirection")).constraintFingerprints()
                .get(PropertyValueKind.ENUM)
                .endsWith(":VerticalDirection:down,up"));
        assertTrue(projection.propertyContracts()
                .get(new PropertyName("textDirection")).constraintFingerprints()
                .get(PropertyValueKind.ENUM)
                .endsWith(":TextDirection:ltr,rtl"));

        assertEquals(Set.of(new SlotName("children")), projection.slots());
        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.LIST, false, 0, 10_000),
                projection.slotContracts().get(new SlotName("children")));
    }

    @Test
    void flexibleHasExactStaticEditableCapabilityAndIndependentProjection() {
        WidgetDefinition definition = definition("flutter.widgets.Flexible");

        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(Set.of(new PropertyName("flex"), new PropertyName("fit")),
                projection.properties().keySet());
        assertEquals(Set.of(new SlotName("child")), projection.slots());

        var flex = projection.propertyContracts().get(new PropertyName("flex"));
        assertFalse(flex.required());
        assertEquals(Set.of(PropertyValueKind.INTEGER), flex.acceptedKinds());
        assertTrue(flex.creationDefaultFingerprint().isEmpty());
        assertEquals("0:1:9007199254740991:1", flex.numericBounds()
                .get(PropertyValueKind.INTEGER).fingerprint());
        assertEquals("range:0:1:9007199254740991:1",
                flex.constraintFingerprints().get(PropertyValueKind.INTEGER));

        var fit = projection.propertyContracts().get(new PropertyName("fit"));
        assertFalse(fit.required());
        assertEquals(Set.of(PropertyValueKind.ENUM), fit.acceptedKinds());
        assertTrue(fit.creationDefaultFingerprint().isEmpty());
        assertTrue(fit.numericBounds().isEmpty());
        assertEquals(
                "enum:cGFja2FnZTpmbHV0dGVyL3JlbmRlcmluZy5kYXJ0:"
                + "FlexFit:loose,tight",
                fit.constraintFingerprints().get(PropertyValueKind.ENUM));
        assertEquals(
                new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.SINGLE, true, 1, 1),
                projection.slotContracts().get(new SlotName("child")));
    }

    @Test
    void spacerHasExactStaticEditableCapabilityAndIndependentProjection() {
        WidgetDefinition definition = definition("flutter.widgets.Spacer");

        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(Set.of(new PropertyName("flex")), projection.properties().keySet());
        assertTrue(projection.slots().isEmpty());

        var flex = projection.propertyContracts().get(new PropertyName("flex"));
        assertFalse(flex.required());
        assertEquals(Set.of(PropertyValueKind.INTEGER), flex.acceptedKinds());
        assertTrue(flex.creationDefaultFingerprint().isEmpty());
        assertEquals("1:1:9007199254740991:1", flex.numericBounds()
                .get(PropertyValueKind.INTEGER).fingerprint());
        assertEquals("range:1:1:9007199254740991:1",
                flex.constraintFingerprints().get(PropertyValueKind.INTEGER));
    }

    @Test
    void textFieldHasExactStaticEditableCapabilityAndIndependentProjection() {
        WidgetDefinition definition = definition("flutter.material.TextField");
        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));

        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(54, projection.propertyContracts().size());
        assertTrue(projection.slots().isEmpty());
        assertTrue(projection.propertyContracts().values().stream()
                .allMatch(value -> !value.required()
                        && value.creationDefaultFingerprint().isEmpty()));

        for (String name : List.of("maxLines", "minLines")) {
            assertEquals("1:1:9007199254740991:1",
                    projection.propertyContracts().get(new PropertyName(name))
                            .numericBounds().get(PropertyValueKind.INTEGER).fingerprint(),
                    name);
        }
        assertEquals("-1:1:9007199254740991:1",
                projection.propertyContracts().get(new PropertyName("maxLength"))
                        .numericBounds().get(PropertyValueKind.INTEGER).fingerprint());
        for (String name : List.of("cursorWidth", "cursorHeight")) {
            var contract = projection.propertyContracts().get(new PropertyName(name));
            assertEquals(Set.of(PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                    contract.acceptedKinds(), name);
            assertEquals("0:1:9007199254740991:1",
                    contract.numericBounds().get(PropertyValueKind.INTEGER).fingerprint(), name);
            assertEquals("0:1:*:1",
                    contract.numericBounds().get(PropertyValueKind.DOUBLE).fingerprint(), name);
        }
        for (String name : List.of(
                "cursorRadiusX", "cursorRadiusY", "scrollPaddingLeft",
                "scrollPaddingTop", "scrollPaddingRight", "scrollPaddingBottom")) {
            assertEquals("0:1:*:1",
                    projection.propertyContracts().get(new PropertyName(name))
                            .numericBounds().get(PropertyValueKind.DOUBLE).fingerprint(), name);
        }

        String obscurerPattern = "[\\u0000-\\uD7FF\\uE000-\\uFFFF]";
        String encodedPattern = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(obscurerPattern.getBytes(StandardCharsets.UTF_8));
        assertEquals("pattern:" + encodedPattern,
                projection.propertyContracts().get(new PropertyName("obscuringCharacter"))
                        .constraintFingerprints().get(PropertyValueKind.STRING));
        assertEquals("length:1:256",
                projection.propertyContracts().get(new PropertyName("restorationId"))
                        .constraintFingerprints().get(PropertyValueKind.STRING));
        assertTrue(projection.propertyContracts().get(new PropertyName("textInputAction"))
                .constraintFingerprints().get(PropertyValueKind.ENUM)
                .startsWith("enum:cGFja2FnZTpmbHV0dGVyL3NlcnZpY2VzLmRhcnQ:"));
        assertTrue(projection.propertyContracts().get(new PropertyName("selectionHeightStyle"))
                .constraintFingerprints().get(PropertyValueKind.ENUM)
                .startsWith("enum:ZGFydDp1aQ:BoxHeightStyle:"));
        assertTrue(projection.propertyContracts().get(new PropertyName("dragStartBehavior"))
                .constraintFingerprints().get(PropertyValueKind.ENUM)
                .startsWith("enum:cGFja2FnZTpmbHV0dGVyL2dlc3R1cmVzLmRhcnQ:"));
        for (String name : List.of(
                "onChanged", "onEditingComplete", "onSubmitted",
                "onAppPrivateCommand", "onTap", "onTapOutside", "onTapUpOutside")) {
            assertEquals("callbackReference",
                    projection.propertyContracts().get(new PropertyName(name))
                            .constraintFingerprints().get(PropertyValueKind.CALLBACK), name);
        }
    }

    @Test
    void textFieldFullReviewedProjectionHasStableFingerprintWithoutRelationLines()
            throws Exception {
        String contract = BuiltInWidgetCapabilityCatalog.reviewedCanvasSchemaContract();
        int start = contract.indexOf("W|flutter.material.TextField\n");
        int end = contract.indexOf("W|", start + 2);
        String textField = contract.substring(start, end);

        assertEquals(8_076,
                textField.getBytes(StandardCharsets.UTF_8).length);
        assertEquals(
                "0cae00ba20bef22302e2b2db29fafbe19a535e51d9791416d670e6881162f61f",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                        .digest(textField.getBytes(StandardCharsets.UTF_8))));
        assertFalse(textField.contains("\nR|"), textField);
        assertFalse(textField.contains("\nC|"), textField);
    }

    @Test
    void imageHasExactStaticEditableCapabilityAndIndependentProjection() {
        WidgetDefinition definition = definition("flutter.widgets.Image");
        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));

        var projection = BuiltInWidgetCapabilityCatalog
                .canvasProjection(definition).orElseThrow();
        assertEquals(22, projection.propertyContracts().size());
        assertTrue(projection.slots().isEmpty());

        var provider = projection.propertyContracts().get(new PropertyName("image"));
        assertTrue(provider.required());
        assertEquals(Set.of(PropertyValueKind.IMAGE_PROVIDER), provider.acceptedKinds());
        assertTrue(provider.creationDefaultFingerprint().isEmpty());
        assertEquals(
                "imageProvider:v1:asset,exactAsset:package:exactScale:"
                + "resize(1..16384,exact,fit,allowUpscaling)",
                provider.constraintFingerprints().get(PropertyValueKind.IMAGE_PROVIDER));

        for (String name : List.of("frameBuilder", "loadingBuilder", "errorBuilder")) {
            assertEquals("callbackReference", projection.propertyContracts()
                    .get(new PropertyName(name)).constraintFingerprints()
                    .get(PropertyValueKind.CALLBACK), name);
        }
        assertEquals("0:1:1:1", projection.propertyContracts()
                .get(new PropertyName("opacity")).numericBounds()
                .get(PropertyValueKind.DOUBLE).fingerprint());
        for (String name : List.of(
                "centerSliceLeft", "centerSliceTop",
                "centerSliceRight", "centerSliceBottom")) {
            assertEquals("0:1:*:1", projection.propertyContracts()
                    .get(new PropertyName(name)).numericBounds()
                    .get(PropertyValueKind.DOUBLE).fingerprint(), name);
        }
        assertTrue(projection.propertyContracts().get(new PropertyName("fit"))
                .constraintFingerprints().get(PropertyValueKind.ENUM)
                .endsWith(":BoxFit:contain,cover,fill,fitHeight,fitWidth,none,scaleDown"));
        assertTrue(projection.propertyContracts().get(new PropertyName("repeat"))
                .constraintFingerprints().get(PropertyValueKind.ENUM)
                .endsWith(":ImageRepeat:noRepeat,repeat,repeatX,repeatY"));
        assertTrue(projection.propertyContracts().get(new PropertyName("filterQuality"))
                .constraintFingerprints().get(PropertyValueKind.ENUM)
                .endsWith(":FilterQuality:high,low,medium,none"));
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
        assertEquals("boxConstraints:v2:finiteOrPositiveInfinity", projection.propertyContracts()
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
        assertTrue(contract.contains(
                "W|flutter.widgets.FittedBox\n"
                + "P|alignment|alignmentGeometry|0|-|-|"
                + "alignmentGeometry:alignmentGeometry\n"
                + "P|clipBehavior|enum|0|-|-|"
                + "enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:"
                + "Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none\n"
                + "P|fit|enum|0|-|-|"
                + "enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:"
                + "BoxFit:contain,cover,fill,fitHeight,fitWidth,none,scaleDown\n"
                + "S|child|single|0|0|1|any\n"));
        assertTrue(contract.contains(
                "W|flutter.widgets.ConstrainedBox\n"
                + "P|constraints|boxConstraints|1|"
                + "boxConstraints:0,inf,0,inf|-|"
                + "boxConstraints:boxConstraints:v2:finiteOrPositiveInfinity\n"
                + "S|child|single|0|0|1|any\n"));
        assertTrue(contract.contains(
                "W|flutter.widgets.UnconstrainedBox\n"
                + "P|alignment|alignmentGeometry|0|-|-|"
                + "alignmentGeometry:alignmentGeometry\n"
                + "P|clipBehavior|enum|0|-|-|"
                + "enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:"
                + "Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none\n"
                + "P|constrainedAxis|enum|0|-|-|"
                + "enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:"
                + "Axis:horizontal,vertical\n"
                + "P|textDirection|enum|0|-|-|"
                + "enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:"
                + "TextDirection:ltr,rtl\n"
                + "S|child|single|0|0|1|any\n"));
        assertTrue(contract.contains(
                "W|flutter.widgets.LimitedBox\n"
                + "P|maxHeight|double|0|-|double:0:1:*:1|"
                + "double:range:0:1:*:1\n"
                + "P|maxWidth|double|0|-|double:0:1:*:1|"
                + "double:range:0:1:*:1\n"
                + "S|child|single|0|0|1|any\n"));
        assertTrue(contract.contains(
                "W|flutter.widgets.OverflowBox\n"
                + "P|alignment|alignmentGeometry|0|-|-|"
                + "alignmentGeometry:alignmentGeometry\n"
                + "P|fit|enum|0|-|-|"
                + "enum:enum:cGFja2FnZTpmbHV0dGVyL3JlbmRlcmluZy5kYXJ0:"
                + "OverflowBoxFit:deferToChild,max\n"
                + "P|maxHeight|double|0|-|double:0:1:*:1|"
                + "double:range:0:1:*:1\n"
                + "P|maxWidth|double|0|-|double:0:1:*:1|"
                + "double:range:0:1:*:1\n"
                + "P|minHeight|double|0|-|double:0:1:*:1|"
                + "double:range:0:1:*:1\n"
                + "P|minWidth|double|0|-|double:0:1:*:1|"
                + "double:range:0:1:*:1\n"
                + "S|child|single|0|0|1|any\n"));
        assertTrue(contract.contains(
                "W|flutter.widgets.Stack\n"
                + "P|alignment|alignmentGeometry|0|-|-|"
                + "alignmentGeometry:alignmentGeometry\n"
                + "P|clipBehavior|enum|0|-|-|"
                + "enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:"
                + "Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none\n"
                + "P|fit|enum|0|-|-|"
                + "enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:"
                + "StackFit:expand,loose,passthrough\n"
                + "P|textDirection|enum|0|-|-|"
                + "enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:"
                + "TextDirection:ltr,rtl\n"
                + "S|children|list|0|0|10000|any\n"));
        assertTrue(contract.contains(
                "W|flutter.widgets.Expanded\n"
                + "P|flex|integer|0|-|integer:0:1:9007199254740991:1|"
                + "integer:range:0:1:9007199254740991:1\n"
                + "S|child|single|1|1|1|any\n"
                + "R|flutter.widgets.Expanded|directParentSlot|"
                + "flutter.widgets.Column|children\n"
                + "R|flutter.widgets.Expanded|directParentSlot|"
                + "flutter.widgets.Row|children\n"
                + "C|flutter.widgets.Expanded|paletteCreate|"
                + "wrapExistingChild|child\n"));
        assertTrue(contract.contains(
                "W|flutter.widgets.Flexible\n"
                + "P|fit|enum|0|-|-|"
                + "enum:enum:cGFja2FnZTpmbHV0dGVyL3JlbmRlcmluZy5kYXJ0:"
                + "FlexFit:loose,tight\n"
                + "P|flex|integer|0|-|integer:0:1:9007199254740991:1|"
                + "integer:range:0:1:9007199254740991:1\n"
                + "S|child|single|1|1|1|any\n"
                + "R|flutter.widgets.Flexible|directParentSlot|"
                + "flutter.widgets.Column|children\n"
                + "R|flutter.widgets.Flexible|directParentSlot|"
                + "flutter.widgets.Row|children\n"
                + "C|flutter.widgets.Flexible|paletteCreate|"
                + "wrapExistingChild|child\n"));
        assertTrue(contract.contains(
                "W|flutter.widgets.Spacer\n"
                + "P|flex|integer|0|-|integer:1:1:9007199254740991:1|"
                + "integer:range:1:1:9007199254740991:1\n"
                + "R|flutter.widgets.Spacer|directParentSlot|"
                + "flutter.widgets.Column|children\n"
                + "R|flutter.widgets.Spacer|directParentSlot|"
                + "flutter.widgets.Row|children\n"));
        int spacerStart = contract.indexOf("W|flutter.widgets.Spacer\n");
        int spacerEnd = contract.indexOf("W|", spacerStart + 2);
        String spacerContract = contract.substring(spacerStart, spacerEnd);
        assertFalse(spacerContract.contains("\nC|"), spacerContract);
        assertTrue(contract.contains(
                "W|flutter.widgets.IntrinsicHeight\n"
                + "S|child|single|0|0|1|any\n"));
        assertTrue(contract.contains(
                "W|flutter.widgets.IntrinsicWidth\n"
                + "P|stepHeight|double|0|-|double:0:1:*:1|"
                + "double:range:0:1:*:1\n"
                + "P|stepWidth|double|0|-|double:0:1:*:1|"
                + "double:range:0:1:*:1\n"
                + "S|child|single|0|0|1|any\n"));
        assertTrue(contract.contains(
                "W|flutter.widgets.Offstage\n"
                + "P|offstage|boolean|0|-|-|boolean:any\n"
                + "S|child|single|0|0|1|any\n"));
        assertTrue(contract.contains(
                "W|flutter.widgets.SizedOverflowBox\n"
                + "P|alignment|alignmentGeometry|0|-|-|"
                + "alignmentGeometry:alignmentGeometry\n"
                + "P|size|size|1|size:100,100|-|"
                + "size:size:finiteNonNegative\n"
                + "S|child|single|0|0|1|any\n"));
        assertTrue(contract.contains(
                "W|flutter.widgets.Transform\n"
                + "P|alignment|alignmentGeometry|0|-|-|"
                + "alignmentGeometry:alignmentGeometry\n"
                + "P|filterQuality|enum|0|-|-|enum:enum:"
                + "cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:"
                + "FilterQuality:high,low,medium,none\n"
                + "P|origin|offset|0|-|-|offset:offset:finiteSigned\n"
                + "P|transform|matrix4|1|"
                + "matrix4:1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1|-|"
                + "matrix4:matrix4\n"
                + "P|transformHitTests|boolean|0|-|-|boolean:any\n"
                + "S|child|single|0|0|1|any\n"));
        assertTrue(contract.contains(
                "W|flutter.widgets.RotatedBox\n"
                + "P|quarterTurns|integer|1|integer:1|"
                + "integer:-9007199254740991:1:9007199254740991:1|"
                + "integer:range:-9007199254740991:1:9007199254740991:1\n"
                + "S|child|single|0|0|1|any\n"));
        assertTrue(contract.contains(
                "W|flutter.widgets.ListBody\n"
                + "P|mainAxis|enum|0|-|-|enum:enum:"
                + "cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:"
                + "Axis:horizontal,vertical\n"
                + "P|reverse|boolean|0|-|-|boolean:any\n"
                + "S|children|list|0|0|10000|any\n"));
        assertTrue(contract.contains(
                "W|flutter.widgets.OverflowBar\n"
                + "P|alignment|enum|0|-|-|enum:enum:"
                + "cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:"
                + "MainAxisAlignment:center,end,spaceAround,spaceBetween,"
                + "spaceEvenly,start\n"
                + "P|overflowAlignment|enum|0|-|-|enum:enum:"
                + "cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:"
                + "OverflowBarAlignment:center,end,start\n"
                + "P|overflowDirection|enum|0|-|-|enum:enum:"
                + "cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:"
                + "VerticalDirection:down,up\n"
                + "P|overflowSpacing|double|0|-|double:*:1:*:1|"
                + "double:range:*:1:*:1\n"
                + "P|spacing|double|0|-|double:*:1:*:1|"
                + "double:range:*:1:*:1\n"
                + "P|textDirection|enum|0|-|-|enum:enum:"
                + "cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:"
                + "TextDirection:ltr,rtl\n"
                + "S|children|list|0|0|10000|any\n"));
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
    void imageFullReviewedProjectionHasStableFingerprintWithoutSyntheticRelationLines()
            throws Exception {
        String contract = BuiltInWidgetCapabilityCatalog.reviewedCanvasSchemaContract();
        int start = contract.indexOf("W|flutter.widgets.Image\n");
        int end = contract.indexOf("W|", start + 2);
        String image = contract.substring(start, end);

        assertEquals(3_583, image.getBytes(StandardCharsets.UTF_8).length);
        assertEquals(
                "e3e0b2b0fbc7b678814d51a257191381fb872a39e797ac46c861954f3eee7b1a",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                        .digest(image.getBytes(StandardCharsets.UTF_8))));
        assertFalse(image.contains("\nR|"), image);
        assertFalse(image.contains("\nC|"), image);
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

    private record Destination(
            WidgetDefinition owner,
            SlotDefinition slot) {
    }
}
