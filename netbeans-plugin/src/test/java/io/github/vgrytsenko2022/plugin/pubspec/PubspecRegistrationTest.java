package io.github.vgrytsenko2022.plugin.pubspec;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class PubspecRegistrationTest {
    @Test
    void registersCompletionAndDiagnosticsForTheBundledYamlMimeType() throws IOException {
        String layer = readGeneratedLayer();

        assertTrue(layer.contains("<folder name=\"x-yaml\">"));
        assertTrue(layer.contains("PubspecCompletionProvider.instance"));
        assertTrue(layer.contains(
                "instanceOf\" stringvalue=\"org.netbeans.spi.editor.completion.CompletionProvider"));
        assertTrue(layer.contains("PubspecErrorProvider.instance"));
        assertTrue(layer.contains(
                "instanceOf\" stringvalue=\"org.netbeans.spi.lsp.ErrorProvider"));
    }

    private static String readGeneratedLayer() throws IOException {
        try (InputStream input = PubspecRegistrationTest.class.getClassLoader()
                .getResourceAsStream("META-INF/generated-layer.xml")) {
            assertNotNull(input, "missing generated NetBeans registration layer");
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
