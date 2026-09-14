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
    private static final Path V7_DOCUMENTATION_SCHEMA =
            Path.of("docs", "flutter-designer", "fd-v7.schema.json");
    private static final Path V8_DOCUMENTATION_SCHEMA =
            Path.of("docs", "flutter-designer", "fd-v8.schema.json");
    private static final Path V9_DOCUMENTATION_SCHEMA =
            Path.of("docs", "flutter-designer", "fd-v9.schema.json");
    private static final Path V10_DOCUMENTATION_SCHEMA =
            Path.of("docs", "flutter-designer", "fd-v10.schema.json");
    private static final Path V11_DOCUMENTATION_SCHEMA =
            Path.of("docs", "flutter-designer", "fd-v11.schema.json");
    private static final Path V12_DOCUMENTATION_SCHEMA =
            Path.of("docs", "flutter-designer", "fd-v12.schema.json");

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

    @Test
    void packagesTheCanonicalV7SchemaWithPositiveInfinityOnEveryBoxConstraintBound()
            throws IOException {
        byte[] packaged = loadPackagedV7Schema();
        byte[] documented = Files.readAllBytes(findRepositoryFile(
                V7_DOCUMENTATION_SCHEMA));

        assertArrayEquals(documented, packaged,
                "The bundled and browsable schema v7 copies must remain byte-identical");
        String schema = new String(packaged, StandardCharsets.UTF_8);
        assertFalse(schema.contains("\r"));
        assertTrue(schema.contains(
                "\"$id\": \"urn:netbeans-flutter-designer:schema:fd:7\""));
        assertTrue(schema.contains("\"const\": 7"));
        assertTrue(schema.contains("\"required\": [\"kind\", \"minWidth\", \"maxWidth\", \"minHeight\", \"maxHeight\"]"));
        int constraints = schema.indexOf("\"boxConstraintsValue\"");
        for (String bound : new String[]{
                "minWidth", "maxWidth", "minHeight", "maxHeight"}) {
            int start = schema.indexOf("\"" + bound + "\": {", constraints);
            int end = schema.indexOf("\n        }", start);
            assertTrue(start >= 0 && end > start, bound);
            assertTrue(schema.substring(start, end).contains("{\"type\": \"null\"}"),
                    bound + " must encode positive infinity as null");
        }
    }

    @Test
    void packagesCanonicalV8WithTypedFiniteNonNegativeSize() throws IOException {
        byte[] packaged = loadPackagedV8Schema();
        byte[] documented = Files.readAllBytes(findRepositoryFile(
                V8_DOCUMENTATION_SCHEMA));
        assertArrayEquals(documented, packaged,
                "The bundled and browsable schema v8 copies must remain byte-identical");
        String schema = new String(packaged, StandardCharsets.UTF_8);

        assertFalse(schema.contains("\r"));
        assertTrue(schema.contains(
                "\"$id\": \"urn:netbeans-flutter-designer:schema:fd:8\""));
        assertTrue(schema.contains("\"const\": 8"));
        assertTrue(schema.contains("\"$ref\": \"#/$defs/sizeValue\""));
        assertTrue(schema.contains("\"const\": \"size\""));
        assertTrue(schema.contains("\"minimum\": 0"));
        assertTrue(schema.contains(
                "\"required\": [\n        \"kind\",\n        \"width\",\n        \"height\""));
    }

    @Test
    void packagesCanonicalV9WithTypedFiniteSignedOffset() throws IOException {
        byte[] packaged = loadPackagedV9Schema();
        byte[] documented = Files.readAllBytes(findRepositoryFile(
                V9_DOCUMENTATION_SCHEMA));
        assertArrayEquals(documented, packaged,
                "The bundled and browsable schema v9 copies must remain byte-identical");
        String schema = new String(packaged, StandardCharsets.UTF_8);

        assertFalse(schema.contains("\r"));
        assertTrue(schema.contains(
                "\"$id\": \"urn:netbeans-flutter-designer:schema:fd:9\""));
        assertTrue(schema.contains("\"const\": 9"));
        assertTrue(schema.contains("\"$ref\": \"#/$defs/offsetValue\""));
        assertTrue(schema.contains("\"const\": \"offset\""));
        assertTrue(schema.contains(
                "\"required\": [\n        \"kind\",\n        \"dx\",\n        \"dy\""));
        int offset = schema.indexOf("\"offsetValue\"");
        int size = schema.indexOf("\"sizeValue\"", offset);
        assertTrue(offset >= 0 && size > offset);
        assertFalse(schema.substring(offset, size).contains("\"minimum\""),
                "Offset coordinates are signed");
    }

    @Test
    void packagesCanonicalV10WithExplicitTypedNull() throws IOException {
        byte[] packaged = loadPackagedV10Schema();
        byte[] documented = Files.readAllBytes(findRepositoryFile(
                V10_DOCUMENTATION_SCHEMA));
        assertArrayEquals(documented, packaged,
                "The bundled and browsable schema v10 copies must remain byte-identical");
        String schema = new String(packaged, StandardCharsets.UTF_8);

        assertFalse(schema.contains("\r"));
        assertTrue(schema.contains(
                "\"$id\": \"urn:netbeans-flutter-designer:schema:fd:10\""));
        assertTrue(schema.contains("\"const\": 10"));
        assertTrue(schema.contains("\"$ref\": \"#/$defs/nullValue\""));
        assertTrue(schema.contains("\"const\": \"null\""));
        int nullValue = schema.indexOf("\"nullValue\"");
        int stringValue = schema.indexOf("\"stringValue\"", nullValue);
        assertTrue(nullValue >= 0 && stringValue > nullValue);
        assertTrue(schema.substring(nullValue, stringValue).contains(
                "\"additionalProperties\": false"));
    }

    @Test
    void packagesCanonicalV11WithStandaloneTypedBorderRadius() throws IOException {
        byte[] packaged = loadPackagedV11Schema();
        byte[] documented = Files.readAllBytes(findRepositoryFile(
                V11_DOCUMENTATION_SCHEMA));
        assertArrayEquals(documented, packaged,
                "The bundled and browsable schema v11 copies must remain byte-identical");
        String schema = new String(packaged, StandardCharsets.UTF_8);

        assertFalse(schema.contains("\r"));
        assertTrue(schema.contains(
                "\"$id\": \"urn:netbeans-flutter-designer:schema:fd:11\""));
        assertTrue(schema.contains("\"const\": 11"));
        assertTrue(schema.contains("\"$ref\": \"#/$defs/borderRadiusValue\""));
        int borderRadiusValue = schema.indexOf("\"borderRadiusValue\": {");
        int boxDecorationValue = schema.indexOf(
                "\"boxDecorationValue\"", borderRadiusValue);
        assertTrue(borderRadiusValue >= 0 && boxDecorationValue > borderRadiusValue);
        String definition = schema.substring(borderRadiusValue, boxDecorationValue);
        assertTrue(definition.contains("\"const\": \"borderRadius\""));
        assertTrue(definition.contains("\"geometry\""));
        assertTrue(definition.contains(
                "\"$ref\": \"#/$defs/borderRadiusGeometry\""));
        assertTrue(definition.contains("\"additionalProperties\": false"));
    }

    @Test
    void packagesCanonicalV12WithTypedDartObjectReferences() throws IOException {
        byte[] packaged = loadPackagedV12Schema();
        byte[] documented = Files.readAllBytes(findRepositoryFile(
                V12_DOCUMENTATION_SCHEMA));
        assertArrayEquals(documented, packaged,
                "The bundled and browsable schema v12 copies must remain byte-identical");
        String schema = new String(packaged, StandardCharsets.UTF_8);

        assertFalse(schema.contains("\r"));
        assertTrue(schema.contains(
                "\"$id\": \"urn:netbeans-flutter-designer:schema:fd:12\""));
        assertTrue(schema.contains("\"const\": 12"));
        assertTrue(schema.contains(
                "\"$ref\": \"#/$defs/dartObjectReferenceValue\""));
        int referenceValue = schema.indexOf("\"dartObjectReferenceValue\": {");
        int themeToken = schema.indexOf("\"themeToken\": {", referenceValue);
        assertTrue(referenceValue >= 0 && themeToken > referenceValue);
        String definition = schema.substring(referenceValue, themeToken);
        assertTrue(definition.contains("\"oneOf\""));
        assertTrue(definition.contains("\"const\": \"dartObjectReference\""));
        assertTrue(definition.contains("\"libraryUri\""));
        assertTrue(definition.contains("\"rootSymbol\""));
        assertTrue(definition.contains("\"member\""));
        assertTrue(definition.contains("\"const\": \"reference\""));
        assertTrue(definition.contains(
                "\"const\": \"zeroArgumentInvocation\""));
        assertTrue(definition.contains("\"constant\""));
        assertTrue(definition.contains("\"type\": \"boolean\""));
        assertTrue(definition.contains(
                "\"$ref\": \"#/$defs/publicDartObjectIdentifier\""));
        assertTrue(definition.contains("\"additionalProperties\": false"));
    }

    @Test
    void packagesCanonicalV13WithClosedShapeBorderClipperAndPreservesFrozenV12() throws IOException {
        byte[] packaged;
        try (InputStream input = FdSchemas.openV13()) { packaged = input.readAllBytes(); }
        assertArrayEquals(Files.readAllBytes(findRepositoryFile(
                Path.of("docs", "flutter-designer", "fd-v13.schema.json"))), packaged);
        String schema = new String(packaged, StandardCharsets.UTF_8);
        assertFalse(schema.contains("\r"));
        assertTrue(schema.contains("urn:netbeans-flutter-designer:schema:fd:13"));
        assertTrue(schema.contains("\"const\": \"shapeBorderClipper\""));
        assertTrue(schema.contains("\"$ref\": \"#/$defs/shapeBorderClipperValue\""));
        for (String shape : java.util.List.of("roundedRectangle", "beveledRectangle", "continuousRectangle",
                "roundedSuperellipse", "circle", "stadium")) { assertTrue(schema.contains(shape)); }
        assertFalse(new String(loadPackagedV12Schema(), StandardCharsets.UTF_8).contains("shapeBorderClipperValue"));
    }

    @Test
    void packagesCanonicalV14StateBindingsWithoutChangingFrozenV13() throws IOException {
        byte[] packaged;
        try (InputStream input = FdSchemas.openV14()) { packaged = input.readAllBytes(); }
        assertArrayEquals(Files.readAllBytes(findRepositoryFile(
                Path.of("docs", "flutter-designer", "fd-v14.schema.json"))), packaged);
        String schema = new String(packaged, StandardCharsets.UTF_8);
        assertFalse(schema.contains("\r"));
        assertTrue(schema.contains("urn:netbeans-flutter-designer:schema:fd:14"));
        assertTrue(schema.contains("\"const\": 14"));
        assertTrue(schema.contains("\"stateBinding\""));
        assertTrue(schema.contains("\"previousOnChanged\""));
        assertTrue(schema.contains("\"referenceType\""));
        assertTrue(schema.contains("\"nullableReference\""));
        try (InputStream input = FdSchemas.openV13()) {
            assertFalse(new String(input.readAllBytes(), StandardCharsets.UTF_8).contains("stateBinding"));
        }
    }

    @Test
    void packagesCanonicalV15StateProjectionsWithoutChangingFrozenV14() throws IOException {
        byte[] packaged;
        try (InputStream input = FdSchemas.openV15()) { packaged = input.readAllBytes(); }
        assertArrayEquals(Files.readAllBytes(findRepositoryFile(
                Path.of("docs", "flutter-designer", "fd-v15.schema.json"))), packaged);
        String schema = new String(packaged, StandardCharsets.UTF_8);
        assertFalse(schema.contains("\r"));
        assertTrue(schema.contains("urn:netbeans-flutter-designer:schema:fd:15"));
        assertTrue(schema.contains("\"const\": 15"));
        assertTrue(schema.contains("\"propertyBindings\""));
        assertTrue(schema.contains("\"comparisonValue\""));
        assertTrue(schema.contains("\"textController\""));
        try (InputStream input = FdSchemas.openV14()) {
            assertFalse(new String(input.readAllBytes(), StandardCharsets.UTF_8).contains("propertyBindings"));
        }
    }

    @Test
    void packagesCanonicalV16PointerDeviceSetsWithoutChangingFrozenSchemas() throws IOException {
        byte[] packaged;
        try (InputStream input = FdSchemas.openV16()) { packaged = input.readAllBytes(); }
        assertArrayEquals(Files.readAllBytes(findRepositoryFile(
                Path.of("docs", "flutter-designer", "fd-v16.schema.json"))), packaged);
        assertArrayEquals(packaged, loadCurrentSchema());
        String schema = new String(packaged, StandardCharsets.UTF_8);
        assertFalse(schema.contains("\r"));
        assertTrue(schema.contains("urn:netbeans-flutter-designer:schema:fd:16"));
        assertTrue(schema.contains("\"const\": 16"));
        assertTrue(schema.contains("\"pointerDeviceKindSetValue\""));
        assertTrue(schema.contains("\"uniqueItems\": true"));
        for (String kind : java.util.List.of("touch", "mouse", "stylus", "invertedStylus", "trackpad", "unknown")) {
            assertTrue(schema.contains("\"" + kind + "\""));
        }
        try (InputStream input = FdSchemas.openV15()) {
            assertFalse(new String(input.readAllBytes(), StandardCharsets.UTF_8).contains("pointerDeviceKindSet"));
        }
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

    private static byte[] loadPackagedV7Schema() throws IOException {
        ClassLoader loader = FdSchemaResourceTest.class.getClassLoader();
        assertNotNull(loader.getResource(FdSchemas.V7_RESOURCE),
                "The canonical schema v7 must be present on the runtime classpath");
        try (InputStream input = FdSchemas.openV7()) {
            assertNotNull(input, "The canonical schema v7 resource must be readable");
            return input.readAllBytes();
        }
    }

    private static byte[] loadPackagedV8Schema() throws IOException {
        ClassLoader loader = FdSchemaResourceTest.class.getClassLoader();
        assertNotNull(loader.getResource(FdSchemas.V8_RESOURCE),
                "The canonical schema v8 must be present on the runtime classpath");
        try (InputStream input = FdSchemas.openV8()) {
            assertNotNull(input, "The canonical schema v8 resource must be readable");
            return input.readAllBytes();
        }
    }

    private static byte[] loadPackagedV9Schema() throws IOException {
        ClassLoader loader = FdSchemaResourceTest.class.getClassLoader();
        assertNotNull(loader.getResource(FdSchemas.V9_RESOURCE),
                "The canonical schema v9 must be present on the runtime classpath");
        try (InputStream input = FdSchemas.openV9()) {
            assertNotNull(input, "The canonical schema v9 resource must be readable");
            return input.readAllBytes();
        }
    }

    private static byte[] loadPackagedV10Schema() throws IOException {
        ClassLoader loader = FdSchemaResourceTest.class.getClassLoader();
        assertNotNull(loader.getResource(FdSchemas.V10_RESOURCE),
                "The canonical schema v10 must be present on the runtime classpath");
        try (InputStream input = FdSchemas.openV10()) {
            assertNotNull(input, "The canonical schema v10 resource must be readable");
            return input.readAllBytes();
        }
    }

    private static byte[] loadPackagedV11Schema() throws IOException {
        ClassLoader loader = FdSchemaResourceTest.class.getClassLoader();
        assertNotNull(loader.getResource(FdSchemas.V11_RESOURCE),
                "The canonical schema v11 must be present on the runtime classpath");
        try (InputStream input = FdSchemas.openV11()) {
            assertNotNull(input, "The canonical schema v11 resource must be readable");
            return input.readAllBytes();
        }
    }

    private static byte[] loadPackagedV12Schema() throws IOException {
        ClassLoader loader = FdSchemaResourceTest.class.getClassLoader();
        assertNotNull(loader.getResource(FdSchemas.V12_RESOURCE),
                "The canonical schema v12 must be present on the runtime classpath");
        try (InputStream input = FdSchemas.openV12()) {
            assertNotNull(input, "The canonical schema v12 resource must be readable");
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
