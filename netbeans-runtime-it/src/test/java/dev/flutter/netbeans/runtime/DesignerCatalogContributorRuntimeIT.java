package dev.flutter.netbeans.runtime;

import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.regex.Pattern;
import junit.framework.TestFailure;
import junit.framework.TestResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.netbeans.junit.NbModuleSuite;
import org.netbeans.junit.NbTestCase;
import org.openide.modules.ModuleInfo;
import org.openide.util.Lookup;

/** Real module-system gate for the public Flutter Designer catalog SPI. */
final class DesignerCatalogContributorRuntimeIT {
    private static final String FLUTTER_MODULE = "dev.flutter.netbeans.netbeans.plugin";
    private static final String FIXTURE_MODULE =
            "dev.flutter.netbeans.designer.catalog.contributor.fixture";
    private static final String FIXTURE_MODULE_JAR =
            "extra/modules/dev-flutter-netbeans-designer-catalog-contributor-fixture.jar";
    private static final String CONTRIBUTOR_TYPE =
            "dev.flutter.netbeans.designer.catalog.WidgetCatalogContributor";
    private static final String PROVIDER_TYPE =
            "dev.flutter.netbeans.plugin.designer.catalog.NetBeansWidgetCatalogProvider";
    private static final String CONTRIBUTOR_ID = "dev.flutter.netbeans.fixture";
    private static final String WIDGET_TYPE = CONTRIBUTOR_ID + ".FixtureCard";
    private static final String API_RESOURCE =
            "dev/flutter/netbeans/designer/catalog/WidgetCatalogContributor.class";

