package io.github.vgrytsenko2022.project.theme;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FlutterProjectThemeCodecTest {
    private static final String FROZEN_V4_ROLE_OVERRIDE_DART_SHA256 =
            "19B60CF503844E4FF1B976AF61EBAAF92B90D4811A3F200B38DD55E771C1E566";

    @TempDir
    Path temporaryDirectory;

    private final FlutterProjectThemeCodec codec = new FlutterProjectThemeCodec();

    @Test
    void roundTripsCanonicalDefaultDescriptorDeterministically() throws Exception {
        byte[] dart = FlutterProjectThemeDartGenerator.generate(
                FlutterProjectTheme.defaultTheme("0".repeat(64)));
        FlutterProjectTheme theme = FlutterProjectTheme.defaultTheme(
                FlutterProjectThemeDigests.sha256(dart));

        byte[] first = codec.encode(theme);
        byte[] second = codec.encode(theme);
        String json = new String(first, StandardCharsets.UTF_8);

        assertArrayEquals(first, second);
        assertEquals(theme, codec.decode(first));
        assertTrue(json.endsWith("\n"));
        assertTrue(json.contains("\"format\" : \"netbeans-flutter-project-theme\""));
        assertTrue(json.contains("\"schemaVersion\" : 5"));
        assertTrue(json.contains("\"enabled\" : true"));
        assertTrue(json.indexOf("\"enabled\"") < json.indexOf("\"defaultMode\""));
        assertTrue(json.indexOf("\"defaultMode\"") < json.indexOf("\"themes\""));
        assertEquals(3, countOccurrences(json, "\"enabled\" : true"));
        assertTrue(json.contains("\"seedArgb\" : \"0xFF6750A4\""));
        assertEquals(2, countOccurrences(json, "\"colorScheme\" : { }"));
        assertEquals(2, countOccurrences(json, "\"textTheme\" : { }"));
        assertEquals(2, countOccurrences(json, "\"components\" : { }"));
        assertTrue(json.contains("\"dartFile\" : \"lib/theme/app_theme.dart\""));
        assertTrue(json.contains("\"sha256\" : \""
                + theme.generated().sha256() + "\""));
    }

    @Test
    void generatedDartHasStableApiHeaderMapAndExactDigest() {
        FlutterProjectTheme provisional = FlutterProjectTheme.defaultTheme("0".repeat(64));

        byte[] first = FlutterProjectThemeDartGenerator.generate(provisional);
        byte[] second = FlutterProjectThemeDartGenerator.generate(provisional);
        String dart = new String(first, StandardCharsets.UTF_8);

        assertArrayEquals(first, second);
        assertTrue(dart.startsWith("// GENERATED FILE - DO NOT EDIT.\n"
                + "// Source: .fd_templates/project.fdtheme.\n"));
        assertTrue(dart.contains("Map<String, ThemeData>.unmodifiable"));
        assertTrue(dart.contains("'light': ThemeData.from("));
        assertTrue(dart.contains("brightness: Brightness.light"));
        assertTrue(dart.contains("'dark': ThemeData.from("));
        assertTrue(dart.contains("brightness: Brightness.dark"));
        assertTrue(dart.contains("seedColor: const Color(0xFF6750A4)"));
        assertTrue(dart.contains("static ThemeData resolve(String id)"));
        assertTrue(dart.contains("static ThemeData get light => resolve('light')"));
        assertTrue(dart.contains("static ThemeData get dark => resolve('dark')"));
        assertTrue(dart.contains("static ThemeMode get mode => ThemeMode.system;"));
        assertEquals(
                "766C2228657C5D19EB7A5C13A3449235F887EC84DFBC7D6491DA45BE1CB4994A",
                FlutterProjectThemeDigests.sha256(first));
        assertTrue(FlutterProjectThemeDigests.sha256(first).matches("[0-9A-F]{64}"));
    }

    @Test
    void decodesSchemaV1AsEnabledWithoutChangingItsGeneratedDartBytes()
            throws Exception {
        FlutterProjectTheme current = defaultTheme();
        String schemaV1 = new String(codec.encode(current), StandardCharsets.UTF_8)
                .replace("\"schemaVersion\" : 5,\n  \"enabled\" : true,",
                        "\"schemaVersion\" : 1,")
                .replace("    \"enabled\" : true,\n", "")
                .replaceAll("(?s),\\s*\"colorScheme\"\\s*:\\s*\\{\\s*\\}\\s*,"
                        + "\\s*\"textTheme\"\\s*:\\s*\\{\\s*\\}\\s*,"
                        + "\\s*\"components\"\\s*:\\s*\\{\\s*\\}", "");

        FlutterProjectTheme decoded = codec.decode(
                schemaV1.getBytes(StandardCharsets.UTF_8));

        assertTrue(decoded.enabled());
        assertTrue(decoded.themes().stream().allMatch(FlutterProjectThemeDefinition::enabled));
        assertArrayEquals(
                FlutterProjectThemeDartGenerator.generate(current),
                FlutterProjectThemeDartGenerator.generate(decoded));
    }

    @Test
    void decodesSchemaV2WithEveryCatalogThemeEnabled() throws Exception {
        FlutterProjectTheme current = defaultTheme();
        String schemaV2 = new String(codec.encode(current), StandardCharsets.UTF_8)
                .replace("\"schemaVersion\" : 5", "\"schemaVersion\" : 2")
                .replace("    \"enabled\" : true,\n", "")
                .replaceAll("(?s),\\s*\"colorScheme\"\\s*:\\s*\\{\\s*\\}\\s*,"
                        + "\\s*\"textTheme\"\\s*:\\s*\\{\\s*\\}\\s*,"
                        + "\\s*\"components\"\\s*:\\s*\\{\\s*\\}", "");

        FlutterProjectTheme decoded = codec.decode(schemaV2.getBytes(StandardCharsets.UTF_8));

        assertTrue(decoded.enabled());
        assertTrue(decoded.themes().stream().allMatch(FlutterProjectThemeDefinition::enabled));
        assertArrayEquals(
                FlutterProjectThemeDartGenerator.generate(current),
                FlutterProjectThemeDartGenerator.generate(decoded));
    }

    @Test
    void schemaV1MigrationDoesNotDependOnRootOrThemeFieldOrder() throws Exception {
        FlutterProjectTheme decoded = decode("""
                {
                  "themes": [
                    {
                      "seedArgb": "0xFF6750A4",
                      "brightness": "light",
                      "displayName": "Light",
                      "id": "light"
                    },
                    {
                      "displayName": "Dark",
                      "id": "dark",
                      "seedArgb": "0xFF6750A4",
                      "brightness": "dark"
                    }
                  ],
                  "darkThemeId": "dark",
                  "generated": {
                    "sha256": "%s",
                    "dartFile": "lib/theme/app_theme.dart"
                  },
                  "defaultMode": "system",
                  "schemaVersion": 1,
                  "lightThemeId": "light",
                  "format": "netbeans-flutter-project-theme"
                }
                """.formatted("A".repeat(64)));

        assertTrue(decoded.enabled());
        assertTrue(decoded.themes().stream().allMatch(FlutterProjectThemeDefinition::enabled));
        assertArrayEquals(
                FlutterProjectThemeDartGenerator.generate(defaultTheme()),
                FlutterProjectThemeDartGenerator.generate(decoded));
    }

    @Test
    void schemaV2MigrationDoesNotDependOnRootOrThemeFieldOrder() throws Exception {
        FlutterProjectTheme decoded = decode("""
                {
                  "themes": [
                    {
                      "brightness": "light",
                      "seedArgb": "0xFF6750A4",
                      "id": "light",
                      "displayName": "Light"
                    },
                    {
                      "seedArgb": "0xFF6750A4",
                      "displayName": "Dark",
                      "brightness": "dark",
                      "id": "dark"
                    }
                  ],
                  "enabled": false,
                  "lightThemeId": "light",
                  "format": "netbeans-flutter-project-theme",
                  "generated": {
                    "sha256": "%s",
                    "dartFile": "lib/theme/app_theme.dart"
                  },
                  "darkThemeId": "dark",
                  "schemaVersion": 2,
                  "defaultMode": "dark"
                }
                """.formatted("B".repeat(64)));

        assertFalse(decoded.enabled());
        assertEquals(FlutterThemeMode.DARK, decoded.defaultMode());
        assertTrue(decoded.themes().stream().allMatch(FlutterProjectThemeDefinition::enabled));
    }

    @Test
    void schemaV3PerThemeStateDoesNotDependOnRootOrThemeFieldOrder() throws Exception {
        FlutterProjectTheme decoded = decode("""
                {
                  "themes": [
                    {
                      "seedArgb": "0xFF6750A4",
                      "displayName": "Light",
                      "brightness": "light",
                      "id": "light",
                      "enabled": true
                    },
                    {
                      "enabled": true,
                      "id": "dark",
                      "brightness": "dark",
                      "seedArgb": "0xFF6750A4",
                      "displayName": "Dark"
                    },
                    {
                      "brightness": "light",
                      "enabled": false,
                      "displayName": "Unused",
                      "seedArgb": "0xFF123456",
                      "id": "unused"
                    }
                  ],
                  "darkThemeId": "dark",
                  "enabled": true,
                  "generated": {
                    "sha256": "%s",
                    "dartFile": "lib/theme/app_theme.dart"
                  },
                  "format": "netbeans-flutter-project-theme",
                  "defaultMode": "light",
                  "lightThemeId": "light",
                  "schemaVersion": 3
                }
                """.formatted("C".repeat(64)));

        assertTrue(decoded.enabled());
        assertTrue(decoded.themeById("light").orElseThrow().enabled());
        assertTrue(decoded.themeById("dark").orElseThrow().enabled());
        assertFalse(decoded.themeById("unused").orElseThrow().enabled());
        String dart = new String(
                FlutterProjectThemeDartGenerator.generate(decoded), StandardCharsets.UTF_8);
        assertFalse(dart.contains("'unused': ThemeData.from("));
    }

    @Test
    void explicitEnabledFlagsDoNotChangeAllEnabledGeneratedDart() {
        FlutterGeneratedThemeArtifact generated = new FlutterGeneratedThemeArtifact(
                FlutterProjectThemePaths.GENERATED_DART_WIRE_PATH, "D".repeat(64));
        FlutterProjectTheme compatibilityModel = new FlutterProjectTheme(
                FlutterThemeMode.SYSTEM,
                "light",
                "dark",
                List.of(
                        new FlutterProjectThemeDefinition(
                                "light", "Light", FlutterThemeBrightness.LIGHT, 0xFF112233),
                        new FlutterProjectThemeDefinition(
                                "dark", "Dark", FlutterThemeBrightness.DARK, 0xFF445566),
                        new FlutterProjectThemeDefinition(
                                "custom", "Custom", FlutterThemeBrightness.LIGHT, 0xFF778899)),
                generated);
        FlutterProjectTheme explicitV3Model = new FlutterProjectTheme(
                true,
                FlutterThemeMode.SYSTEM,
                "light",
                "dark",
                List.of(
                        new FlutterProjectThemeDefinition(
                                "light", "Light", FlutterThemeBrightness.LIGHT,
                                0xFF112233, true),
                        new FlutterProjectThemeDefinition(
                                "dark", "Dark", FlutterThemeBrightness.DARK,
                                0xFF445566, true),
                        new FlutterProjectThemeDefinition(
                                "custom", "Custom", FlutterThemeBrightness.LIGHT,
                                0xFF778899, true)),
                generated);

        assertArrayEquals(
                FlutterProjectThemeDartGenerator.generate(compatibilityModel),
                FlutterProjectThemeDartGenerator.generate(explicitV3Model));
    }

    @Test
    void roundTripsTypedV4OverridesAndGeneratesSemanticMaterialCopyWith() throws Exception {
        EnumMap<FlutterMaterialColorRole, Integer> colors =
                new EnumMap<>(FlutterMaterialColorRole.class);
        colors.put(FlutterMaterialColorRole.PRIMARY, 0xFF123456);
        colors.put(FlutterMaterialColorRole.ON_PRIMARY, 0xFFFFFFFF);
        FlutterThemeTextStyleOverride body = new FlutterThemeTextStyleOverride(
                Optional.of(new FlutterThemeColorValue.ColorRole(
                        FlutterMaterialColorRole.ON_SURFACE)),
                Optional.of(new FlutterThemeColorValue.Literal(0x1A000000)),
                Optional.of(16.0d),
                Optional.of(FlutterThemeFontWeight.W600),
                Optional.of(FlutterThemeFontStyle.ITALIC),
                Optional.of(0.25d),
                Optional.of(1.0d),
                Optional.of(1.4d),
                Optional.of("Noto Sans"),
                Optional.of(Set.of(
                        FlutterThemeTextDecorationLine.UNDERLINE,
                        FlutterThemeTextDecorationLine.LINE_THROUGH)),
                Optional.of(new FlutterThemeColorValue.ColorRole(
                        FlutterMaterialColorRole.PRIMARY)),
                Optional.of(FlutterThemeTextDecorationStyle.DASHED),
                Optional.of(2.0d));
        FlutterThemeOverrides overrides = new FlutterThemeOverrides(
                colors, Map.of(FlutterMaterialTextStyleRole.BODY_MEDIUM, body));
        FlutterProjectTheme base = defaultTheme();
        FlutterProjectThemeDefinition light = new FlutterProjectThemeDefinition(
                "light", "Light", FlutterThemeBrightness.LIGHT, 0xFF6750A4,
                true, overrides);
        FlutterProjectTheme theme = new FlutterProjectTheme(
                true, base.defaultMode(), "light", "dark",
                List.of(light, base.darkTheme()), base.generated());

        byte[] encoded = codec.encode(theme);
        String json = new String(encoded, StandardCharsets.UTF_8);
        String dart = new String(
                FlutterProjectThemeDartGenerator.generate(theme), StandardCharsets.UTF_8);

        assertEquals(theme, codec.decode(encoded));
        assertTrue(json.contains("\"primary\" : \"0xFF123456\""));
        assertTrue(json.contains("\"kind\" : \"colorScheme\""));
        assertTrue(json.contains("\"decoration\" : ["));
        assertTrue(json.indexOf("\"underline\"") < json.indexOf("\"lineThrough\""));
        assertTrue(dart.contains("final colorScheme = ColorScheme.fromSeed("));
        assertTrue(dart.contains("primary: const Color(0xFF123456)"));
        assertTrue(dart.contains("bodyMedium: (base.textTheme.bodyMedium"));
        assertTrue(dart.contains("color: colorScheme.onSurface"));
        assertTrue(dart.contains("TextDecoration.combine(const <TextDecoration>["));
    }

    @Test
    void schemaV4MigratesToEmptyComponentsWithoutChangingGeneratedDartBytes()
            throws Exception {
        FlutterThemeTextStyleOverride textOverride = new FlutterThemeTextStyleOverride(
                Optional.of(new FlutterThemeColorValue.ColorRole(
                        FlutterMaterialColorRole.ON_SURFACE)),
                Optional.empty(), Optional.of(15.0d), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty());
        FlutterProjectTheme base = defaultTheme();
        FlutterProjectThemeDefinition light = new FlutterProjectThemeDefinition(
                "light", "Light", FlutterThemeBrightness.LIGHT, 0xFF6750A4, true,
                new FlutterThemeOverrides(
                        Map.of(FlutterMaterialColorRole.PRIMARY, 0xFF123456),
                        Map.of(FlutterMaterialTextStyleRole.BODY_MEDIUM, textOverride)));
        FlutterProjectTheme current = new FlutterProjectTheme(
                true, base.defaultMode(), "light", "dark",
                List.of(light, base.darkTheme()), base.generated());
        byte[] legacyDart = FlutterProjectThemeDartGenerator.generate(current);
        assertEquals(FROZEN_V4_ROLE_OVERRIDE_DART_SHA256,
                FlutterProjectThemeDigests.sha256(legacyDart));
        FlutterProjectTheme verifiedV4 = new FlutterProjectTheme(
                current.enabled(), current.defaultMode(), current.lightThemeId(),
                current.darkThemeId(), current.themes(),
                new FlutterGeneratedThemeArtifact(
                        FlutterProjectThemePaths.GENERATED_DART_WIRE_PATH,
                        FROZEN_V4_ROLE_OVERRIDE_DART_SHA256));
        String schemaV4 = new String(codec.encode(verifiedV4), StandardCharsets.UTF_8)
                .replace("\"schemaVersion\" : 5", "\"schemaVersion\" : 4")
                .replaceAll(",\\s*\"components\"\\s*:\\s*\\{\\s*\\}", "");

        Path project = Files.createDirectory(temporaryDirectory.resolve("verified-v4"));
        Path descriptor = project.resolve(FlutterProjectThemePaths.DESCRIPTOR_PATH);
        Path dartFile = project.resolve(FlutterProjectThemePaths.GENERATED_DART_PATH);
        Files.createDirectories(descriptor.getParent());
        Files.createDirectories(dartFile.getParent());
        Files.writeString(descriptor, schemaV4, StandardCharsets.UTF_8);
        Files.write(dartFile, legacyDart);
        assertEquals(FlutterProjectThemeLoadStatus.VALID,
                new FlutterProjectThemeStore().load(project).status());

        FlutterProjectTheme migrated = codec.decode(
                schemaV4.getBytes(StandardCharsets.UTF_8));

        assertTrue(migrated.themes().stream()
                .allMatch(theme -> theme.overrides().componentColors().isEmpty()));
        assertEquals(current.lightTheme().overrides().colorScheme(),
                migrated.lightTheme().overrides().colorScheme());
        assertEquals(current.lightTheme().overrides().textTheme(),
                migrated.lightTheme().overrides().textTheme());
        assertEquals(FROZEN_V4_ROLE_OVERRIDE_DART_SHA256,
                migrated.generated().sha256());
        assertArrayEquals(legacyDart,
                FlutterProjectThemeDartGenerator.generate(migrated));

        byte[] canonicalV5 = codec.encode(migrated);
        String canonicalV5Json = new String(canonicalV5, StandardCharsets.UTF_8);
        assertTrue(canonicalV5Json.contains("\"schemaVersion\" : 5"));
        assertEquals(2, countOccurrences(canonicalV5Json, "\"components\" : { }"));
        Files.write(descriptor, canonicalV5);
        assertEquals(FlutterProjectThemeLoadStatus.VALID,
                new FlutterProjectThemeStore().load(project).status());
    }

    @Test
    void componentColorsRoundTripAndGenerateClosedThemeDataContracts()
            throws Exception {
        assertEquals(36, FlutterThemeComponentColorRole.values().length);
        EnumMap<FlutterThemeComponentColorRole, FlutterThemeColorValue> components =
                new EnumMap<>(FlutterThemeComponentColorRole.class);
        components.put(FlutterThemeComponentColorRole.SCAFFOLD_BACKGROUND,
                new FlutterThemeColorValue.Literal(0xFF102030));
        components.put(FlutterThemeComponentColorRole.APP_BAR_FOREGROUND,
                new FlutterThemeColorValue.ColorRole(FlutterMaterialColorRole.ON_PRIMARY));
        components.put(FlutterThemeComponentColorRole.ICON_COLOR,
                new FlutterThemeColorValue.ColorRole(FlutterMaterialColorRole.SECONDARY));
        components.put(FlutterThemeComponentColorRole.ELEVATED_BUTTON_BACKGROUND_DEFAULT,
                new FlutterThemeColorValue.ColorRole(FlutterMaterialColorRole.PRIMARY));
        components.put(FlutterThemeComponentColorRole.ELEVATED_BUTTON_BACKGROUND_DISABLED,
                new FlutterThemeColorValue.Literal(0x61102030));
        components.put(FlutterThemeComponentColorRole.ELEVATED_BUTTON_ICON_FOCUSED,
                new FlutterThemeColorValue.ColorRole(FlutterMaterialColorRole.TERTIARY));
        components.put(FlutterThemeComponentColorRole.ELEVATED_BUTTON_OVERLAY_PRESSED,
                new FlutterThemeColorValue.Literal(0x22102030));
        FlutterProjectTheme base = defaultTheme();
        FlutterProjectThemeDefinition light = new FlutterProjectThemeDefinition(
                "light", "Light", FlutterThemeBrightness.LIGHT, 0xFF6750A4, true,
                new FlutterThemeOverrides(Map.of(), Map.of(), components));
        FlutterProjectTheme theme = new FlutterProjectTheme(
                true, base.defaultMode(), "light", "dark",
                List.of(light, base.darkTheme()), base.generated());

        byte[] encoded = codec.encode(theme);
        String json = new String(encoded, StandardCharsets.UTF_8);
        String dart = new String(
                FlutterProjectThemeDartGenerator.generate(theme), StandardCharsets.UTF_8);

        assertEquals(theme, codec.decode(encoded));
        assertTrue(json.contains("\"scaffold.backgroundColor\""));
        assertTrue(json.contains("\"elevatedButton.iconColor.focused\""));
        assertTrue(dart.contains("scaffoldBackgroundColor: const Color(0xFF102030)"));
        assertTrue(dart.contains("appBarTheme: base.appBarTheme.copyWith("));
        assertTrue(dart.contains("foregroundColor: colorScheme.onPrimary"));
        assertTrue(dart.contains("iconTheme: base.iconTheme.copyWith("));
        assertTrue(dart.contains("elevatedButtonTheme: ElevatedButtonThemeData("));
        assertTrue(dart.contains("backgroundColor: WidgetStateProperty.resolveWith<Color?>"));
        assertTrue(dart.contains("states.contains(WidgetState.disabled)"));
        assertTrue(dart.contains("return colorScheme.primary;"));
        assertTrue(dart.contains("iconColor: WidgetStateProperty.resolveWith<Color?>"));
        assertTrue(dart.contains(
                ").merge(base.elevatedButtonTheme.style),"),
                "component ButtonStyle must keep unrelated base theme fields");
        int overlay = dart.indexOf(
                "overlayColor: WidgetStateProperty.resolveWith<Color?>");
        int disabledGuard = dart.indexOf(
                "if (states.contains(WidgetState.disabled))", overlay);
        int nullFallback = dart.indexOf("return null;", disabledGuard);
        int pressedBranch = dart.indexOf(
                "if (states.contains(WidgetState.pressed))", disabledGuard);
        assertTrue(overlay >= 0 && disabledGuard > overlay
                && nullFallback > disabledGuard && pressedBranch > nullFallback,
                "disabled must terminate with null before pressed/hovered/focused");
    }

    @Test
    void componentBuildersUseUniqueLintSafeNamesWithoutChangingLegacyBuilders() {
        FlutterThemeOverrides component = new FlutterThemeOverrides(
                Map.of(), Map.of(), Map.of(
                        FlutterThemeComponentColorRole.ICON_COLOR,
                        new FlutterThemeColorValue.Literal(0xFF123456)));
        FlutterProjectTheme base = defaultTheme();
        FlutterProjectTheme theme = new FlutterProjectTheme(
                true, FlutterThemeMode.SYSTEM, "a_b", "dark",
                List.of(
                        new FlutterProjectThemeDefinition(
                                "a_b", "A B", FlutterThemeBrightness.LIGHT,
                                0xFF6750A4, true, component),
                        new FlutterProjectThemeDefinition(
                                "a__b", "A double B", FlutterThemeBrightness.LIGHT,
                                0xFF123456, true, component),
                        base.darkTheme()),
                base.generated());

        String dart = new String(
                FlutterProjectThemeDartGenerator.generate(theme), StandardCharsets.UTF_8);

        assertTrue(dart.contains("'a_b': _buildComponentTheme0(),"));
        assertTrue(dart.contains("'a__b': _buildComponentTheme1(),"));
        assertTrue(dart.contains("static ThemeData _buildComponentTheme0()"));
        assertTrue(dart.contains("static ThemeData _buildComponentTheme1()"));
        assertFalse(dart.contains("_build_a_b"));
        assertFalse(dart.contains("_build_a__b"));
    }

    @Test
    void schemaV5ComponentContractRejectsUnknownMissingAndLegacyInjection()
            throws Exception {
        String valid = new String(codec.encode(defaultTheme()), StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> codec.decode(valid.replaceFirst(
                "\"components\" : \\{ \\}",
                "\"components\" : { \"unknown.color\" : "
                + "{ \"kind\" : \"argb\", \"argb\" : \"0xFF000000\" } }")
                .getBytes(StandardCharsets.UTF_8)));
        assertThrows(IOException.class, () -> codec.decode(valid.replaceFirst(
                ",\\s*\"components\"\\s*:\\s*\\{\\s*\\}", "")
                .getBytes(StandardCharsets.UTF_8)));
        String schemaV4WithComponents = valid.replace(
                "\"schemaVersion\" : 5", "\"schemaVersion\" : 4");
        assertThrows(IOException.class, () -> codec.decode(
                schemaV4WithComponents.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void disabledCatalogThemeRoundTripsButIsOmittedFromGeneratedMap() throws Exception {
        FlutterProjectTheme current = defaultTheme();
        FlutterProjectThemeDefinition custom = new FlutterProjectThemeDefinition(
                "custom", "Custom", FlutterThemeBrightness.LIGHT, 0xFF123456, false);
        FlutterProjectTheme withDisabledCustom = new FlutterProjectTheme(
                true,
                current.defaultMode(),
                current.lightThemeId(),
                current.darkThemeId(),
                List.of(current.lightTheme(), current.darkTheme(), custom),
                current.generated());

        String descriptor = new String(codec.encode(withDisabledCustom), StandardCharsets.UTF_8);
        String dart = new String(
                FlutterProjectThemeDartGenerator.generate(withDisabledCustom),
                StandardCharsets.UTF_8);

        assertTrue(descriptor.contains("\"id\" : \"custom\""));
        assertTrue(descriptor.contains("\"enabled\" : false"));
        assertFalse(dart.contains("'custom': ThemeData.from("));
        assertTrue(dart.contains("'light': ThemeData.from("));
        assertTrue(dart.contains("'dark': ThemeData.from("));
        assertEquals(withDisabledCustom, codec.decode(codec.encode(withDisabledCustom)));
    }

    @Test
    void disabledThemeKeepsCatalogButExposesNullableUnwiredAccessors() throws Exception {
        FlutterProjectTheme enabled = defaultTheme();
        FlutterProjectTheme disabled = new FlutterProjectTheme(
                false,
                enabled.defaultMode(),
                enabled.lightThemeId(),
                enabled.darkThemeId(),
                enabled.themes(),
                enabled.generated());

        byte[] first = FlutterProjectThemeDartGenerator.generate(disabled);
        byte[] second = FlutterProjectThemeDartGenerator.generate(disabled);
        String dart = new String(first, StandardCharsets.UTF_8);

        assertArrayEquals(first, second);
        assertTrue(dart.contains("'light': ThemeData.from("));
        assertTrue(dart.contains("'dark': ThemeData.from("));
        assertTrue(dart.contains("static ThemeData? get light => null;"));
        assertTrue(dart.contains("static ThemeData? get dark => null;"));
        assertTrue(dart.contains("static ThemeMode? get mode => null;"));
        FlutterProjectTheme decoded = codec.decode(codec.encode(disabled));
        assertEquals(disabled, decoded);
        assertFalse(decoded.enabled());
    }

    @Test
    void globalDisablePermitsDisabledSelectedThemesAndPreservesReferences() throws Exception {
        FlutterProjectTheme current = defaultTheme();
        FlutterProjectTheme disabled = new FlutterProjectTheme(
                false,
                current.defaultMode(),
                current.lightThemeId(),
                current.darkThemeId(),
                List.of(
                        new FlutterProjectThemeDefinition(
                                "light", "Light", FlutterThemeBrightness.LIGHT,
                                FlutterProjectTheme.DEFAULT_SEED_ARGB, false),
                        new FlutterProjectThemeDefinition(
                                "dark", "Dark", FlutterThemeBrightness.DARK,
                                FlutterProjectTheme.DEFAULT_SEED_ARGB, false)),
                current.generated());

        FlutterProjectTheme decoded = codec.decode(codec.encode(disabled));
        String dart = new String(
                FlutterProjectThemeDartGenerator.generate(decoded), StandardCharsets.UTF_8);

        assertEquals("light", decoded.lightThemeId());
        assertEquals("dark", decoded.darkThemeId());
        assertTrue(decoded.themes().stream().noneMatch(FlutterProjectThemeDefinition::enabled));
        assertFalse(dart.contains("ThemeData.from("));
        assertTrue(dart.contains("static ThemeData? get light => null;"));
        assertTrue(dart.contains("static ThemeData? get dark => null;"));
    }

    @Test
    void rejectsUnknownDuplicateAndNonCanonicalWireValues() throws Exception {
        String valid = new String(codec.encode(defaultTheme()), StandardCharsets.UTF_8);

        assertThrows(IOException.class, () -> codec.decode(valid.replaceFirst(
                "\"format\"", "\"unknown\" : true,\n  \"format\"")
                .getBytes(StandardCharsets.UTF_8)));
        assertThrows(IOException.class, () -> codec.decode(valid.replaceFirst(
                "\"schemaVersion\" : 5", "\"schemaVersion\" : 5,\n"
                + "  \"schemaVersion\" : 5").getBytes(StandardCharsets.UTF_8)));
        assertThrows(IOException.class, () -> codec.decode(valid.replaceFirst(
                "\"enabled\" : true", "\"enabled\" : \"true\"")
                .getBytes(StandardCharsets.UTF_8)));
        assertThrows(IOException.class, () -> codec.decode(valid.replace(
                "  \"enabled\" : true,\n", "").getBytes(StandardCharsets.UTF_8)));
        assertThrows(IOException.class, () -> codec.decode(valid.replace(
                "\"schemaVersion\" : 5", "\"schemaVersion\" : 6")
                .getBytes(StandardCharsets.UTF_8)));
        assertThrows(IOException.class, () -> codec.decode(valid.replace(
                "\"schemaVersion\" : 5", "\"schemaVersion\" : 1")
                .getBytes(StandardCharsets.UTF_8)));
        assertThrows(IOException.class, () -> codec.decode(valid.replaceFirst(
                "    \"enabled\" : true,\n", "")
                .getBytes(StandardCharsets.UTF_8)));
        assertThrows(IOException.class, () -> codec.decode(valid.replace(
                "0xFF6750A4", "0xff6750a4").getBytes(StandardCharsets.UTF_8)));
        assertThrows(IOException.class, () -> codec.decode(valid.replaceFirst(
                "0xFF6750A4", "0x006750A4").getBytes(StandardCharsets.UTF_8)));
        assertThrows(IOException.class, () -> codec.decode(valid.replace(
                defaultTheme().generated().sha256(),
                defaultTheme().generated().sha256().toLowerCase())
                .getBytes(StandardCharsets.UTF_8)));
        assertThrows(IOException.class, () -> codec.decode(valid.replaceFirst(
                "\"defaultMode\" : \"system\"",
                "\"defaultMode\" : \"automatic\"")
                .getBytes(StandardCharsets.UTF_8)));
        assertThrows(IOException.class, () -> codec.decode(valid.replaceFirst(
                "\"displayName\" : \"Light\"",
                "\"displayName\" : \"\\\\uD800\"")
                .getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void modelRejectsDuplicateMissingOrWrongBrightnessReferencesAndCatalogOverflow() {
        FlutterGeneratedThemeArtifact generated = new FlutterGeneratedThemeArtifact(
                FlutterProjectThemePaths.GENERATED_DART_WIRE_PATH, "A".repeat(64));
        FlutterProjectThemeDefinition light = definition(
                "light", FlutterThemeBrightness.LIGHT);
        FlutterProjectThemeDefinition dark = definition(
                "dark", FlutterThemeBrightness.DARK);

        assertThrows(IllegalArgumentException.class,
                () -> new FlutterProjectThemeDefinition(
                        "transparent", "Transparent", FlutterThemeBrightness.LIGHT,
                        0x006750A4));

        assertThrows(IllegalArgumentException.class, () -> new FlutterProjectTheme(
                FlutterThemeMode.SYSTEM, "light", "dark",
                List.of(light, light, dark), generated));
        assertThrows(IllegalArgumentException.class, () -> new FlutterProjectTheme(
                FlutterThemeMode.SYSTEM, "missing", "dark",
                List.of(light, dark), generated));
        assertThrows(IllegalArgumentException.class, () -> new FlutterProjectTheme(
                FlutterThemeMode.SYSTEM, "dark", "light",
                List.of(light, dark), generated));
        FlutterProjectThemeDefinition disabledLight = new FlutterProjectThemeDefinition(
                "light", "light", FlutterThemeBrightness.LIGHT,
                FlutterProjectTheme.DEFAULT_SEED_ARGB, false);
        assertThrows(IllegalArgumentException.class, () -> new FlutterProjectTheme(
                true, FlutterThemeMode.SYSTEM, "light", "dark",
                List.of(disabledLight, dark), generated));

        List<FlutterProjectThemeDefinition> tooMany = new ArrayList<>();
        tooMany.add(light);
        tooMany.add(dark);
        for (int index = 0; index < FlutterProjectTheme.MAX_THEMES - 1; index++) {
            tooMany.add(definition("custom_" + index, FlutterThemeBrightness.LIGHT));
        }
        assertThrows(IllegalArgumentException.class, () -> new FlutterProjectTheme(
                FlutterThemeMode.SYSTEM, "light", "dark", tooMany, generated));
    }

    @Test
    void rejectsOversizedDescriptorBeforeParsing() {
        byte[] oversized = new byte[FlutterProjectThemeCodec.MAX_DESCRIPTOR_BYTES + 1];
        assertThrows(IOException.class, () -> codec.decode(oversized));
    }

    @Test
    void completeMaximumCatalogRemainsInsideBoundedCodecAndGeneratorBudgets()
            throws Exception {
        EnumMap<FlutterMaterialColorRole, Integer> colors =
                new EnumMap<>(FlutterMaterialColorRole.class);
        for (FlutterMaterialColorRole role : FlutterMaterialColorRole.values()) {
            colors.put(role, 0xFF000000 | role.ordinal());
        }
        FlutterThemeTextStyleOverride style = new FlutterThemeTextStyleOverride(
                Optional.of(new FlutterThemeColorValue.ColorRole(
                        FlutterMaterialColorRole.ON_SURFACE)),
                Optional.of(new FlutterThemeColorValue.Literal(0x11000000)),
                Optional.of(14.0d), Optional.of(FlutterThemeFontWeight.W400),
                Optional.of(FlutterThemeFontStyle.NORMAL), Optional.of(0.1d),
                Optional.of(0.2d), Optional.of(1.2d), Optional.of("Noto Sans"),
                Optional.of(Set.of(FlutterThemeTextDecorationLine.UNDERLINE)),
                Optional.of(new FlutterThemeColorValue.ColorRole(
                        FlutterMaterialColorRole.PRIMARY)),
                Optional.of(FlutterThemeTextDecorationStyle.SOLID), Optional.of(1.0d));
        EnumMap<FlutterMaterialTextStyleRole, FlutterThemeTextStyleOverride> text =
                new EnumMap<>(FlutterMaterialTextStyleRole.class);
        for (FlutterMaterialTextStyleRole role : FlutterMaterialTextStyleRole.values()) {
            text.put(role, style);
        }
        EnumMap<FlutterThemeComponentColorRole, FlutterThemeColorValue> components =
                new EnumMap<>(FlutterThemeComponentColorRole.class);
        for (FlutterThemeComponentColorRole role
                : FlutterThemeComponentColorRole.values()) {
            components.put(role, new FlutterThemeColorValue.Literal(
                    0xFF000000 | role.ordinal()));
        }
        FlutterThemeOverrides overrides = new FlutterThemeOverrides(
                colors, text, components);
        List<FlutterProjectThemeDefinition> definitions = new ArrayList<>();
        definitions.add(new FlutterProjectThemeDefinition(
                "light", "Light", FlutterThemeBrightness.LIGHT,
                0xFF6750A4, true, overrides));
        definitions.add(new FlutterProjectThemeDefinition(
                "dark", "Dark", FlutterThemeBrightness.DARK,
                0xFF6750A4, true, overrides));
        for (int index = 0; index < FlutterProjectTheme.MAX_THEMES - 2; index++) {
            definitions.add(new FlutterProjectThemeDefinition(
                    "custom_" + index, "Custom " + index,
                    FlutterThemeBrightness.LIGHT, 0xFF6750A4, true, overrides));
        }
        FlutterProjectTheme theme = new FlutterProjectTheme(
                true, FlutterThemeMode.SYSTEM, "light", "dark", definitions,
                new FlutterGeneratedThemeArtifact(
                        FlutterProjectThemePaths.GENERATED_DART_WIRE_PATH,
                        "A".repeat(64)));

        byte[] descriptor = codec.encode(theme);
        byte[] generated = FlutterProjectThemeDartGenerator.generate(theme);

        assertTrue(descriptor.length <= FlutterProjectThemeCodec.MAX_DESCRIPTOR_BYTES);
        assertTrue(generated.length <= FlutterProjectThemeStore.MAX_GENERATED_DART_BYTES);
        assertEquals(theme, codec.decode(descriptor));
    }

    private static FlutterProjectTheme defaultTheme() {
        return FlutterProjectTheme.defaultTheme("A".repeat(64));
    }

    private FlutterProjectTheme decode(String json) throws IOException {
        return codec.decode(json.getBytes(StandardCharsets.UTF_8));
    }

    private static FlutterProjectThemeDefinition definition(
            String id, FlutterThemeBrightness brightness) {
        return new FlutterProjectThemeDefinition(
                id, id, brightness, FlutterProjectTheme.DEFAULT_SEED_ARGB);
    }

    private static int countOccurrences(String value, String needle) {
        int count = 0;
        int from = 0;
        while ((from = value.indexOf(needle, from)) >= 0) {
            count++;
            from += needle.length();
        }
        return count;
    }
}
