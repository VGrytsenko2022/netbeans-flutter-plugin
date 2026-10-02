package io.github.vgrytsenko2022.plugin.tooling.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vgrytsenko2022.plugin.tooling.DartSourceLocation;
import io.github.vgrytsenko2022.run.FlutterTestCase;
import io.github.vgrytsenko2022.run.FlutterTestSuite;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DartSourceLocationResolverTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void resolvesTestMetadataAndProducesAProjectRelativeRerunPath() throws Exception {
        Path root = Files.createDirectories(temporaryDirectory.resolve("project"));
        Path source = dartFile(root.resolve("test/widgets/login_test.dart"));
        DartSourceLocationResolver resolver = new DartSourceLocationResolver(root);
        FlutterTestSuite suite = new FlutterTestSuite(
                1, "vm", Optional.of("test/widgets/login_test.dart"));
        FlutterTestCase testcase = testcase(
                Optional.of(source.toUri().toString()), Optional.of(17), Optional.of(9));

        DartSourceLocation location = resolver.resolve(testcase, suite).orElseThrow();

        assertEquals(source.toAbsolutePath().normalize().toString(), location.sourcePath());
        assertEquals(17, location.line());
        assertEquals(9, location.column());
        assertEquals(
                Optional.of("test/widgets/login_test.dart"),
                resolver.projectRelativePath(testcase, suite));
    }

    @Test
    void resolvesTypicalParenthesizedFileStackFrames() throws Exception {
        Path root = Files.createDirectories(temporaryDirectory.resolve("project with spaces"));
        Path source = dartFile(root.resolve("test/a test.dart"));
        DartSourceLocationResolver resolver = new DartSourceLocationResolver(root);

        DartSourceLocation location = resolver.resolveStackFrame(
                "#0      main.<anonymous closure> (" + source.toUri() + ":12:7)")
                .orElseThrow();

        assertEquals(source.toAbsolutePath().normalize().toString(), location.sourcePath());
        assertEquals(12, location.line());
        assertEquals(7, location.column());
    }

    @Test
    void resolvesPackageUrisUsingDartPackageConfiguration() throws Exception {
        Path root = Files.createDirectories(temporaryDirectory.resolve("project"));
        Path dependency = Files.createDirectories(temporaryDirectory.resolve("sample_package"));
        Path source = dartFile(dependency.resolve("lib/src/value.dart"));
        Path dartTool = Files.createDirectories(root.resolve(".dart_tool"));
        Files.writeString(dartTool.resolve("package_config.json"), """
                {"configVersion":2,"packages":[
                  {"name":"sample","rootUri":"%s","packageUri":"lib/"}
                ]}
                """.formatted(dependency.toUri()));
        DartSourceLocationResolver resolver = new DartSourceLocationResolver(root);

        DartSourceLocation location = resolver.resolveStackFrame(
                "package:sample/src/value.dart 31:4").orElseThrow();

        assertEquals(source.toAbsolutePath().normalize().toString(), location.sourcePath());
        assertEquals(31, location.line());
        assertEquals(4, location.column());
    }

    @Test
    void rejectsMissingAndProjectEscapingRelativeLocations() throws Exception {
        Path root = Files.createDirectories(temporaryDirectory.resolve("project"));
        dartFile(temporaryDirectory.resolve("outside.dart"));
        DartSourceLocationResolver resolver = new DartSourceLocationResolver(root);

        assertTrue(resolver.resolve("test/missing.dart", 1, 1).isEmpty());
        assertTrue(resolver.resolve("../outside.dart", 1, 1).isEmpty());
        assertTrue(resolver.resolveStackFrame("not a Dart frame").isEmpty());
    }

    private static FlutterTestCase testcase(
            Optional<String> url,
            Optional<Integer> line,
            Optional<Integer> column) {
        return new FlutterTestCase(10, "renders login", 1, List.of(), line, column, url);
    }

    private static Path dartFile(Path path) throws Exception {
        Files.createDirectories(path.getParent());
        return Files.writeString(path, "void main() {}\n");
    }
}
