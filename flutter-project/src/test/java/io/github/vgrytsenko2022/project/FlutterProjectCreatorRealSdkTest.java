package io.github.vgrytsenko2022.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vgrytsenko2022.sdk.FlutterCli;
import io.github.vgrytsenko2022.sdk.FlutterSdkLocator;
import io.github.vgrytsenko2022.project.theme.FlutterGeneratedThemeArtifact;
import io.github.vgrytsenko2022.project.theme.FlutterMaterialColorRole;
import io.github.vgrytsenko2022.project.theme.FlutterProjectTheme;
import io.github.vgrytsenko2022.project.theme.FlutterProjectThemeCodec;
import io.github.vgrytsenko2022.project.theme.FlutterProjectThemeDartGenerator;
import io.github.vgrytsenko2022.project.theme.FlutterProjectThemeDefinition;
import io.github.vgrytsenko2022.project.theme.FlutterProjectThemeDigests;
import io.github.vgrytsenko2022.project.theme.FlutterProjectThemeLoadStatus;
import io.github.vgrytsenko2022.project.theme.FlutterProjectThemePaths;
import io.github.vgrytsenko2022.project.theme.FlutterProjectThemeStore;
import io.github.vgrytsenko2022.project.theme.FlutterThemeColorValue;
import io.github.vgrytsenko2022.project.theme.FlutterThemeComponentColorRole;
import io.github.vgrytsenko2022.project.theme.FlutterThemeOverrides;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

/** Optional smoke test. Run with -Dflutter.it.sdk=/path/to/flutter. */
class FlutterProjectCreatorRealSdkTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    @EnabledIfSystemProperty(named = "flutter.it.sdk", matches = ".+")
    void createsBaseApplicationWithRealFlutterSdk() throws Exception {
        Path sdkHome = Path.of(System.getProperty("flutter.it.sdk"));
        var sdk = new FlutterSdkLocator().fromHome(sdkHome).orElseThrow();
        var request = new FlutterProjectCreationRequest(
                temporaryDirectory,
                "netbeans_flutter_smoke",
                "io.github.vgrytsenko2022",
                "NetBeans Flutter plugin smoke test.",
                Set.of(
                        FlutterProjectPlatform.ANDROID,
                        FlutterProjectPlatform.WEB));

        var cli = new FlutterCli(sdk);
        var created = new FlutterProjectCreator(cli).create(request);

        assertEquals("netbeans_flutter_smoke", created.name());
        assertTrue(Files.isRegularFile(created.root().resolve("lib/main.dart")));
        assertTrue(Files.isRegularFile(created.root().resolve("pubspec.yaml")));
        assertTrue(Files.isDirectory(created.root().resolve("android")));
        assertTrue(Files.isDirectory(created.root().resolve("web")));
        assertTrue(Files.notExists(created.root().resolve("windows")));
        assertTrue(Files.isRegularFile(created.root().resolve(
                FlutterProjectThemePaths.DESCRIPTOR_PATH)));
        assertTrue(Files.isRegularFile(created.root().resolve(
                FlutterProjectThemePaths.GENERATED_DART_PATH)));
        var loaded = new FlutterProjectThemeStore().load(created.root());
        assertEquals(FlutterProjectThemeLoadStatus.VALID, loaded.status());
        String main = Files.readString(created.root().resolve("lib/main.dart"));
        assertTrue(main.contains("import 'theme/app_theme.dart';"));
        assertTrue(main.contains("theme: AppTheme.light"));
        assertTrue(main.contains("darkTheme: AppTheme.dark"));
        assertTrue(main.contains("themeMode: AppTheme.mode"));

        FlutterProjectTheme componentTheme = withAllComponentColors(
                loaded.theme().orElseThrow());
        byte[] componentDart = FlutterProjectThemeDartGenerator.generate(componentTheme);
        Files.write(
                created.root().resolve(FlutterProjectThemePaths.GENERATED_DART_PATH),
                componentDart);
        Files.write(
                created.root().resolve(FlutterProjectThemePaths.DESCRIPTOR_PATH),
                new FlutterProjectThemeCodec().encode(componentTheme));
        assertEquals(FlutterProjectThemeLoadStatus.VALID,
                new FlutterProjectThemeStore().load(created.root()).status());

        var analyze = cli.execute(created.root(), Duration.ofMinutes(5), "analyze");
        assertTrue(analyze.success(), () -> "flutter analyze failed:\n"
                + analyze.stdout() + "\n" + analyze.stderr());
        var test = cli.execute(created.root(), Duration.ofMinutes(5), "test");
        assertTrue(test.success(), () -> "flutter test failed:\n"
                + test.stdout() + "\n" + test.stderr());
    }

    private static FlutterProjectTheme withAllComponentColors(
            FlutterProjectTheme original) {
        EnumMap<FlutterThemeComponentColorRole, FlutterThemeColorValue> components =
                new EnumMap<>(FlutterThemeComponentColorRole.class);
        FlutterMaterialColorRole[] semanticRoles = FlutterMaterialColorRole.values();
        for (FlutterThemeComponentColorRole role
                : FlutterThemeComponentColorRole.values()) {
            FlutterThemeColorValue value = role.ordinal() % 2 == 0
                    ? new FlutterThemeColorValue.Literal(
                            0xFF000000 | (role.ordinal() * 0x0003070B))
                    : new FlutterThemeColorValue.ColorRole(
                            semanticRoles[role.ordinal() % semanticRoles.length]);
            components.put(role, value);
        }
        FlutterProjectThemeDefinition light = original.lightTheme();
        FlutterProjectThemeDefinition componentLight = new FlutterProjectThemeDefinition(
                light.id(), light.displayName(), light.brightness(), light.seedArgb(),
                light.enabled(), new FlutterThemeOverrides(
                        light.overrides().colorScheme(),
                        light.overrides().textTheme(),
                        components));
        FlutterProjectTheme provisional = new FlutterProjectTheme(
                original.enabled(), original.defaultMode(), original.lightThemeId(),
                original.darkThemeId(), java.util.List.of(
                        componentLight, original.darkTheme()), original.generated());
        byte[] dart = FlutterProjectThemeDartGenerator.generate(provisional);
        return new FlutterProjectTheme(
                provisional.enabled(), provisional.defaultMode(),
                provisional.lightThemeId(), provisional.darkThemeId(),
                provisional.themes(), new FlutterGeneratedThemeArtifact(
                        FlutterProjectThemePaths.GENERATED_DART_WIRE_PATH,
                        FlutterProjectThemeDigests.sha256(dart)));
    }
}
