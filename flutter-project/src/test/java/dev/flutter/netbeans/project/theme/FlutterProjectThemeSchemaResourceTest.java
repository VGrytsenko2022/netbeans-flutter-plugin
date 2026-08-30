package dev.flutter.netbeans.project.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Keeps the browsable project-theme v5 contract aligned with the typed model. */
class FlutterProjectThemeSchemaResourceTest {
    private static final Path V4_SCHEMA =
            Path.of("docs", "flutter-designer", "project-theme-v4.schema.json");
    private static final Path V5_SCHEMA =
            Path.of("docs", "flutter-designer", "project-theme-v5.schema.json");
    private static final Path EXAMPLE =
            Path.of("docs", "flutter-designer", "examples", "project.fdtheme");

    @Test
    void v5SchemaPinsTheExactTypedComponentColorCatalog() throws Exception {
        String schema = readAndValidateJson(V5_SCHEMA);
        List<String> expected = Arrays.stream(FlutterThemeComponentColorRole.values())
                .map(FlutterThemeComponentColorRole::wireName)
                .toList();

        assertFalse(schema.contains("\r"), "The canonical schema must use LF line endings");
        assertTrue(schema.contains(
                "\"$id\": \"urn:netbeans-flutter-designer:schema:project-theme:5\""));
        assertTrue(schema.contains("\"schemaVersion\": {\"const\": 5}"));
        assertTrue(schema.contains("\"maxProperties\": 36"));
        assertEquals(expected, stringArray(schema, "componentColorRole", "enum"));
        assertTrue(schema.contains(
                "\"components\": {\"$ref\": \"#/$defs/componentColorOverrides\"}"));
    }

    @Test
    void frozenV4SchemaDoesNotAdmitComponentColors() throws Exception {
        String schema = readAndValidateJson(V4_SCHEMA);

        assertTrue(schema.contains(
                "\"$id\": \"urn:netbeans-flutter-designer:schema:project-theme:4\""));
        assertTrue(schema.contains("\"schemaVersion\": {\"const\": 4}"));
        assertFalse(schema.contains("componentColorRole"));
        assertFalse(schema.contains("componentColorOverrides"));
        assertFalse(schema.contains("\"components\""));
    }

    @Test
    void documentedExampleIsAValidCanonicalV5Descriptor() throws Exception {
        byte[] bytes = Files.readAllBytes(findRepositoryFile(EXAMPLE));
        FlutterProjectTheme decoded = new FlutterProjectThemeCodec().decode(bytes);

        assertTrue(decoded.themes().stream()
                .allMatch(theme -> theme.overrides().componentColors().isEmpty()));
        String canonical = new String(
                new FlutterProjectThemeCodec().encode(decoded), StandardCharsets.UTF_8);
        assertTrue(canonical.contains("\"schemaVersion\" : 5"));
        assertEquals(decoded.themes().size(), occurrences(canonical, "\"components\" : { }"));
    }

    private static String readAndValidateJson(Path relativePath) throws IOException {
        byte[] bytes = Files.readAllBytes(findRepositoryFile(relativePath));
        try (var parser = new JsonFactory().createParser(bytes)) {
            while (parser.nextToken() != null) {
                // Exhausting the parser proves the documentation artifact is valid JSON.
            }
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static List<String> stringArray(
            String json, String objectField, String arrayField) {
        Pattern blockPattern = Pattern.compile(
                "\\\"" + Pattern.quote(objectField)
                + "\\\"\\s*:\\s*\\{.*?\\\"" + Pattern.quote(arrayField)
                + "\\\"\\s*:\\s*\\[(.*?)]",
                Pattern.DOTALL);
        Matcher block = blockPattern.matcher(json);
        assertTrue(block.find(), "Missing " + objectField + "." + arrayField);
        Matcher value = Pattern.compile("\\\"([^\\\"]+)\\\"").matcher(block.group(1));
        List<String> result = new ArrayList<>();
        while (value.find()) {
            result.add(value.group(1));
        }
        return List.copyOf(result);
    }

    private static int occurrences(String source, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }

    private static Path findRepositoryFile(Path relativePath) {
        Path directory = Path.of(System.getProperty("user.dir"))
                .toAbsolutePath().normalize();
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
