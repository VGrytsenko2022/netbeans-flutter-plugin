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
    private static final Path V1_DOCUMENTATION_SCHEMA =
            Path.of("docs", "flutter-designer", "fd-v1.schema.json");
    private static final Path V2_DOCUMENTATION_SCHEMA =
            Path.of("docs", "flutter-designer", "fd-v2.schema.json");
    private static final Path V3_DOCUMENTATION_SCHEMA =
            Path.of("docs", "flutter-designer", "fd-v3.schema.json");
    private static final Path V4_DOCUMENTATION_SCHEMA =
            Path.of("docs", "flutter-designer", "fd-v4.schema.json");
    private static final Path V5_DOCUMENTATION_SCHEMA =
            Path.of("docs", "flutter-designer", "fd-v5.schema.json");
    private static final Path V6_DOCUMENTATION_SCHEMA =
            Path.of("docs", "flutter-designer", "fd-v6.schema.json");

    @Test
    void packagesTheCanonicalSchemaAsAnLfNormalizedRuntimeResource() throws IOException {
        byte[] packaged = loadPackagedV1Schema();
        byte[] documented = Files.readAllBytes(findRepositoryFile(V1_DOCUMENTATION_SCHEMA));

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

    @Test
    void packagesTheCanonicalV2SchemaFromTheBrowsableDocumentationCopy() throws IOException {
        byte[] packaged = loadPackagedV2Schema();
        byte[] documented = Files.readAllBytes(findRepositoryFile(V2_DOCUMENTATION_SCHEMA));

        assertArrayEquals(documented, packaged,
                "The bundled and browsable schema v2 copies must remain byte-identical");
        String schema = new String(packaged, StandardCharsets.UTF_8);
        assertFalse(schema.contains("\r"));
        assertTrue(schema.contains("\"$id\": \"urn:netbeans-flutter-designer:schema:fd:2\""));
        assertTrue(schema.contains("\"const\": 2"));
        assertTrue(schema.contains("\"themeTokenValue\""));
        assertTrue(schema.contains("\"paintValue\""));
        assertTrue(schema.contains("\"shadowListValue\""));
        assertTrue(schema.contains("\"fontFeatureListValue\""));
        assertTrue(schema.contains("\"fontVariationListValue\""));
    }

    @Test
    void packagesTheCanonicalV3SchemaWithDirectionalEdgeInsets() throws IOException {
        byte[] packaged = loadPackagedV3Schema();
        byte[] documented = Files.readAllBytes(findRepositoryFile(V3_DOCUMENTATION_SCHEMA));

        assertArrayEquals(documented, packaged,
                "The bundled and browsable schema v3 copies must remain byte-identical");
        String schema = new String(packaged, StandardCharsets.UTF_8);
        assertFalse(schema.contains("\r"));
        assertTrue(schema.contains("\"$id\": \"urn:netbeans-flutter-designer:schema:fd:3\""));
        assertTrue(schema.contains("\"const\": 3"));
        assertTrue(schema.contains("\"edgeInsetsDirectionalValue\""));
        assertTrue(schema.contains("\"start\""));
        assertTrue(schema.contains("\"end\""));
    }

    @Test
    void packagesTheCanonicalV4SchemaWithTypedNullableIconData() throws IOException {
        byte[] packaged = loadPackagedV4Schema();
        byte[] documented = Files.readAllBytes(findRepositoryFile(V4_DOCUMENTATION_SCHEMA));

        assertArrayEquals(documented, packaged,
                "The bundled and browsable schema v4 copies must remain byte-identical");
        String schema = new String(packaged, StandardCharsets.UTF_8);
        assertFalse(schema.contains("\r"));
        assertTrue(schema.contains("\"$id\": \"urn:netbeans-flutter-designer:schema:fd:4\""));
        assertTrue(schema.contains("\"const\": 4"));
        assertTrue(schema.contains("\"iconDataValue\""));
        assertTrue(schema.contains("\"const\": \"iconData\""));
        assertTrue(schema.contains("\"maximum\": 1114111"));
        assertTrue(schema.contains("\"maxItems\": 32"));
        assertTrue(schema.contains("\"uniqueItems\": true"));
        assertTrue(schema.contains("\\\\u2028-\\\\u202E"));
        assertTrue(schema.contains("\\\\u001C-\\\\u0020\\\\u1680"));
        assertTrue(schema.contains("\\\\u2000-\\\\u2006\\\\u2008-\\\\u200A"));
        assertFalse(schema.contains("(?!\\\\s)"));
    }

    @Test
    void packagesTheCanonicalV5SchemaWithStructuredContainerValues()
            throws IOException {
        byte[] packaged = loadPackagedV5Schema();
        byte[] documented = Files.readAllBytes(findRepositoryFile(
                V5_DOCUMENTATION_SCHEMA));

        assertArrayEquals(documented, packaged,
                "The bundled and browsable schema v5 copies must remain byte-identical");
        String schema = new String(packaged, StandardCharsets.UTF_8);
        assertFalse(schema.contains("\r"));
        assertTrue(schema.contains(
                "\"$id\": \"urn:netbeans-flutter-designer:schema:fd:5\""));
        assertTrue(schema.contains("\"const\": 5"));
        assertTrue(schema.contains("\"alignmentGeometryValue\""));
        assertTrue(schema.contains("\"boxConstraintsValue\""));
        assertTrue(schema.contains("\"matrix4Value\""));
        assertTrue(schema.contains("\"boxDecorationValue\""));
        assertTrue(schema.contains("\"minItems\": 16"));
        assertTrue(schema.contains("\"maxItems\": 16"));
        assertTrue(schema.contains("\"linearGradient\""));
        assertTrue(schema.contains("\"radialGradient\""));
        assertTrue(schema.contains("\"sweepGradient\""));
        assertTrue(schema.contains("\"backgroundBlendMode\""));
        assertTrue(schema.contains("\"focalRadius\": {\"const\": 0}"));
    }

    @Test
    void packagesTheCanonicalV6SchemaWithTypedAssetImages()
            throws IOException {
        byte[] packaged = loadPackagedV6Schema();
        byte[] documented = Files.readAllBytes(findRepositoryFile(
                V6_DOCUMENTATION_SCHEMA));

        assertArrayEquals(documented, packaged,
                "The bundled and browsable schema v6 copies must remain byte-identical");
        assertArrayEquals(packaged, loadCurrentSchema(),
                "The current schema pointer must resolve to v6");
        String schema = new String(packaged, StandardCharsets.UTF_8);
        assertFalse(schema.contains("\r"));
        assertTrue(schema.contains(
                "\"$id\": \"urn:netbeans-flutter-designer:schema:fd:6\""));
        assertTrue(schema.contains("\"const\": 6"));
        assertTrue(schema.contains("\"imageProviderValue\""));
        assertTrue(schema.contains("\"const\": \"imageProvider\""));
        assertTrue(schema.contains("\"maximum\": 16384"));
        assertTrue(schema.contains("\"decorationImage\""));
        assertTrue(schema.contains("\"decorationImageColorFilterMatrix\""));
        assertTrue(schema.contains("\"minItems\": 20"));
        assertTrue(schema.contains("\"linearToSrgbGamma\""));
        assertTrue(schema.contains("\"srgbToLinearGamma\""));
        assertTrue(schema.contains("\"saturation\""));
        assertTrue(schema.contains("\"centerSlice\""));
        assertTrue(schema.contains("{\"const\": \"contain\"}"));
        assertTrue(schema.contains("{\"const\": \"scaleDown\"}"));
        assertTrue(schema.contains(
                "\"pattern\": \"^(?!(?:[\\\\u0009-\\\\u000D"
                + "\\\\u001C-\\\\u0020"),
                "Image asset names must reject Java-blank strings");
        assertTrue(schema.contains(
                "])+$)(?![\\\\u0000-\\\\u0020])"),
                "Image asset names must reject Java-trimmed leading whitespace");
        assertTrue(schema.contains(
                "(?!.*[\\\\u0000-\\\\u0020]$)(?!/)(?!~)(?!.*%)"),
                "Image asset names must reject trailing whitespace, home-relative, "
                + "and percent syntax");
    }

    private static byte[] loadPackagedV1Schema() throws IOException {
        ClassLoader loader = FdSchemaResourceTest.class.getClassLoader();
        assertNotNull(loader.getResource(FdSchemas.V1_RESOURCE),
                "The canonical schema must be present on the runtime classpath");
        try (InputStream input = FdSchemas.openV1()) {
            assertNotNull(input, "The canonical schema resource must be readable");
            return input.readAllBytes();
        }
    }

    private static byte[] loadPackagedV2Schema() throws IOException {
        ClassLoader loader = FdSchemaResourceTest.class.getClassLoader();
        assertNotNull(loader.getResource(FdSchemas.V2_RESOURCE),
                "The canonical schema v2 must be present on the runtime classpath");
        try (InputStream input = FdSchemas.openV2()) {
            assertNotNull(input, "The canonical schema v2 resource must be readable");
            return input.readAllBytes();
        }
    }

    private static byte[] loadPackagedV3Schema() throws IOException {
        ClassLoader loader = FdSchemaResourceTest.class.getClassLoader();
        assertNotNull(loader.getResource(FdSchemas.V3_RESOURCE),
                "The canonical schema v3 must be present on the runtime classpath");
        try (InputStream input = FdSchemas.openV3()) {
            assertNotNull(input, "The canonical schema v3 resource must be readable");
            return input.readAllBytes();
        }
    }

    private static byte[] loadPackagedV4Schema() throws IOException {
        ClassLoader loader = FdSchemaResourceTest.class.getClassLoader();
        assertNotNull(loader.getResource(FdSchemas.V4_RESOURCE),
                "The canonical schema v4 must be present on the runtime classpath");
        try (InputStream input = FdSchemas.openV4()) {
            assertNotNull(input, "The canonical schema v4 resource must be readable");
            return input.readAllBytes();
        }
    }

    private static byte[] loadPackagedV5Schema() throws IOException {
        ClassLoader loader = FdSchemaResourceTest.class.getClassLoader();
        assertNotNull(loader.getResource(FdSchemas.V5_RESOURCE),
                "The canonical schema v5 must be present on the runtime classpath");
        try (InputStream input = FdSchemas.openV5()) {
            assertNotNull(input, "The canonical schema v5 resource must be readable");
            return input.readAllBytes();
        }
    }

    private static byte[] loadPackagedV6Schema() throws IOException {
        ClassLoader loader = FdSchemaResourceTest.class.getClassLoader();
        assertNotNull(loader.getResource(FdSchemas.V6_RESOURCE),
                "The canonical schema v6 must be present on the runtime classpath");
        try (InputStream input = FdSchemas.openV6()) {
            assertNotNull(input, "The canonical schema v6 resource must be readable");
            return input.readAllBytes();
        }
    }

    private static byte[] loadCurrentSchema() throws IOException {
        try (InputStream input = FdSchemas.openCurrent()) {
            assertNotNull(input, "The current schema resource must be readable");
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
