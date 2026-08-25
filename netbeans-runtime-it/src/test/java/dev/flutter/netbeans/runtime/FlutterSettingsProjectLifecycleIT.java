package dev.flutter.netbeans.runtime;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import junit.framework.TestFailure;
import junit.framework.TestResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.junit.NbModuleSuite;
import org.netbeans.junit.NbTestCase;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.ProjectConfigurationProvider;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.ModuleInfo;
import org.openide.util.Lookup;

/** Mandatory production settings and project-lifecycle gate in an assembled NetBeans runtime. */
final class FlutterSettingsProjectLifecycleIT {

    private static final String FLUTTER_MODULE = "dev.flutter.netbeans.netbeans.plugin";

    @Test
    void settingsAndProjectLifecycleWorkInsideTheAssembledNetBeansRuntime() {
        junit.framework.Test suite = NbModuleSuite.createConfiguration(
                        FlutterSettingsProjectLifecycleRuntimeCase.class)
                .clusters("ide|harness|extra")
                .enableModules("extra", Pattern.quote(FLUTTER_MODULE))
                .enableClasspathModules(false)
                .honorAutoloadEager(true)
                .failOnMessage(Level.SEVERE)
                .failOnException(Level.SEVERE)
                .gui(false)
                .suite();

        TestResult result = new TestResult();
        suite.run(result);

        if (!result.wasSuccessful()) {
            Assertions.fail(runtimeFailures(result));
        }
        Assertions.assertEquals(
                1,
                result.runCount(),
                "The Flutter settings/project lifecycle runtime test did not run exactly once");
    }

    private static String runtimeFailures(TestResult result) {
        StringBuilder message = new StringBuilder(
                "Flutter settings/project lifecycle NetBeans runtime gate failed");
        appendFailures(message, "error", result.errors());
        appendFailures(message, "failure", result.failures());
        return message.toString();
    }

    private static void appendFailures(
            StringBuilder message,
            String kind,
            Enumeration<TestFailure> failures) {
        while (failures.hasMoreElements()) {
            TestFailure failure = failures.nextElement();
            message.append(System.lineSeparator())
                    .append(kind)
                    .append(": ")
                    .append(failure.trace());
        }
    }

    public static final class FlutterSettingsProjectLifecycleRuntimeCase extends NbTestCase {

        private static final Duration TIMEOUT = Duration.ofSeconds(30);
        private static final String SETTINGS_CLASS =
                "dev.flutter.netbeans.plugin.settings.FlutterSettings";
        private static final String CONFIG_CLASS =
                "dev.flutter.netbeans.plugin.settings.FlutterToolchainConfig";
        private static final String TOOLCHAIN_SERVICE_CLASS =
                "dev.flutter.netbeans.plugin.settings.FlutterToolchainService";
        private static final String LIFECYCLE_CLASS =
                "dev.flutter.netbeans.plugin.project.DartAnalysisLifecycle";
        private static final String EXPECTED_PREFERENCES_NODE =
                "/dev/flutter/netbeans/netbeans/plugin";
        private static final String KEY_FLUTTER_HOME = "flutter.sdk.home";
        private static final String KEY_USE_BUNDLED_DART = "dart.sdk.useBundled";
        private static final String KEY_DART_HOME = "dart.sdk.home";
        private static final String KEY_DISCOVERY_VERSION = "sdk.discovery.version";
        private static final String[] SETTINGS_KEYS = {
            KEY_FLUTTER_HOME,
            KEY_USE_BUNDLED_DART,
            KEY_DART_HOME,
            KEY_DISCOVERY_VERSION
        };

        private final Map<String, String> originalSettings = new LinkedHashMap<>();
        private Project project;
        private Preferences preferences;
        private Object settings;
        private Object lifecycle;
        private Method saveSettings;
        private Class<?> configClass;

        public FlutterSettingsProjectLifecycleRuntimeCase(String name) {
            super(name);
        }

