package dev.flutter.netbeans.designer.generation;

import dev.flutter.netbeans.api.DartCandidateCapacityBudget;
import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.DartParameter;
import dev.flutter.netbeans.designer.catalog.DartSymbolReference;
import dev.flutter.netbeans.designer.catalog.PaletteMetadata;
import dev.flutter.netbeans.designer.catalog.PropertyDefinition;
import dev.flutter.netbeans.designer.catalog.PropertyValueConstraint;
import dev.flutter.netbeans.designer.catalog.SlotAcceptance;
import dev.flutter.netbeans.designer.catalog.SlotDefinition;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.model.ColorSource;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.Extensions;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.SlotCardinality;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.ThemeToken;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.designer.source.DartManagedRegionHashing;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DartRegionGeneratorTest {
    private static final String MATERIAL_IMPORT = "package:flutter/material.dart";
    private static final String WIDGETS_IMPORT = "package:flutter/widgets.dart";
    private static final String PROBE_IMPORT = "package:probe/probe.dart";
    private static final String ZERO_HASH = "0".repeat(64);

    @Test
    void builtInGoldenIsDeterministicAndMatchesExecutableFixture() {
        DesignerDocument document = homePageDocument(WidgetClassKind.STATELESS);
        DartRegionGenerator generator = new DartRegionGenerator();

        DartGenerationResult first = generator.generate(document, BuiltInWidgetCatalog.getDefault());
        DartGenerationResult second = generator.generate(document, BuiltInWidgetCatalog.getDefault());

        assertTrue(first.successful());
        assertEquals(first, second);
        GeneratedDartRegions generated = first.generated().orElseThrow();
        assertEquals(DartRegionGenerator.PROFILE_ID, generated.profileId());
        assertEquals(
                "import 'package:flutter/material.dart';\n",
                generated.imports().payload());
        assertEquals("""
                  @override
                  Widget build(BuildContext context) {
                    return Scaffold(
                      appBar: AppBar(
                        title: const Text('Hello from NetBeans'),
                      ),
                      body: Center(
                        child: ElevatedButton(
                          onPressed: onContinue,
                          child: const Text('Continue'),
                        ),
                      ),
                    );
                  }
                """, generated.build().payload());
        assertEquals(
                "7D7414B55709E54DEA29770C4D6F8D6F48A1DFC02505F62D192A4DE0333556B7",
                generated.imports().normalizedSha256());
        assertEquals(
                "4565291327B5F6A910BC99F3D3238A0F0C826058DD800C017F1D7C8250FA707E",
                generated.build().normalizedSha256());
        assertEquals(
                DartManagedRegionHashing.normalizedSha256(generated.build().payload()),
                generated.build().normalizedSha256());
        assertEquals(List.of(new DartImportDirective(MATERIAL_IMPORT, Optional.empty())),
                generated.importPlan().directives());
        assertCanonical(generated.imports());
        assertCanonical(generated.build());
        assertEquals(
                List.of(
                        "Widget",
                        "BuildContext",
                        "Scaffold",
                        "AppBar",
                        "Text",
                        "Center",
                        "ElevatedButton",
                        "Text"),
                generated.symbolOccurrences().stream()
                        .map(GeneratedDartSymbolOccurrence::symbolName)
                        .toList());
        assertTrue(generated.symbolOccurrences().stream().allMatch(occurrence ->
                generated.build().payload()
                        .substring(occurrence.offset(), occurrence.endOffset())
                        .equals(occurrence.symbolName())));
        assertTrue(generated.symbolOccurrences().stream().allMatch(occurrence ->
                occurrence.libraryUri().equals(MATERIAL_IMPORT)));
    }

    @Test
    void emitsAllSafePropertyKindsAndPrefixesContributorSymbols() {
        WidgetDefinition probe = allValuesProbe();
        WidgetCatalog catalog = WidgetCatalog.strict(List.of(probe));
        LinkedHashMap<PropertyName, PropertyValue> properties = new LinkedHashMap<>();
        properties.put(property("text"), new PropertyValue.StringValue("a'b\\c $d\n😀"));
        properties.put(property("enabled"), new PropertyValue.BooleanValue(true));
        properties.put(property("count"), new PropertyValue.IntegerValue(BigInteger.valueOf(42)));
        properties.put(property("ratio"), new PropertyValue.DoubleValue(BigDecimal.ONE));
        properties.put(property("mode"), new PropertyValue.EnumValue("ProbeMode", "fast"));
        properties.put(property("color"), new PropertyValue.ColorValue(0x80ABCDEFL));
        properties.put(property("padding"), new PropertyValue.EdgeInsetsValue(
                BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4)));
        properties.put(property("asset"), new PropertyValue.AssetValue("assets/$logo.png"));
        properties.put(property("handler"), new PropertyValue.CallbackValue("onTap"));
        WidgetNode root = new WidgetNode(
                StableId.random(), probe.typeId(), properties, Map.of(), Extensions.empty());

        DartGenerationResult result = new DartRegionGenerator().generate(
                document(root, WidgetClassKind.STATELESS), catalog);

        assertTrue(result.successful());
        GeneratedDartRegions generated = result.generated().orElseThrow();
        DartImportDirective contributor = generated.importPlan().directives().stream()
                .filter(value -> value.uri().equals(PROBE_IMPORT))
                .findFirst()
                .orElseThrow();
        String prefix = contributor.prefix().orElseThrow();
        assertTrue(prefix.matches("_nbfd_[0-9a-f]{12}"));
        assertEquals(Optional.empty(), generated.importPlan().directives().stream()
                .filter(value -> value.uri().equals(WIDGETS_IMPORT))
                .findFirst().orElseThrow().prefix());

        String build = generated.build().payload();
        assertTrue(build.contains("return " + prefix + ".Probe("));
        assertTrue(build.contains("'a\\'b\\\\c \\$d\\n😀'"));
        assertTrue(build.contains("enabled: true,"));
        assertTrue(build.contains("count: 42,"));
        assertTrue(build.contains("ratio: 1.0,"));
        assertTrue(build.contains("mode: " + prefix + ".ProbeMode.fast,"));
        assertTrue(build.contains("color: const Color(0x80ABCDEF),"));
        assertTrue(build.contains(
                "padding: const EdgeInsets.fromLTRB(1.0, 2.0, 3.0, 4.0),"));
        assertTrue(build.contains("asset: 'assets/\\$logo.png',"));
        assertTrue(build.contains("handler: onTap,"));
        assertFalse(build.contains("const " + prefix + ".Probe("));
        assertEquals(
                List.of(
                        "Widget",
                        "BuildContext",
                        "Probe",
                        "ProbeMode",
                        "Color",
                        "EdgeInsets"),
                generated.symbolOccurrences().stream()
                        .map(GeneratedDartSymbolOccurrence::symbolName)
                        .toList());
    }

    @Test
    void assemblesExpandedTextLeavesIntoTypedFlutterObjects() {
        LinkedHashMap<PropertyName, PropertyValue> properties = new LinkedHashMap<>();
        properties.put(property("data"), new PropertyValue.StringValue("Styled"));
        properties.put(property("localeLanguageCode"), new PropertyValue.StringValue("uk"));
        properties.put(property("localeScriptCode"), new PropertyValue.StringValue("Cyrl"));
        properties.put(property("localeCountryCode"), new PropertyValue.StringValue("UA"));
        properties.put(property("textScalerFactor"),
                new PropertyValue.DoubleValue(new BigDecimal("1.25")));
        properties.put(property("textHeightApplyFirstAscent"),
                new PropertyValue.BooleanValue(false));
        properties.put(property("textHeightLeadingDistribution"),
                new PropertyValue.EnumValue("TextLeadingDistribution", "even"));
        properties.put(property("styleColor"), new PropertyValue.ColorValue(0xFF112233L));
        properties.put(property("styleFontSize"),
                new PropertyValue.DoubleValue(new BigDecimal("18")));
        properties.put(property("styleFontWeight"),
                new PropertyValue.EnumValue("FontWeight", "w700"));
        properties.put(property("styleFontStyle"),
                new PropertyValue.EnumValue("FontStyle", "italic"));
        properties.put(property("styleLetterSpacing"),
                new PropertyValue.DoubleValue(new BigDecimal("-0.25")));
        properties.put(property("styleLocaleLanguageCode"),
                new PropertyValue.StringValue("en"));
        properties.put(property("styleFontFamily"),
                new PropertyValue.StringValue("Inter"));
        properties.put(property("styleFontFamilyFallback"),
                new PropertyValue.StringValue("Noto Sans\nRoboto"));
        properties.put(property("styleDecorationUnderline"),
                new PropertyValue.BooleanValue(true));
        properties.put(property("styleDecorationLineThrough"),
                new PropertyValue.BooleanValue(true));
        properties.put(property("strutFontSize"),
                new PropertyValue.DoubleValue(new BigDecimal("16")));
        properties.put(property("strutLeading"),
                new PropertyValue.DoubleValue(new BigDecimal("0.5")));
        properties.put(property("strutForceHeight"),
                new PropertyValue.BooleanValue(true));
        WidgetNode root = new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.Text"),
                properties,
                Map.of(),
                Extensions.empty());

        DartGenerationResult result = new DartRegionGenerator().generate(
                document(root, WidgetClassKind.STATELESS),
                BuiltInWidgetCatalog.getDefault());

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        String build = result.generated().orElseThrow().build().payload();
        assertTrue(build.contains("return Text("), build);
        assertTrue(build.contains("style: TextStyle("), build);
        assertTrue(build.contains("color: const Color(0xFF112233)"), build);
        assertTrue(build.contains("fontSize: 18.0"), build);
        assertTrue(build.contains("fontWeight: FontWeight.w700"), build);
        assertTrue(build.contains("fontStyle: FontStyle.italic"), build);
        assertTrue(build.contains("letterSpacing: -0.25"), build);
        assertTrue(build.contains(
                "locale: const Locale.fromSubtags(languageCode: 'en')"), build);
        assertTrue(build.contains(
                "fontFamilyFallback: const <String>['Noto Sans', 'Roboto']"), build);
        assertTrue(build.contains(
                "decoration: TextDecoration.combine(const <TextDecoration>["
                + "TextDecoration.underline, TextDecoration.lineThrough])"), build);
        assertTrue(build.contains("strutStyle: const StrutStyle("), build);
        assertTrue(build.contains("fontSize: 16.0"), build);
        assertTrue(build.contains("leading: 0.5"), build);
        assertTrue(build.contains("forceStrutHeight: true"), build);
        assertTrue(build.contains(
                "locale: const Locale.fromSubtags(languageCode: 'uk', "
                + "scriptCode: 'Cyrl', countryCode: 'UA')"), build);
        assertTrue(build.contains("textScaler: const TextScaler.linear(1.25)"), build);
        assertTrue(build.contains("textHeightBehavior: const TextHeightBehavior("), build);
        assertTrue(build.contains("applyHeightToFirstAscent: false"), build);
        assertTrue(build.contains("leadingDistribution: TextLeadingDistribution.even"), build);
    }

    @Test
    void emitsThemeBoundComplexTextStyleWithTypedConstructors() {
        LinkedHashMap<PropertyName, PropertyValue> properties = new LinkedHashMap<>();
        properties.put(property("data"), new PropertyValue.StringValue("Styled"));
        properties.put(property("selectionColor"), new PropertyValue.ThemeTokenValue(
                new ThemeToken("material.colorScheme.primary")));
        properties.put(property("styleThemeTextStyle"), new PropertyValue.ThemeTokenValue(
                new ThemeToken("material.textTheme.bodyLarge")));
        properties.put(property("styleFontSize"),
                new PropertyValue.DoubleValue(BigDecimal.valueOf(21)));
        properties.put(property("styleForeground"), new PropertyValue.PaintValue(
                new ColorSource.Theme(new ThemeToken("material.colorScheme.secondary")),
                PropertyValue.PaintValue.BlendMode.SRC_OVER,
                PropertyValue.PaintValue.Style.STROKE,
                new BigDecimal("2.5"),
                PropertyValue.PaintValue.StrokeCap.ROUND,
                PropertyValue.PaintValue.StrokeJoin.BEVEL,
                BigDecimal.valueOf(4),
                true,
                PropertyValue.PaintValue.FilterQuality.MEDIUM,
                false,
                Optional.of(new PropertyValue.PaintValue.BlurMask(
                        PropertyValue.PaintValue.BlurStyle.OUTER,
                        new BigDecimal("1.5")))));
        properties.put(property("styleShadows"), new PropertyValue.ShadowListValue(List.of(
                new PropertyValue.ShadowListValue.Shadow(
                        StableId.parse("192489fb-3bbb-46c5-9bac-c988f412218c"),
                        new ColorSource.Theme(new ThemeToken("material.colorScheme.shadow")),
                        new BigDecimal("-1.25"), new BigDecimal("2.5"),
                        BigDecimal.valueOf(4)))));
        properties.put(property("styleFontFeatures"),
                new PropertyValue.FontFeatureListValue(List.of(
                        new PropertyValue.FontFeatureListValue.FontFeature(
                                StableId.parse("d8ca6ff9-1aa5-4bb1-944b-fdd475b5359d"),
                                "liga", 1))));
        properties.put(property("styleFontVariations"),
                new PropertyValue.FontVariationListValue(List.of(
                        new PropertyValue.FontVariationListValue.FontVariation(
                                StableId.parse("2115406c-c05d-4323-81a2-7cfe7ea35dc4"),
                                "wght", BigDecimal.valueOf(700)))));
        properties.put(property("styleDecorationColor"),
                new PropertyValue.ThemeTokenValue(
                        new ThemeToken("material.colorScheme.error")));
        WidgetNode root = new WidgetNode(
                StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                properties, Map.of(), Extensions.empty());

        DartGenerationResult result = new DartRegionGenerator().generate(
                document(root, WidgetClassKind.STATELESS),
                BuiltInWidgetCatalog.getDefault());

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        GeneratedDartRegions generated = result.generated().orElseThrow();
        assertEquals("import 'package:flutter/material.dart';\n",
                generated.imports().payload());
        String build = generated.build().payload();
        assertTrue(build.contains(
                "selectionColor: Theme.of(context).colorScheme.primary"), build);
        assertTrue(build.contains(
                "(Theme.of(context).textTheme.bodyLarge ?? const TextStyle()).copyWith("),
                build);
        assertTrue(build.contains("fontSize: 21.0"), build);
        assertTrue(build.contains("foreground: (Paint()"), build);
        assertTrue(build.contains("..color = Theme.of(context).colorScheme.secondary"), build);
        assertTrue(build.contains("..style = PaintingStyle.stroke"), build);
        assertTrue(build.contains("..strokeWidth = 2.5"), build);
        assertTrue(build.contains("..maskFilter = const MaskFilter.blur(BlurStyle.outer, 1.5)"),
                build);
        assertTrue(build.contains("shadows: <Shadow>[Shadow("), build);
        assertTrue(build.contains("color: Theme.of(context).colorScheme.shadow"), build);
        assertTrue(build.contains("offset: const Offset(-1.25, 2.5)"), build);
        assertTrue(build.contains(
                "fontFeatures: const <FontFeature>[const FontFeature('liga', 1)]"), build);
        assertTrue(build.contains(
                "fontVariations: const <FontVariation>[const FontVariation('wght', 700.0)]"),
                build);
        assertTrue(build.contains(
                "decorationColor: Theme.of(context).colorScheme.error"), build);
        assertFalse(build.contains("const Text('Styled'"), build);
    }

    @Test
    void emitsAThemeOnlyTextStyleWithTheMaterialUmbrellaImport() {
        WidgetNode root = new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.Text"),
                Map.of(
                        property("data"), new PropertyValue.StringValue("Theme only"),
                        property("styleThemeTextStyle"),
                        new PropertyValue.ThemeTokenValue(
                                new ThemeToken("material.textTheme.bodyMedium"))),
                Map.of(),
                Extensions.empty());

        GeneratedDartRegions generated = new DartRegionGenerator().generate(
                document(root, WidgetClassKind.STATELESS),
                BuiltInWidgetCatalog.getDefault())
                .generated().orElseThrow();

        assertEquals("import 'package:flutter/material.dart';\n",
                generated.imports().payload());
        String build = generated.build().payload();
        assertTrue(build.contains(
                "style: Theme.of(context).textTheme.bodyMedium"), build);
        assertFalse(build.contains("copyWith("), build);
    }

    @Test
    void nestedThemeColorAloneSelectsTheMaterialUmbrellaImport() {
        WidgetNode root = new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.Text"),
                Map.of(
                        property("data"), new PropertyValue.StringValue("Nested theme"),
                        property("styleForeground"),
                        PropertyValue.PaintValue.defaults(new ColorSource.Theme(
                                new ThemeToken("material.colorScheme.primary")))),
                Map.of(),
                Extensions.empty());

        GeneratedDartRegions generated = new DartRegionGenerator().generate(
                document(root, WidgetClassKind.STATELESS),
                BuiltInWidgetCatalog.getDefault())
                .generated().orElseThrow();

        assertEquals("import 'package:flutter/material.dart';\n",
                generated.imports().payload());
        assertTrue(generated.build().payload().contains(
                "..color = Theme.of(context).colorScheme.primary"),
                generated.build().payload());
    }

    @Test
    void preservesExplicitEmptyComplexTextStyleLists() {
        WidgetNode root = new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.Text"),
                Map.of(
                        property("data"), new PropertyValue.StringValue("Empty"),
                        property("styleShadows"), new PropertyValue.ShadowListValue(List.of()),
                        property("styleFontFeatures"),
                        new PropertyValue.FontFeatureListValue(List.of()),
                        property("styleFontVariations"),
                        new PropertyValue.FontVariationListValue(List.of())),
                Map.of(),
                Extensions.empty());

        String build = new DartRegionGenerator().generate(
                document(root, WidgetClassKind.STATELESS),
                BuiltInWidgetCatalog.getDefault())
                .generated().orElseThrow().build().payload();

        assertTrue(build.contains("shadows: const <Shadow>[]"), build);
        assertTrue(build.contains("fontFeatures: const <FontFeature>[]"), build);
        assertTrue(build.contains("fontVariations: const <FontVariation>[]"), build);
    }

    @Test
    void symbolManifestIgnoresSymbolLikeStringsAndCallbackIdentifiers() {
        WidgetDefinition probe = allValuesProbe();
        LinkedHashMap<PropertyName, PropertyValue> properties = new LinkedHashMap<>();
        properties.put(property("text"),
                new PropertyValue.StringValue("Widget BuildContext Probe Color"));
        properties.put(property("enabled"), new PropertyValue.BooleanValue(true));
        properties.put(property("count"), new PropertyValue.IntegerValue(BigInteger.ONE));
        properties.put(property("ratio"), new PropertyValue.DoubleValue(BigDecimal.ONE));
        properties.put(property("mode"), new PropertyValue.EnumValue("ProbeMode", "fast"));
        properties.put(property("color"), new PropertyValue.ColorValue(0xFF000000L));
        properties.put(property("padding"), new PropertyValue.EdgeInsetsValue(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO));
        properties.put(property("asset"), new PropertyValue.AssetValue("Widget"));
        properties.put(property("handler"), new PropertyValue.CallbackValue("Widget"));
        WidgetNode root = new WidgetNode(
                StableId.random(),
                probe.typeId(),
                properties,
                Map.of(),
                Extensions.empty());

        GeneratedDartRegions generated = new DartRegionGenerator().generate(
                document(root, WidgetClassKind.STATELESS),
                WidgetCatalog.strict(List.of(probe)))
                .generated().orElseThrow();

        assertEquals(1, generated.symbolOccurrences().stream()
                .filter(value -> value.symbolName().equals("Widget"))
                .count());
        assertEquals(1, generated.symbolOccurrences().stream()
                .filter(value -> value.symbolName().equals("BuildContext"))
                .count());
        assertEquals(1, generated.symbolOccurrences().stream()
                .filter(value -> value.symbolName().equals("Probe"))
                .count());
    }

    @Test
    void generatedRegionsRejectOccurrenceThatDoesNotSelectExactPayloadText() {
        GeneratedDartRegions generated = new DartRegionGenerator().generate(
                homePageDocument(WidgetClassKind.STATELESS),
                BuiltInWidgetCatalog.getDefault())
                .generated().orElseThrow();
        GeneratedDartSymbolOccurrence valid = generated.symbolOccurrences().getFirst();
        GeneratedDartSymbolOccurrence shifted = new GeneratedDartSymbolOccurrence(
                valid.id(),
                valid.region(),
                valid.offset() + 1,
                valid.length(),
                valid.symbolName(),
                valid.libraryUri(),
                valid.modelPath(),
                valid.widgetId());

        assertThrows(IllegalArgumentException.class, () -> new GeneratedDartRegions(
                generated.imports(),
                generated.build(),
                generated.importPlan(),
                generated.profileId(),
                List.of(shifted)));
    }

    @Test
    void keepsOmittedExplicitNullAndListOrderDistinct() {
        WidgetDefinition leaf = leafDefinition();
        WidgetDefinition holder = holderDefinition();
        WidgetCatalog catalog = WidgetCatalog.strict(List.of(holder, leaf));
        WidgetNode first = leaf("first");
        WidgetNode second = leaf("second");
        WidgetNode root = new WidgetNode(
                StableId.random(),
                holder.typeId(),
                Map.of(),
                Map.of(
                        slot("nullableChild"), WidgetSlot.SingleSlot.empty(),
                        slot("children"), new WidgetSlot.ListSlot(List.of(first, second))),
                Extensions.empty());

        DartGenerationResult result = new DartRegionGenerator().generate(
                document(root, WidgetClassKind.STATELESS), catalog);

        assertTrue(result.successful());
        String build = result.generated().orElseThrow().build().payload();
        assertTrue(build.contains("nullableChild: null,"));
        assertFalse(build.contains("omittedChild:"));
        int firstIndex = build.indexOf("'first'");
        int secondIndex = build.indexOf("'second'");
        assertTrue(firstIndex >= 0 && secondIndex > firstIndex);
        assertTrue(build.contains("children: [\n"));
    }

    @Test
    void constructorArgumentsUseGlobalPositionalThenNamedOrder() {
        WidgetDefinition definition = orderedDefinition();
        WidgetNode child = leaf("child");
        WidgetCatalog catalog = WidgetCatalog.strict(List.of(definition, leafDefinition()));
        WidgetNode root = new WidgetNode(
                StableId.random(),
                definition.typeId(),
                Map.of(
                        property("namedLater"), new PropertyValue.StringValue("named-2"),
                        property("positional"), new PropertyValue.StringValue("pos-0"),
                        property("namedFirst"), new PropertyValue.StringValue("named-0")),
                Map.of(slot("namedMiddle"), WidgetSlot.SingleSlot.of(child)),
                Extensions.empty());

        String build = new DartRegionGenerator().generate(
                document(root, WidgetClassKind.STATELESS), catalog)
                .generated().orElseThrow().build().payload();

        int positional = build.indexOf("'pos-0'");
        int namedFirst = build.indexOf("namedFirst: 'named-0'");
        int namedMiddle = build.indexOf("namedMiddle:");
        int namedLater = build.indexOf("namedLater: 'named-2'");
        assertTrue(positional < namedFirst);
        assertTrue(namedFirst < namedMiddle);
        assertTrue(namedMiddle < namedLater);
    }

    @Test
    void emitsCatalogNamedConstructorAfterGeneratedPrefix() {
        WidgetDefinition definition = new WidgetDefinition(
                new WidgetTypeId("example.Named"),
                "Named",
                Optional.of("compact"),
                true,
                PROBE_IMPORT,
                List.of(PROBE_IMPORT),
                Set.of(),
                palette("Named"),
                List.of(),
                List.of());
        DesignerDocument document = document(
                WidgetNode.empty(StableId.random(), definition.typeId()),
                WidgetClassKind.STATELESS);

        GeneratedDartRegions generated = new DartRegionGenerator().generate(
                document, WidgetCatalog.strict(List.of(definition)))
                .generated().orElseThrow();
        String prefix = generated.importPlan().directives().stream()
                .filter(value -> value.uri().equals(PROBE_IMPORT))
                .findFirst().orElseThrow().prefix().orElseThrow();

        assertTrue(generated.build().payload().contains(
                "return const " + prefix + ".Named.compact();"));
    }

    @Test
    void rejectsOpaqueExpressionAtomicallyAtItsExactModelPath() {
        WidgetNode icon = new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.Icon"),
                Map.of(property("icon"), new PropertyValue.DartExpressionValue("Icons.star")),
                Map.of(),
                Extensions.empty());

        DartGenerationResult result = new DartRegionGenerator().generate(
                document(icon, WidgetClassKind.STATELESS), BuiltInWidgetCatalog.getDefault());

        assertFalse(result.successful());
        assertTrue(result.generated().isEmpty());
        DartGenerationDiagnostic diagnostic = result.diagnostics().getFirst();
        assertEquals(DartGenerationDiagnosticCode.DART_EXPRESSION_UNSUPPORTED, diagnostic.code());
        assertEquals("/root/properties/icon", diagnostic.path());
        assertEquals(Optional.of(DartManagedRegionId.BUILD), diagnostic.region());
    }

    @Test
    void rejectsStatefulDocumentWithoutProducingEitherRegion() {
        DartGenerationResult result = new DartRegionGenerator().generate(
                homePageDocument(WidgetClassKind.STATEFUL), BuiltInWidgetCatalog.getDefault());

        assertFalse(result.successful());
        assertTrue(result.generated().isEmpty());
        assertEquals(DartGenerationDiagnosticCode.UNSUPPORTED_WIDGET_KIND,
                result.diagnostics().getFirst().code());
        assertEquals("/source/widgetKind", result.diagnostics().getFirst().path());
    }

    @Test
    void runsSemanticValidationInternallyAndReturnsNoPartialOutput() {
        WidgetNode invalidText = new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.Text"),
                Map.of(),
                Map.of(),
                Extensions.empty());

        DartGenerationResult result = new DartRegionGenerator().generate(
                document(invalidText, WidgetClassKind.STATELESS),
                BuiltInWidgetCatalog.getDefault());

        assertFalse(result.modelValidation().valid());
        assertTrue(result.generated().isEmpty());
        assertEquals(DartGenerationDiagnosticCode.MODEL_INVALID,
                result.diagnostics().getFirst().code());
        assertEquals("/root/properties/data", result.diagnostics().getFirst().path());
    }

    @Test
    void enforcesExactUtf8OutputBudgetBeforePublishingRegions() {
        // Each input LF expands to the two-byte Dart escape "\\n".
        WidgetNode text = leaf("\n".repeat(300));
        DartRegionGenerator generator = new DartRegionGenerator(
                new DartGenerationLimits(128, 100, 1_000));

        DartGenerationResult result = generator.generate(
                document(text, WidgetClassKind.STATELESS),
                WidgetCatalog.strict(List.of(leafDefinition())));

        assertFalse(result.successful());
        assertTrue(result.generated().isEmpty());
        assertEquals(DartGenerationDiagnosticCode.OUTPUT_SIZE_LIMIT,
                result.diagnostics().getFirst().code());
        assertTrue(result.diagnostics().getFirst().message().contains("maxTotalPayloadUtf8Bytes")
                || result.diagnostics().getFirst().message().contains("build budget"));
    }

    @Test
    void acceptsExactByteBudgetAndRejectsOneByteLess() {
        DesignerDocument document = homePageDocument(WidgetClassKind.STATELESS);
        WidgetCatalog catalog = BuiltInWidgetCatalog.getDefault();
        GeneratedDartRegions baseline = new DartRegionGenerator().generate(document, catalog)
                .generated().orElseThrow();
        int exactBytes = baseline.totalUtf8Size();

        DartGenerationResult exact = new DartRegionGenerator(
                new DartGenerationLimits(exactBytes, 100, 1_000))
                .generate(document, catalog);
        DartGenerationResult oneByteShort = new DartRegionGenerator(
                new DartGenerationLimits(exactBytes - 1, 100, 1_000))
                .generate(document, catalog);

        assertTrue(exact.successful());
        assertFalse(oneByteShort.successful());
        assertEquals(DartGenerationDiagnosticCode.OUTPUT_SIZE_LIMIT,
                oneByteShort.diagnostics().getFirst().code());
    }

    @Test
    void enforcesImportLimitWithoutPublishingRegions() {
        WidgetDefinition definition = new WidgetDefinition(
                new WidgetTypeId("example.ManyImports"),
                "ManyImports",
                Optional.empty(),
                true,
                "package:example/widget.dart",
                List.of("package:example/extra.dart", "package:example/widget.dart"),
                Set.of(),
                palette("Many Imports"),
                List.of(),
                List.of());
        DartRegionGenerator generator = new DartRegionGenerator(
                new DartGenerationLimits(10_000, 1, 1_000));

        DartGenerationResult result = generator.generate(
                document(WidgetNode.empty(StableId.random(), definition.typeId()),
                        WidgetClassKind.STATELESS),
                WidgetCatalog.strict(List.of(definition)));

        assertFalse(result.successful());
        assertTrue(result.generated().isEmpty());
        assertEquals(DartGenerationDiagnosticCode.IMPORT_LIMIT,
                result.diagnostics().getFirst().code());
    }

    @Test
    void sharedProbeCapacityAccepts256TotalAndRejects257WithoutPartialRegions() {
        DartCandidateCapacityBudget capacity = new DartCandidateCapacityBudget(
                "generator-probe-boundary", 2 * 1024 * 1024, 256, 1);
        DartGenerationLimits generationLimits = new DartGenerationLimits(
                DartGenerationLimits.DEFAULT_MAX_TOTAL_PAYLOAD_UTF8_BYTES,
                DartGenerationLimits.DEFAULT_MAX_IMPORTS,
                DartGenerationLimits.DEFAULT_MAX_VALUE_CODE_POINTS,
                capacity);

        DartGenerationResult exact = new DartRegionGenerator(generationLimits)
                .generate(columnWithTextChildren(252),
                        BuiltInWidgetCatalog.getDefault());
        DartGenerationResult exceeded = new DartRegionGenerator(generationLimits)
                .generate(columnWithTextChildren(253),
                        BuiltInWidgetCatalog.getDefault());

        assertTrue(exact.successful());
        GeneratedDartRegions exactRegions = exact.generated().orElseThrow();
        assertEquals(255, exactRegions.symbolOccurrences().size());
        assertSame(capacity, exactRegions.candidateCapacityBudget());
        assertFalse(exceeded.successful());
        assertTrue(exceeded.generated().isEmpty());
        assertEquals(DartGenerationDiagnosticCode.SYMBOL_PROBE_LIMIT,
                exceeded.diagnostics().getFirst().code());
    }

    @Test
    void minimumUsableProbeCapacityIncludesTheRootConstructor() {
        DartCandidateCapacityBudget capacity = new DartCandidateCapacityBudget(
                "generator-minimum-probe-capacity", 100_000, 4, 1);
        DartGenerationLimits generationLimits = new DartGenerationLimits(
                99_999,
                DartGenerationLimits.DEFAULT_MAX_IMPORTS,
                DartGenerationLimits.DEFAULT_MAX_VALUE_CODE_POINTS,
                capacity);

        DartGenerationResult result = new DartRegionGenerator(generationLimits)
                .generate(
                        document(text("minimum"), WidgetClassKind.STATELESS),
                        BuiltInWidgetCatalog.getDefault());

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        GeneratedDartRegions regions = result.generated().orElseThrow();
        assertEquals(3, regions.symbolOccurrences().size());
        assertSame(capacity, regions.candidateCapacityBudget());
    }

    @Test
    void materialUmbrellaCountsAsOneImportAfterWidgetsReduction() {
        DartRegionGenerator generator = new DartRegionGenerator(
                new DartGenerationLimits(100_000, 1, 1_000));

        DartGenerationResult result = generator.generate(
                homePageDocument(WidgetClassKind.STATELESS),
                BuiltInWidgetCatalog.getDefault());

        assertTrue(result.successful());
        assertEquals(List.of(new DartImportDirective(MATERIAL_IMPORT, Optional.empty())),
                result.generated().orElseThrow().importPlan().directives());
    }

    @Test
    void rejectsUnpairedSurrogateInsteadOfSilentlyReplacingIt() {
        WidgetNode root = leaf("bad\uD800value");

        DartGenerationResult result = new DartRegionGenerator().generate(
                document(root, WidgetClassKind.STATELESS),
                WidgetCatalog.strict(List.of(leafDefinition())));

        assertFalse(result.successful());
        assertEquals(DartGenerationDiagnosticCode.INVALID_UNICODE,
                result.diagnostics().getFirst().code());
        assertEquals("/root/properties/value", result.diagnostics().getFirst().path());
    }

    @Test
    void returnedUtf8BytesAreDefensiveCopies() {
        GeneratedDartRegion imports = new DartRegionGenerator().generate(
                homePageDocument(WidgetClassKind.STATELESS), BuiltInWidgetCatalog.getDefault())
                .generated().orElseThrow().imports();
        byte[] first = imports.utf8Bytes();
        byte[] expected = imports.payload().getBytes(StandardCharsets.UTF_8);
        first[0] ^= 0x7F;

        assertNotEquals(first[0], imports.utf8Bytes()[0]);
        assertArrayEquals(expected, imports.utf8Bytes());
    }

    private static void assertCanonical(GeneratedDartRegion region) {
        assertFalse(region.payload().contains("\r"));
        assertTrue(region.payload().endsWith("\n"));
        assertFalse(region.payload().endsWith("\n\n"));
        assertArrayEquals(region.payload().getBytes(StandardCharsets.UTF_8), region.utf8Bytes());
    }

    private static DesignerDocument homePageDocument(WidgetClassKind kind) {
        WidgetNode title = text("Hello from NetBeans");
        WidgetNode appBar = new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.material.AppBar"),
                Map.of(),
                Map.of(slot("title"), WidgetSlot.SingleSlot.of(title)),
                Extensions.empty());
        WidgetNode button = new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.material.ElevatedButton"),
                Map.of(property("onPressed"), new PropertyValue.CallbackValue("onContinue")),
                Map.of(slot("child"), WidgetSlot.SingleSlot.of(text("Continue"))),
                Extensions.empty());
        WidgetNode center = new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.Center"),
                Map.of(),
                Map.of(slot("child"), WidgetSlot.SingleSlot.of(button)),
                Extensions.empty());
        WidgetNode scaffold = new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.material.Scaffold"),
                Map.of(),
                Map.of(
                        slot("appBar"), WidgetSlot.SingleSlot.of(appBar),
                        slot("body"), WidgetSlot.SingleSlot.of(center)),
                Extensions.empty());
        return document(scaffold, kind);
    }

    private static DesignerDocument columnWithTextChildren(int childCount) {
        ArrayList<WidgetNode> children = new ArrayList<>();
        for (int index = 0; index < childCount; index++) {
            children.add(text("value-" + index));
        }
        WidgetNode column = new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.Column"),
                Map.of(),
                Map.of(slot("children"), new WidgetSlot.ListSlot(children)),
                Extensions.empty());
        return document(column, WidgetClassKind.STATELESS);
    }

    private static WidgetNode text(String value) {
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.Text"),
                Map.of(property("data"), new PropertyValue.StringValue(value)),
                Map.of(),
                Extensions.empty());
    }

    private static DesignerDocument document(WidgetNode root, WidgetClassKind kind) {
        return new DesignerDocument(
                Optional.empty(),
                StableId.random(),
                new DartSourceDescriptor(
                        "sample.dart",
                        "Sample",
                        kind,
                        Optional.empty(),
                        new ManagedRegions(
                                new ManagedRegion(ZERO_HASH),
                                new ManagedRegion(ZERO_HASH))),
                Optional.empty(),
                root,
                Extensions.empty());
    }

    private static WidgetDefinition allValuesProbe() {
        ArrayList<PropertyDefinition> properties = new ArrayList<>();
        properties.add(propertyDefinition("text", DartParameter.positional(0),
                new PropertyValueConstraint.AnyValue(PropertyValueKind.STRING)));
        properties.add(propertyDefinition("enabled", DartParameter.named(0, true),
                new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN)));
        properties.add(propertyDefinition("count", DartParameter.named(1, true),
                new PropertyValueConstraint.IntegerRange(null, null)));
        properties.add(propertyDefinition("ratio", DartParameter.named(2, true),
                new PropertyValueConstraint.DoubleRange(null, true, null, true)));
        properties.add(propertyDefinition("mode", DartParameter.named(3, true),
                new PropertyValueConstraint.EnumValues(
                        new DartSymbolReference(PROBE_IMPORT, "ProbeMode"), List.of("slow", "fast"))));
        properties.add(propertyDefinition("color", DartParameter.named(4, true),
                new PropertyValueConstraint.AnyValue(PropertyValueKind.COLOR)));
        properties.add(propertyDefinition("padding", DartParameter.named(5, true),
                new PropertyValueConstraint.EdgeInsetsValues(false)));
        properties.add(propertyDefinition("asset", DartParameter.named(6, true),
                new PropertyValueConstraint.AnyValue(PropertyValueKind.ASSET)));
        properties.add(propertyDefinition("handler", DartParameter.named(7, true),
                new PropertyValueConstraint.CallbackReference()));
        return new WidgetDefinition(
                new WidgetTypeId("example.Probe"),
                "Probe",
                Optional.empty(),
                true,
                PROBE_IMPORT,
                List.of(PROBE_IMPORT),
                Set.of(),
                palette("Probe"),
                properties,
                List.of());
    }

    private static WidgetDefinition leafDefinition() {
        return new WidgetDefinition(
                new WidgetTypeId("example.Leaf"),
                "Leaf",
                Optional.empty(),
                true,
                PROBE_IMPORT,
                List.of(PROBE_IMPORT),
                Set.of(),
                palette("Leaf"),
                List.of(propertyDefinition(
                        "value",
                        DartParameter.positional(0),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.STRING))),
                List.of());
    }

    private static WidgetNode leaf(String value) {
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("example.Leaf"),
                Map.of(property("value"), new PropertyValue.StringValue(value)),
                Map.of(),
                Extensions.empty());
    }

    private static WidgetDefinition holderDefinition() {
        return new WidgetDefinition(
                new WidgetTypeId("example.Holder"),
                "Holder",
                Optional.empty(),
                true,
                PROBE_IMPORT,
                List.of(PROBE_IMPORT),
                Set.of(),
                palette("Holder"),
                List.of(),
                List.of(
                        slotDefinition("nullableChild", 0, SlotCardinality.SINGLE),
                        slotDefinition("children", 1, SlotCardinality.LIST),
                        slotDefinition("omittedChild", 2, SlotCardinality.SINGLE)));
    }

    private static WidgetDefinition orderedDefinition() {
        return new WidgetDefinition(
                new WidgetTypeId("example.Ordered"),
                "Ordered",
                Optional.empty(),
                true,
                PROBE_IMPORT,
                List.of(PROBE_IMPORT),
                Set.of(),
                palette("Ordered"),
                List.of(
                        propertyDefinition("namedLater", DartParameter.named(2, false),
                                new PropertyValueConstraint.AnyValue(PropertyValueKind.STRING)),
                        propertyDefinition("positional", DartParameter.positional(0),
                                new PropertyValueConstraint.AnyValue(PropertyValueKind.STRING)),
                        propertyDefinition("namedFirst", DartParameter.named(0, false),
                                new PropertyValueConstraint.AnyValue(PropertyValueKind.STRING))),
                List.of(new SlotDefinition(
                        slot("namedMiddle"),
                        DartParameter.named(1, false),
                        SlotCardinality.SINGLE,
                        0,
                        1,
                        new SlotAcceptance.AnyWidget())));
    }

    private static PropertyDefinition propertyDefinition(
            String name,
            DartParameter parameter,
            PropertyValueConstraint constraint) {
        return new PropertyDefinition(
                property(name), parameter, List.of(constraint), Optional.empty());
    }

    private static SlotDefinition slotDefinition(
            String name,
            int order,
            SlotCardinality cardinality) {
        return new SlotDefinition(
                slot(name),
                DartParameter.named(order, false),
                cardinality,
                0,
                cardinality == SlotCardinality.SINGLE ? 1 : 10_000,
                new SlotAcceptance.AnyWidget());
    }

    private static PaletteMetadata palette(String name) {
        return new PaletteMetadata("example", 1, 1, name);
    }

    private static PropertyName property(String name) {
        return new PropertyName(name);
    }

    private static SlotName slot(String name) {
        return new SlotName(name);
    }
}
