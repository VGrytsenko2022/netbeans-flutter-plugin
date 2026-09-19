package io.github.vgrytsenko2022.plugin;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;
import org.netbeans.modules.lsp.client.spi.LanguageServerProvider;

/** Locks the Dart editor integration to the standard adapters bundled by NetBeans 30. */
class NetBeansLspFeatureContractTest {
    private static final String BINDINGS = "org/netbeans/modules/lsp/client/bindings/";

    @Test
    void netBeans30SuppliesAdaptersForTheAdvertisedDartFeatures()
            throws IOException, URISyntaxException {
        Path module = Path.of(LanguageServerProvider.class
                        .getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .toURI())
                .toAbsolutePath()
                .normalize();
        assertTrue(Files.isRegularFile(module), "LSP client dependency is not a module JAR: " + module);
        assertTrue(
                module.getFileName().toString().contains("RELEASE300"),
                "feature contract must be reviewed when the NetBeans LSP client changes: " + module);

        try (JarFile jar = new JarFile(module.toFile())) {
            String layer = readEntry(jar, "META-INF/generated-layer.xml");
            assertTrue(layer.contains("bindings-CodeActions.instance"));
            assertTrue(layer.contains("bindings-CompletionProviderImpl.instance"));
            assertTrue(layer.contains("bindings-HyperlinkProviderImpl.instance"));

            String mimeDataProviders = readEntry(
                    jar,
                    "META-INF/services/org.netbeans.spi.editor.mimelookup.MimeDataProvider");
            assertTrue(mimeDataProviders.contains("bindings.LspMimeDataProvider"));

            String refactoringProviders = readEntry(
                    jar,
                    "META-INF/services/"
                            + "org.netbeans.modules.refactoring.spi.ui.ActionsImplementationProvider");
            assertTrue(refactoringProviders.contains("bindings.refactoring.RefactoringActionsProvider"));

            assertEntry(jar, BINDINGS + "LanguageClientImpl.class");
            assertEntry(jar, BINDINGS + "CompletionProviderImpl.class");
            assertEntry(jar, BINDINGS + "HyperlinkProviderImpl.class");
            assertEntry(jar, BINDINGS + "Formatter$Factory.class");
            assertEntry(jar, BINDINGS + "refactoring/RefactoringActionsProvider.class");
        }
    }

    private static String readEntry(JarFile jar, String name) throws IOException {
        JarEntry entry = jar.getJarEntry(name);
        assertNotNull(entry, "missing NetBeans 30 LSP contract entry: " + name);
        try (InputStream input = jar.getInputStream(entry)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void assertEntry(JarFile jar, String name) {
        assertNotNull(jar.getJarEntry(name), "missing NetBeans 30 LSP adapter: " + name);
    }
}