        public void testProductionSettingsAndProjectOpenCloseReopen() throws Exception {
            clearWorkDir();
            ClassLoader moduleLoader = flutterModule().getClassLoader();
            initializeSettings(moduleLoader);

            try {
                FakeSdk fakeSdk = createFakeSdk();
                saveAndAssertSettings(moduleLoader, fakeSdk);
                assertToolchainResolution(moduleLoader, fakeSdk);

                project = recognizeFlutterProject(createFlutterProject());
                assertProjectLookup(moduleLoader);
                lifecycle = project.getLookup().lookup(Class.forName(
                        LIFECYCLE_CLASS, true, moduleLoader));
                assertNotNull("Flutter project lookup has no DartAnalysisLifecycle", lifecycle);
                assertFalse("A newly recognized closed project has an open Dart lifecycle",
                        lifecycleIsOpen());

                openProject();
                await("Dart analysis lifecycle to open with the Flutter project",
                        this::lifecycleIsOpen);

                closeProject();
                await("Dart analysis lifecycle to close with the Flutter project",
                        () -> !lifecycleIsOpen());

                openProject();
                await("Dart analysis lifecycle to reopen with the Flutter project",
                        this::lifecycleIsOpen);
            } finally {
                closeProject();
                restoreSettings();
            }
        }

        private void initializeSettings(ClassLoader moduleLoader) throws Exception {
            Class<?> settingsClass = Class.forName(SETTINGS_CLASS, true, moduleLoader);
            configClass = Class.forName(CONFIG_CLASS, true, moduleLoader);
            settings = settingsClass.getMethod("getDefault").invoke(null);
            saveSettings = settingsClass.getMethod("save", configClass);

            Method discoveryVersion = settingsClass.getMethod("discoveryVersion");
            await("Flutter SDK auto-discovery to finish before changing its preferences", () ->
                    invokeInt(settings, discoveryVersion) >= 1);

            Class<?> nbPreferences = Class.forName(
                    "org.openide.util.NbPreferences", true, moduleLoader);
            preferences = (Preferences) nbPreferences
                    .getMethod("forModule", Class.class)
                    .invoke(null, settingsClass);
            preferences.sync();
            for (String key : SETTINGS_KEYS) {
                originalSettings.put(key, preferences.get(key, null));
            }
        }

        private FakeSdk createFakeSdk() throws Exception {
            Path sdkRoot = Files.createDirectories(getWorkDir().toPath().resolve("fake-sdk"));
            Path flutterHome = Files.createDirectories(sdkRoot.resolve("flutter"));
            Path flutterBin = Files.createDirectories(flutterHome.resolve("bin"));
            Path dartHome = Files.createDirectories(sdkRoot.resolve("dart"));
            Path dartBin = Files.createDirectories(dartHome.resolve("bin"));

            boolean windows = System.getProperty("os.name", "")
                    .toLowerCase(java.util.Locale.ROOT)
                    .contains("win");
            Path flutterExecutable;
            Path dartExecutable;
            if (windows) {
                flutterExecutable = flutterBin.resolve("flutter.bat");
                dartExecutable = dartBin.resolve("dart.bat");
                Files.writeString(flutterExecutable, "@echo off\r\necho []\r\nexit /b 0\r\n",
                        StandardCharsets.UTF_8);
                Files.writeString(dartExecutable, "@echo off\r\nexit /b 0\r\n",
                        StandardCharsets.UTF_8);
            } else {
                flutterExecutable = flutterBin.resolve("flutter");
                dartExecutable = dartBin.resolve("dart");
                Files.writeString(flutterExecutable, "#!/bin/sh\nprintf '[]\\n'\n",
                        StandardCharsets.UTF_8);
                Files.writeString(dartExecutable, "#!/bin/sh\nexit 0\n", StandardCharsets.UTF_8);
                assertTrue("Could not make the fake Flutter executable runnable",
                        flutterExecutable.toFile().setExecutable(true));
                assertTrue("Could not make the fake Dart executable runnable",
                        dartExecutable.toFile().setExecutable(true));
            }
            return new FakeSdk(
                    flutterHome.toAbsolutePath().normalize(),
                    flutterExecutable.toAbsolutePath().normalize(),
                    dartHome.toAbsolutePath().normalize(),
                    dartExecutable.toAbsolutePath().normalize());
        }

