package dev.flutter.netbeans.designer.codec;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FdSchemaResourceTest {
    private static final Path DOCUMENTATION_SCHEMA =
            Path.of("docs", "flutter-designer", "fd-v1.schema.json");

    @Test
    void packagesTheCanonicalSchemaAsAnLfNormalizedRuntimeResource() throws IOException {
        byte[] packaged = loadPackagedSchema();
        byte[] documented = Files.readAllBytes(findRepositoryFile(DOCUMENTATION_SCHEMA));

        assertArrayEquals(documented, packaged,
                "The runtime schema and browsable documentation copy must remain byte-identical");
        assertFalse(new String(packaged, StandardCharsets.UTF_8).contains("\r"),
                "The canonical schema must use LF line endings");

        String schema = new String(packaged, StandardCharsets.UTF_8);
        assertTrue(schema.contains("\"$schema\": \"https://json-schema.org/draft/2020-12/schema\""));
        assertTrue(schema.contains("\"$id\": \"urn:netbeans-flutter-designer:schema:fd:1\""));
        assertTrue(schema.contains("\"const\": \"netbeans-flutter-designer\""));
        assertTrue(schema.contains("\"schemaVersion\""));
        assertTrue(schema.contains("\"const\": 1"));
    }

    private static byte[] loadPackagedSchema() throws IOException {
        ClassLoader loader = FdSchemaResourceTest.class.getClassLoader();
        assertNotNull(loader.getResource(FdSchemas.V1_RESOURCE),
                "The canonical schema must be present on the runtime classpath");
        try (InputStream input = FdSchemas.openV1()) {
            assertNotNull(input, "The canonical schema resource must be readable");
            return input.readAllBytes();
        }
    }

    private static Path findRepositoryFile(Path relativePath) {
        Path directory = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (directory != null) {
            Path candidate = directory.resolve(relativePath);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            directory = directory.getParent();
        }
        throw new IllegalStateException("Cannot locate repository file " + relativePath);
    }
}