    @Test
    void externalCatalogContributorUsesTheHostsPublicSpiAtRuntime() throws Exception {
        assertFixturePackagingUsesHostApi();

        String enabledModules = Pattern.quote(FLUTTER_MODULE)
                + "|" + Pattern.quote(FIXTURE_MODULE);
        junit.framework.Test suite = NbModuleSuite
                .createConfiguration(CatalogContributorRuntimeCase.class)
                .clusters("ide|harness|extra")
                .enableModules("extra", enabledModules)
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
                "The catalog contributor runtime test did not run exactly once");
    }

    private static void assertFixturePackagingUsesHostApi() throws Exception {
        Path runtime = Path.of(requiredProperty("all.clusters"))
                .toAbsolutePath()
                .normalize();
        Path fixtureJar = runtime.resolve(FIXTURE_MODULE_JAR);
        Assertions.assertTrue(
                Files.isRegularFile(fixtureJar),
                () -> "Catalog contributor fixture module is missing: " + fixtureJar);

        try (JarFile jar = new JarFile(fixtureJar.toFile())) {
            Attributes attributes = jar.getManifest().getMainAttributes();
            String dependencies = attributes.getValue("OpenIDE-Module-Module-Dependencies");
            Assertions.assertNotNull(
                    dependencies,
                    "Fixture module has no NetBeans module dependency");
            Assertions.assertTrue(
                    dependencies.contains(FLUTTER_MODULE + " > 0.1.3"),
                    () -> "Fixture must have a specification dependency on the host: "
                    + dependencies);
            for (String attribute : List.of("Class-Path", "X-Class-Path")) {
                String value = attributes.getValue(attribute);
                Assertions.assertTrue(
                        value == null || !value.contains("flutter-designer"),
                        () -> "Fixture must not own flutter-designer.jar through "
                        + attribute + ": " + value);
            }
        }

        List<Path> designerLibraries;
        try (var files = Files.walk(runtime)) {
            designerLibraries = files
                    .filter(Files::isRegularFile)
                    .filter(path -> "flutter-designer.jar".equals(
                            path.getFileName().toString()))
                    .toList();
        }
        Assertions.assertEquals(
                1,
                designerLibraries.size(),
                () -> "The runtime must contain one host-owned flutter-designer.jar: "
                + designerLibraries);
        Assertions.assertTrue(
                normalized(designerLibraries.getFirst())
                        .contains("/dev.flutter.netbeans.netbeans-plugin/"),
                () -> "Designer API library is not owned by the Flutter host module: "
                + designerLibraries.getFirst());
    }

    private static String requiredProperty(String name) {
        String value = System.getProperty(name);
        Assertions.assertNotNull(value, "missing system property: " + name);
        Assertions.assertFalse(value.isBlank(), "blank system property: " + name);
        return value;
    }

    private static String normalized(Path path) {
        return path.toAbsolutePath().normalize().toString().replace('\\', '/');
    }

    private static String runtimeFailures(TestResult result) {
        StringBuilder message = new StringBuilder("Catalog contributor runtime gate failed");
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

    public static final class CatalogContributorRuntimeCase extends NbTestCase {
        public CatalogContributorRuntimeCase(String name) {
            super(name);
        }

        public void testExternalContributorDiscoveryCompositionAndClassIdentity()
                throws Exception {
            ModuleInfo flutterModule = module(FLUTTER_MODULE);
            ModuleInfo fixtureModule = module(FIXTURE_MODULE);
            assertTrue("Flutter host module must be enabled", flutterModule.isEnabled());
            assertTrue("Catalog contributor fixture must be enabled", fixtureModule.isEnabled());

            ClassLoader flutterLoader = flutterModule.getClassLoader();
            ClassLoader fixtureLoader = fixtureModule.getClassLoader();
            Class<?> hostApi = Class.forName(CONTRIBUTOR_TYPE, true, flutterLoader);
            Class<?> fixtureApi = Class.forName(CONTRIBUTOR_TYPE, true, fixtureLoader);
            assertSame(
                    "Fixture must resolve the exact API class exported by the host module",
                    hostApi,
                    fixtureApi);

            URL hostApiResource = flutterLoader.getResource(API_RESOURCE);
            URL fixtureApiResource = fixtureLoader.getResource(API_RESOURCE);
            assertNotNull("Host module cannot resolve its catalog SPI class", hostApiResource);
            assertEquals(
                    "Fixture resolved a second catalog SPI resource",
                    hostApiResource,
                    fixtureApiResource);
            assertTrue(
                    "Catalog SPI class must come from the host's flutter-designer.jar: "
                    + hostApiResource,
                    hostApiResource.toExternalForm()
                            .replace('\\', '/')
                            .contains("/dev.flutter.netbeans.netbeans-plugin/"));

            @SuppressWarnings({"rawtypes", "unchecked"})
            Collection<?> contributors = Lookup.getDefault().lookupAll((Class) hostApi);
            Object fixture = contributors.stream()
                    .filter(value -> CONTRIBUTOR_ID.equals(invokeString(value, "contributorId")))
                    .findFirst()
                    .orElse(null);
            assertNotNull(
                    "Default Lookup did not discover the external contributor; found "
                    + contributors,
                    fixture);
            assertSame(
                    "Lookup contributor must implement the host-owned API identity",
                    hostApi,
                    fixture.getClass().getInterfaces()[0]);
            assertSame(
                    "Contributor implementation must be loaded by its fixture module",
                    fixtureLoader,
                    fixture.getClass().getClassLoader());

            Class<?> providerClass = Class.forName(PROVIDER_TYPE, true, flutterLoader);
            Object provider = providerClass.getConstructor().newInstance();
            Object result = providerClass.getMethod("snapshotOffEdt").invoke(provider);
            Object catalog = result.getClass().getMethod("catalog").invoke(result);
            @SuppressWarnings("unchecked")
            List<Object> definitions = (List<Object>) catalog.getClass()
                    .getMethod("definitions")
                    .invoke(catalog);
            assertTrue(
                    "Composed catalog is missing the fixture widget " + WIDGET_TYPE,
                    definitions.stream().anyMatch(definition ->
                            WIDGET_TYPE.equals(widgetTypeId(definition))));
            @SuppressWarnings("unchecked")
            List<Object> diagnostics = (List<Object>) result.getClass()
                    .getMethod("diagnostics")
                    .invoke(result);
            assertTrue("Fixture contributor produced diagnostics: " + diagnostics,
                    diagnostics.isEmpty());
        }

        private static ModuleInfo module(String codeNameBase) {
            Collection<? extends ModuleInfo> modules = Lookup.getDefault()
                    .lookupAll(ModuleInfo.class);
            return modules.stream()
                    .filter(candidate -> codeNameBase.equals(candidate.getCodeNameBase()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "Missing module " + codeNameBase + "; available: "
                            + modules.stream()
                                    .map(ModuleInfo::getCodeNameBase)
                                    .sorted()
                                    .toList()));
        }

        private static String widgetTypeId(Object definition) {
            try {
                Object typeId = definition.getClass().getMethod("typeId").invoke(definition);
                return invokeString(typeId, "value");
            } catch (ReflectiveOperationException failure) {
                throw new AssertionError("Cannot inspect contributed widget type", failure);
            }
        }

        private static String invokeString(Object target, String methodName) {
            try {
                Method method = target.getClass().getMethod(methodName);
                return (String) method.invoke(target);
            } catch (ReflectiveOperationException failure) {
                throw new AssertionError(
                        "Cannot invoke " + methodName + " on " + target,
                        failure);
            }
        }
    }
}