        private void saveAndAssertSettings(ClassLoader moduleLoader, FakeSdk fakeSdk)
                throws Exception {
            Constructor<?> constructor = configClass.getConstructor(
                    String.class, boolean.class, String.class);
            Object config = constructor.newInstance(
                    fakeSdk.flutterHome().toString(), false, fakeSdk.dartHome().toString());
            saveSettings.invoke(settings, config);
            preferences.sync();

            assertEquals("Flutter settings use the wrong NetBeans module preferences node",
                    EXPECTED_PREFERENCES_NODE, preferences.absolutePath());
            assertEquals("Flutter SDK home was not persisted",
                    fakeSdk.flutterHome().toString(), preferences.get(KEY_FLUTTER_HOME, null));
            assertEquals("Standalone Dart mode was not persisted",
                    "false", preferences.get(KEY_USE_BUNDLED_DART, null));
            assertEquals("Dart SDK home was not persisted",
                    fakeSdk.dartHome().toString(), preferences.get(KEY_DART_HOME, null));

            Object loaded = settings.getClass().getMethod("load").invoke(settings);
            assertEquals("FlutterSettings.load() did not return the saved Flutter SDK",
                    fakeSdk.flutterHome().toString(), invoke(loaded, "flutterHome"));
            assertEquals("FlutterSettings.load() changed the standalone Dart mode",
                    false, invoke(loaded, "useBundledDart"));
            assertEquals("FlutterSettings.load() did not return the saved Dart SDK",
                    fakeSdk.dartHome().toString(), invoke(loaded, "dartHome"));

            FileObject persisted = FileUtil.getConfigFile(
                    "Preferences/dev/flutter/netbeans/netbeans/plugin.properties");
            assertNotNull("Flushed Flutter settings are absent from the NetBeans config filesystem",
                    persisted);
        }

        private void assertToolchainResolution(ClassLoader moduleLoader, FakeSdk fakeSdk)
                throws Exception {
            Class<?> serviceClass = Class.forName(TOOLCHAIN_SERVICE_CLASS, true, moduleLoader);
            Object service = serviceClass.getConstructor().newInstance();
            Object status = serviceClass.getMethod("resolve").invoke(service);
            assertTrue("The production toolchain service rejected the persisted fake SDKs",
                    (boolean) invoke(status, "isReady"));

            Object flutterSdk = ((Optional<?>) invoke(status, "flutterSdk")).orElseThrow();
            assertEquals("Resolved Flutter home differs from the persisted setting",
                    fakeSdk.flutterHome(), invoke(flutterSdk, "home"));
            assertEquals("Resolved Flutter executable differs from the fake SDK",
                    fakeSdk.flutterExecutable(), invoke(flutterSdk, "flutterExecutable"));

            Object dartSdk = ((Optional<?>) invoke(status, "dartSdk")).orElseThrow();
            assertEquals("Resolved Dart home differs from the persisted setting",
                    fakeSdk.dartHome(), invoke(dartSdk, "home"));
            assertEquals("Resolved Dart executable differs from the fake SDK",
                    fakeSdk.dartExecutable(), invoke(dartSdk, "dartExecutable"));
        }

        private Path createFlutterProject() throws Exception {
            Path root = getWorkDir().toPath().resolve("flutter-settings-lifecycle");
            Path lib = Files.createDirectories(root.resolve("lib"));
            Files.writeString(root.resolve("pubspec.yaml"), """
                    name: flutter_settings_lifecycle
                    environment:
                      sdk: '>=3.0.0 <4.0.0'
                    flutter:
                      uses-material-design: true
                    """, StandardCharsets.UTF_8);
            Files.writeString(lib.resolve("main.dart"), "void main() {}\n",
                    StandardCharsets.UTF_8);
            return root.toAbsolutePath().normalize();
        }

