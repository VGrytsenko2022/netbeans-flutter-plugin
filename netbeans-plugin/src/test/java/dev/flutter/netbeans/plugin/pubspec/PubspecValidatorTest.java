package dev.flutter.netbeans.plugin.pubspec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PubspecValidatorTest {
    @TempDir
    Path packageRoot;

    private final PubspecValidator validator = new PubspecValidator();

    @Test
    void acceptsAWellFormedFlutterPubspecWithExistingLocalFiles() throws Exception {
        Files.createDirectories(packageRoot.resolve("packages/local_package"));
        Files.createDirectories(packageRoot.resolve("assets/images"));
        Files.createDirectories(packageRoot.resolve("shaders"));
        Files.createDirectories(packageRoot.resolve("fonts"));
        Files.writeString(packageRoot.resolve("assets/LICENSE"), "license\n");
        Files.writeString(packageRoot.resolve("shaders/background.frag"), "void main() {}\n");
        Files.writeString(packageRoot.resolve("fonts/Inter.ttf"), "font\n");

        String source = """
                name: sample_app
                description: A sample Flutter application
                version: 1.0.0+1
                environment:
                  sdk: '>=3.5.0 <4.0.0'
                  flutter: '>=3.24.0'
                dependencies:
                  flutter:
                    sdk: flutter
                  local_package:
                    path: packages/local_package
                dev_dependencies:
                  flutter_test:
                    sdk: flutter
                flutter:
                  uses-material-design: true
                  generate: false
                  assets:
                    - assets/images/
                  licenses:
                    - assets/LICENSE
                  shaders:
                    - shaders/background.frag
                  fonts:
                    - family: Inter
                      fonts:
                        - asset: fonts/Inter.ttf
                          weight: 400
                """;

        assertEquals(List.of(), validator.validate(source, packageRoot));
    }

    @Test
    void reportsRequiredNameAndEnvironment() {
        Set<String> codes = codes(validator.validate("description: demo\n", packageRoot));

        assertEquals(Set.of(
                "pubspec.name.missing",
                "pubspec.environment.missing"), codes);
    }

    @Test
    void reportsInvalidNameEnvironmentAndSectionTypes() {
        String source = """
                name: Bad-Name
                environment: any
                dependencies: []
                dev_dependencies: true
                dependency_overrides: version
                flutter: []
                """;

        List<PubspecDiagnostic> diagnostics = validator.validate(source, packageRoot);
        Set<String> codes = codes(diagnostics);

        assertTrue(codes.contains("pubspec.name.invalid"));
        assertTrue(codes.contains("pubspec.environment.type"));
        assertEquals(3, diagnostics.stream()
                .filter(diagnostic -> diagnostic.code().equals("pubspec.section.type"))
                .count());
        assertTrue(codes.contains("pubspec.flutter.type"));
    }

    @Test
    void reportsMissingSdkConstraint() {
        String source = """
                name: sample_app
                environment:
                  flutter: '>=3.24.0'
                """;

        assertTrue(codes(validator.validate(source, packageRoot))
                .contains("pubspec.environment.sdk.missing"));
    }

    @Test
    void reportsConflictingDependencySourcesAndMissingPath() {
        String source = """
                name: sample_app
                environment:
                  sdk: ^3.5.0
                dependencies:
                  local_package:
                    path: packages/missing
                    git:
                      url: https://example.invalid/package.git
                """;

        List<PubspecDiagnostic> diagnostics = validator.validate(source, packageRoot);

        assertTrue(codes(diagnostics).contains("pubspec.dependency.source.conflict"));
        assertTrue(codes(diagnostics).contains("pubspec.path.missing"));
        assertTrue(diagnostics.stream()
                .filter(diagnostic -> diagnostic.code().equals("pubspec.dependency.source.conflict"))
                .allMatch(diagnostic -> diagnostic.message().contains("git, path")));
    }

    @Test
    void reportsFlutterBooleanAndListTypes() {
        String source = """
                name: sample_app
                environment:
                  sdk: ^3.5.0
                flutter:
                  uses-material-design: yes
                  generate: 1
                  assets: assets/
                  licenses: LICENSE
                  shaders: shader.frag
                  fonts:
                    - family: Inter
                      fonts: fonts/Inter.ttf
                """;

        List<PubspecDiagnostic> diagnostics = validator.validate(source, packageRoot);
        Set<String> codes = codes(diagnostics);

        assertEquals(2, diagnostics.stream()
                .filter(diagnostic -> diagnostic.code().equals("pubspec.flutter.boolean.type"))
                .count());
        assertEquals(3, diagnostics.stream()
                .filter(diagnostic -> diagnostic.code().equals("pubspec.flutter.list.type"))
                .count());
        assertTrue(codes.contains("pubspec.flutter.fonts.type"));
    }

    @Test
    void reportsMissingAssetAndFontFiles() {
        String source = """
                name: sample_app
                environment:
                  sdk: ^3.5.0
                flutter:
                  assets:
                    - assets/missing.png
                  fonts:
                    - family: Inter
                      fonts:
                        - asset: fonts/missing.ttf
                """;

        List<PubspecDiagnostic> diagnostics = validator.validate(source, packageRoot);

        assertEquals(2, diagnostics.stream()
                .filter(diagnostic -> diagnostic.code().equals("pubspec.asset.missing"))
                .count());
        assertTrue(diagnostics.stream()
                .filter(diagnostic -> diagnostic.code().equals("pubspec.asset.missing"))
                .allMatch(diagnostic -> diagnostic.severity()
                        == PubspecDiagnostic.Severity.WARNING));
    }

    @Test
    void convertsYamlCodePointMarksToJavaDocumentOffsets() {
        String source = """
                description: "😀"
                name: Bad
                environment:
                  sdk: ^3.5.0
                """;

        PubspecDiagnostic invalidName = validator.validate(source, packageRoot).stream()
                .filter(diagnostic -> diagnostic.code().equals("pubspec.name.invalid"))
                .findFirst()
                .orElseThrow();

        assertEquals(source.indexOf("Bad"), invalidName.startOffset());
        assertEquals(source.indexOf("Bad") + "Bad".length(), invalidName.endOffset());
    }

    @Test
    void ignoresSyntaxErrorsOwnedByTheYamlParser() {
        assertEquals(List.of(), validator.validate("name: [unterminated\n", packageRoot));
    }

    @Test
    void honoursCancellationWithoutPublishingPartialDiagnostics() {
        String source = """
                name: sample_app
                environment:
                  sdk: ^3.5.0
                dependencies:
                  local:
                    path: missing
                flutter:
                  assets:
                    - missing.png
                """;
        AtomicInteger checks = new AtomicInteger();

        assertEquals(List.of(), validator.validate(
                source,
                packageRoot,
                () -> checks.incrementAndGet() > 3));
        assertTrue(checks.get() > 3);
    }

    @Test
    void skipsDocumentsAboveTheSizeGuard() {
        String source = " ".repeat(PubspecValidator.MAX_DOCUMENT_LENGTH + 1);

        assertEquals(List.of(), validator.validate(source, packageRoot));
    }

    @Test
    void rejectsReservedDartWordsAsPackageNames() {
        assertFalse(PubspecValidator.isValidPackageName("class"));
        assertTrue(PubspecValidator.isValidPackageName("class_helpers2"));
    }

    private static Set<String> codes(List<PubspecDiagnostic> diagnostics) {
        return diagnostics.stream()
                .map(PubspecDiagnostic::code)
                .collect(Collectors.toSet());
    }

}
