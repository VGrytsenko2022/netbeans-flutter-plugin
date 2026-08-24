package dev.flutter.netbeans.plugin.pubspec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PubspecCompletionEngineTest {
    @TempDir
    Path workspace;

    private final PubspecCompletionEngine engine = new PubspecCompletionEngine();

    @Test
    void completesRootKeysAndReplacesOnlyTheTypedPrefix() {
        String source = "na";

        PubspecCompletionEngine.Suggestion suggestion = complete(source).stream()
                .filter(item -> item.label().equals("name"))
                .findFirst()
                .orElseThrow();

        assertEquals("name: ", suggestion.insertText());
        assertEquals(0, suggestion.replaceStart());
        assertEquals(2, suggestion.replaceEnd());
    }

    @Test
    void suppressesKeysAlreadyPresentInTheCurrentMapping() {
        String source = """
                name: sample_app
                environment:
                  sdk: ^3.5.0
                """;

        Set<String> labels = labels(complete(source));

        assertFalse(labels.contains("name"));
        assertFalse(labels.contains("environment"));
        assertTrue(labels.contains("flutter"));
    }

    @Test
    void completesEnvironmentFlutterAndNestedSourceKeys() {
        assertEquals(Set.of("sdk"), labels(complete("environment:\n  s")));
        assertTrue(labels(complete("flutter:\n  us"))
                .contains("uses-material-design"));
        assertTrue(labels(complete("dependencies:\n  sample:\n    p"))
                .contains("path"));
        assertEquals(Set.of("url"), labels(complete("""
                dependencies:
                  sample:
                    git:
                      u""")));
    }

    @Test
    void completesStructuredListItems() {
        assertEquals(Set.of("family"), labels(complete("""
                flutter:
                  fonts:
                    - fa""")));
        assertEquals(Set.of("asset"), labels(complete("""
                flutter:
                  fonts:
                    - family: Inter
                      fonts:
                        - as""")));
        assertEquals(Set.of("description"), labels(complete("""
                screenshots:
                  - de""")));
    }

    @Test
    void suggestsFlutterSdkPackagesForDependencySections() {
        Set<String> runtime = labels(complete("dependencies:\n  fl"));
        Set<String> development = labels(complete("dev_dependencies:\n  fl"));

        assertEquals(Set.of("flutter", "flutter_localizations"), runtime);
        assertEquals(Set.of("flutter_test"), development);
        PubspecCompletionEngine.Suggestion flutter = complete("dependencies:\n  fl").stream()
                .filter(item -> item.label().equals("flutter"))
                .findFirst()
                .orElseThrow();
        assertEquals("flutter:\n    sdk: flutter", flutter.insertText());
    }

    @Test
    void suggestsBoundedLocalPackagesWithPortableRelativePaths() throws Exception {
        Path current = createPackage("apps/current", "current_app");
        createPackage("packages/local_tools", "local_tools");
        String source = """
                name: current_app
                environment:
                  sdk: ^3.5.0
                dependencies:
                  loc""";

        List<PubspecCompletionEngine.Suggestion> suggestions = engine.complete(
                source,
                source.length(),
                workspace,
                current,
                () -> false);
        PubspecCompletionEngine.Suggestion local = suggestions.stream()
                .filter(item -> item.label().equals("local_tools"))
                .findFirst()
                .orElseThrow();

        assertEquals("local_tools:\n    path: '../../packages/local_tools'", local.insertText());
        assertEquals(5, local.priority());
    }

    @Test
    void doesNotSuggestAnAlreadyDeclaredLocalPackage() throws Exception {
        Path current = createPackage("apps/current", "current_app");
        createPackage("packages/local_tools", "local_tools");
        String source = """
                dependencies:
                  local_tools:
                    path: ../../packages/local_tools
                  """;

        assertFalse(labels(engine.complete(
                source,
                source.length(),
                workspace,
                current,
                () -> false)).contains("local_tools"));
    }

    @Test
    void preservesCrLfWhenGeneratingMultilineInsertion() {
        String source = "dependencies:\r\n  fl";
        PubspecCompletionEngine.Suggestion flutter = engine.complete(
                        source, source.length(), workspace, workspace, () -> false).stream()
                .filter(item -> item.label().equals("flutter"))
                .findFirst()
                .orElseThrow();

        assertEquals("flutter:\r\n    sdk: flutter", flutter.insertText());
    }

    @Test
    void avoidsCompletionInCommentsValuesAndOversizedOrCancelledDocuments() {
        assertEquals(List.of(), complete("# na"));
        assertEquals(List.of(), complete("name: sa"));
        assertEquals(List.of(), engine.complete(
                " ".repeat(PubspecValidator.MAX_DOCUMENT_LENGTH + 1),
                0,
                workspace,
                workspace,
                () -> false));
        assertEquals(List.of(), engine.complete(
                "na", 2, workspace, workspace, () -> true));
    }

    private List<PubspecCompletionEngine.Suggestion> complete(String source) {
        return engine.complete(
                source,
                source.length(),
                workspace,
                workspace,
                () -> false);
    }

    private Path createPackage(String relativePath, String name) throws Exception {
        Path directory = workspace.resolve(relativePath);
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("pubspec.yaml"), "name: " + name + "\n");
        return directory;
    }

    private static Set<String> labels(
            List<PubspecCompletionEngine.Suggestion> suggestions) {
        return suggestions.stream()
                .map(PubspecCompletionEngine.Suggestion::label)
                .collect(Collectors.toSet());
    }
}