        private Project recognizeFlutterProject(Path projectPath) throws Exception {
            FileUtil.refreshFor(projectPath.toFile());
            FileObject directory = FileUtil.toFileObject(projectPath.toFile());
            assertNotNull("The Flutter project directory is absent from the NetBeans filesystem",
                    directory);
            Project recognized = ProjectManager.getDefault().findProject(directory);
            assertNotNull("The assembled runtime did not recognize pubspec.yaml as a Flutter project",
                    recognized);
            assertEquals("The assembled runtime loaded the wrong project implementation",
                    "dev.flutter.netbeans.plugin.project.FlutterProject",
                    recognized.getClass().getName());
            return recognized;
        }

        private void assertProjectLookup(ClassLoader moduleLoader) throws Exception {
            assertNotNull("Flutter project lookup has no ActionProvider",
                    project.getLookup().lookup(ActionProvider.class));
            assertNotNull("Flutter project lookup has no LogicalViewProvider",
                    project.getLookup().lookup(LogicalViewProvider.class));
            assertNotNull("Flutter project lookup has no ProjectConfigurationProvider",
                    project.getLookup().lookup(ProjectConfigurationProvider.class));

            Class<?> flutterProjectClass = Class.forName(
                    "dev.flutter.netbeans.plugin.project.FlutterProject", true, moduleLoader);
            assertTrue("Recognized project was not loaded by the active Flutter module",
                    flutterProjectClass.isInstance(project));
        }

        private void openProject() throws Exception {
            OpenProjects.getDefault().open(new Project[] {project}, false, true);
            await("the Flutter project to open",
                    () -> OpenProjects.getDefault().isProjectOpen(project));
            OpenProjects.getDefault().openProjects().get(
                    TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        }

        private void closeProject() throws Exception {
            if (project == null || !OpenProjects.getDefault().isProjectOpen(project)) {
                return;
            }
            OpenProjects.getDefault().close(new Project[] {project});
            await("the Flutter project to close",
                    () -> !OpenProjects.getDefault().isProjectOpen(project));
        }

        private boolean lifecycleIsOpen() {
            if (lifecycle == null) {
                return false;
            }
            try {
                return (boolean) lifecycle.getClass().getMethod("isOpen").invoke(lifecycle);
            } catch (ReflectiveOperationException ex) {
                throw new AssertionError("Could not read DartAnalysisLifecycle.isOpen()", ex);
            }
        }

        private void restoreSettings() throws Exception {
            if (preferences == null || originalSettings.isEmpty()) {
                return;
            }
            for (String key : SETTINGS_KEYS) {
                String previous = originalSettings.get(key);
                if (previous == null) {
                    preferences.remove(key);
                } else {
                    preferences.put(key, previous);
                }
            }
            preferences.flush();
            preferences.sync();
        }

        private ModuleInfo flutterModule() {
            Collection<? extends ModuleInfo> modules = Lookup.getDefault()
                    .lookupAll(ModuleInfo.class);
            ModuleInfo module = modules.stream()
                    .filter(candidate -> FLUTTER_MODULE.equals(candidate.getCodeNameBase()))
                    .findFirst()
                    .orElse(null);
            assertNotNull("Flutter module is missing from the assembled runtime", module);
            assertTrue("Flutter module is not active in the assembled runtime", module.isEnabled());
            return module;
        }

        private static Object invoke(Object target, String method) throws Exception {
            return target.getClass().getMethod(method).invoke(target);
        }

        private static int invokeInt(Object target, Method method) {
            try {
                return (int) method.invoke(target);
            } catch (ReflectiveOperationException ex) {
                throw new AssertionError("Could not invoke " + method.getName(), ex);
            }
        }

        private static void await(String description, BooleanSupplier condition) throws Exception {
            long deadline = System.nanoTime() + TIMEOUT.toNanos();
            while (System.nanoTime() < deadline) {
                if (condition.getAsBoolean()) {
                    return;
                }
                Thread.sleep(50);
            }
            fail("Timed out waiting for " + description);
        }

        private record FakeSdk(
                Path flutterHome,
                Path flutterExecutable,
                Path dartHome,
                Path dartExecutable) {
        }
    }
}
