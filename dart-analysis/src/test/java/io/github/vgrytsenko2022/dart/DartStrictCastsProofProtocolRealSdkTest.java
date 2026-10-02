package io.github.vgrytsenko2022.dart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Real-SDK protocol experiment for a proof-owned strict-casts session.
 *
 * <p>This fixture deliberately uses the native analyzer protocol rather than
 * {@link DartCandidateAnalyzer}. It verifies the ordering and language rules
 * needed before the production analyzer adopts this proof shape.</p>
 */
class DartStrictCastsProofProtocolRealSdkTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Map<String, String> FLUTTER_3448_HOSTED_PACKAGES =
            new LinkedHashMap<>(Map.of(
                    "characters", "1.4.1",
                    "collection", "1.19.1",
                    "material_color_utilities", "0.13.0",
                    "meta", "1.18.0",
                    "vector_math", "2.2.0"));
    private static final String PROOF_OPTIONS = """
            analyzer:
              language:
                strict-casts: true
            """;

    @TempDir
    Path workspace;

    @Test
    void preRootOptionsOverlayEnforcesExactTypedAssignmentProofs()
            throws Exception {
        Path dart = configuredDartExecutable();
        Path flutterSdk = configuredFlutter3448Sdk();
        Path projectRoot = Files.createDirectories(workspace.resolve("project"));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        writeFlutterPackageConfig(projectRoot, flutterSdk);
        Path options = projectRoot.resolve("analysis_options.yaml");
        assertFalse(Files.exists(options),
                "The proof options path must be virtual before overlay installation");
        Path dartFile = lib.resolve("main.dart");
        Files.writeString(dartFile, "void diskVersion() {}\n",
                StandardCharsets.UTF_8);

        List<String> stderr = new ArrayList<>();
        try (DartAnalyzerProtocolSession session = DartAnalyzerProtocolSession.start(
                dart,
                projectRoot,
                line -> {
                    synchronized (stderr) {
                        stderr.add(line);
                    }
                },
                DartCandidateAnalysisLimits.DEFAULT)) {
            session.awaitConnected();

            // The proof owns analysis semantics. The analyzer must accept a
            // virtual options file before roots are registered.
            assertSucceeded(session.request(
                    "analysis.updateContent",
                    update(options, PROOF_OPTIONS, 1)), stderr);
            ObjectNode roots = JSON.createObjectNode();
            roots.set("included", array(projectRoot.toString()));
            roots.set("excluded", JSON.createArrayNode());
            assertSucceeded(session.request(
                    "analysis.setAnalysisRoots", roots), stderr);

            String candidate = candidate(false);
            assertSucceeded(session.request(
                    "analysis.updateContent",
                    update(dartFile, candidate, 1)), stderr);
            JsonNode errors = errors(session, dartFile, stderr);

            assertNoErrorOnLine(errors, candidate, "ctorProof =");
            assertNoErrorOnLine(errors, candidate, "factoryProof =");
            assertNoErrorOnLine(errors, candidate, "validConstCallSite() =>");
            assertErrorCodeOnLine(errors, candidate, "dynamicProof =",
                    "invalid_assignment");
            assertErrorCodeOnLine(errors, candidate, "nullableProof =",
                    "invalid_assignment");
            assertErrorCodeOnLine(errors, candidate, "wrongProof =",
                    "invalid_assignment");
            assertErrorCodeOnLine(errors, candidate, "requiredProof =",
                    "not_enough_positional_arguments");
            assertErrorCodeOnLine(errors, candidate, "invalidConstProof =",
                    "const_with_non_const");

            String sourceSuppressed = candidate(true);
            assertSucceeded(session.request(
                    "analysis.updateContent",
                    update(dartFile, sourceSuppressed, 2)), stderr);
            JsonNode suppressedErrors = errors(session, dartFile, stderr);
            assertNoErrorCodeOnLine(suppressedErrors, sourceSuppressed,
                    "dynamicProof =", "invalid_assignment");
            assertNoErrorCodeOnLine(suppressedErrors, sourceSuppressed,
                    "nullableProof =", "invalid_assignment");
            assertNoErrorCodeOnLine(suppressedErrors, sourceSuppressed,
                    "wrongProof =", "invalid_assignment");

            ObjectNode removals = JSON.createObjectNode();
            removals.set(dartFile.toString(),
                    JSON.createObjectNode().put("type", "remove"));
            removals.set(options.toString(),
                    JSON.createObjectNode().put("type", "remove"));
            assertSucceeded(session.request(
                    "analysis.updateContent",
                    JSON.createObjectNode().set("files", removals)), stderr);
            assertSucceeded(session.request("server.shutdown", null), stderr);
        }
    }

    private static String candidate(boolean suppressAssignmentDiagnostics) {
        String fileIgnore = suppressAssignmentDiagnostics
                ? "// ignore_for_file: invalid_assignment\n"
                : "";
        return fileIgnore + """
                import 'package:flutter/widgets.dart';

                class GenericCtorClipper<T> extends CustomClipper<T> {
                  const GenericCtorClipper();

                  @override
                  T getClip(Size size) => throw UnimplementedError();

                  @override
                  bool shouldReclip(covariant GenericCtorClipper<T> oldClipper) => false;
                }

                abstract class GenericFactoryClipper<T> extends CustomClipper<T> {
                  const GenericFactoryClipper._();
                  const factory GenericFactoryClipper() = GenericFactoryClipperImpl<T>;
                }

                class GenericFactoryClipperImpl<T> extends GenericFactoryClipper<T> {
                  const GenericFactoryClipperImpl() : super._();

                  @override
                  T getClip(Size size) => throw UnimplementedError();

                  @override
                  bool shouldReclip(
                    covariant GenericFactoryClipperImpl<T> oldClipper,
                  ) => false;
                }

                class RequiredRRectClipper extends CustomClipper<RRect> {
                  const RequiredRRectClipper(this.radius);
                  final double radius;

                  @override
                  RRect getClip(Size size) => RRect.zero;

                  @override
                  bool shouldReclip(covariant RequiredRRectClipper oldClipper) => false;
                }

                class NonConstRRectClipper extends CustomClipper<RRect> {
                  NonConstRRectClipper();

                  @override
                  RRect getClip(Size size) => RRect.zero;

                  @override
                  bool shouldReclip(covariant NonConstRRectClipper oldClipper) => false;
                }

                const CustomClipper<RRect> actualClipper = GenericCtorClipper<RRect>();
                dynamic dynamicClipper = actualClipper;
                CustomClipper<RRect>? nullableClipper = actualClipper;
                CustomClipper<Rect> wrongClipper = const GenericCtorClipper<Rect>();

                void proofAssignments() {
                  final CustomClipper<RRect> ctorProof = GenericCtorClipper();
                  final CustomClipper<RRect> factoryProof = GenericFactoryClipper();
                  final CustomClipper<RRect> dynamicProof = dynamicClipper;
                  final CustomClipper<RRect> nullableProof = nullableClipper;
                  final CustomClipper<RRect> wrongProof = wrongClipper;
                  final CustomClipper<RRect> requiredProof = const RequiredRRectClipper();
                  final CustomClipper<RRect> invalidConstProof = const NonConstRRectClipper();
                }

                Widget validConstCallSite() => const ClipRRect(
                  clipper: GenericFactoryClipper(),
                );
                """;
    }

    private static JsonNode errors(
            DartAnalyzerProtocolSession session,
            Path file,
            List<String> stderr) throws Exception {
        DartAnalyzerProtocolSession.Response response = session.request(
                "analysis.getErrors",
                JSON.createObjectNode().put("file", file.toString()));
        assertSucceeded(response, stderr);
        JsonNode errors = response.result().path("errors");
        assertTrue(errors.isArray(), () -> "errors=" + response.result());
        return errors;
    }

    private static void assertNoErrorOnLine(
            JsonNode errors,
            String content,
            String marker) {
        int[] range = lineRange(content, marker);
        assertTrue(errorCodesOnLine(errors, range).isEmpty(),
                () -> marker + ": " + errors);
    }

    private static void assertErrorCodeOnLine(
            JsonNode errors,
            String content,
            String marker,
            String code) {
        int[] range = lineRange(content, marker);
        assertTrue(errorCodesOnLine(errors, range).contains(code),
                () -> marker + " expected " + code + ": " + errors);
    }

    private static void assertNoErrorCodeOnLine(
            JsonNode errors,
            String content,
            String marker,
            String code) {
        int[] range = lineRange(content, marker);
        assertFalse(errorCodesOnLine(errors, range).contains(code),
                () -> marker + " unexpectedly contained " + code + ": " + errors);
    }

    private static List<String> errorCodesOnLine(JsonNode errors, int[] range) {
        ArrayList<String> codes = new ArrayList<>();
        for (JsonNode error : errors) {
            if (!"ERROR".equals(error.path("severity").asText())) {
                continue;
            }
            int offset = error.path("location").path("offset").asInt(-1);
            int length = error.path("location").path("length").asInt(0);
            if (offset < range[1] && (long) offset + Math.max(length, 1) > range[0]) {
                codes.add(error.path("code").asText());
            }
        }
        return List.copyOf(codes);
    }

    private static int[] lineRange(String content, String marker) {
        int markerOffset = content.indexOf(marker);
        assertTrue(markerOffset >= 0, () -> "Missing marker: " + marker);
        int start = content.lastIndexOf('\n', markerOffset);
        int end = content.indexOf('\n', markerOffset);
        return new int[]{start < 0 ? 0 : start + 1,
            end < 0 ? content.length() : end};
    }

    private static ObjectNode update(Path file, String content, long version) {
        ObjectNode files = JSON.createObjectNode();
        files.set(file.toString(), JSON.createObjectNode()
                .put("type", "add")
                .put("content", content)
                .put("version", version));
        return JSON.createObjectNode().set("files", files);
    }

    private static ArrayNode array(String value) {
        return JSON.createArrayNode().add(value);
    }

    private static void assertSucceeded(
            DartAnalyzerProtocolSession.Response response,
            List<String> stderr) {
        assertFalse(response.failed(),
                () -> "protocol error=" + response.error() + " stderr=" + stderr);
    }

    private void writeFlutterPackageConfig(Path projectRoot, Path flutterSdk)
            throws Exception {
        Path pubCache = configuredPubCache();
        ObjectNode config = JSON.createObjectNode();
        config.put("configVersion", 2);
        ArrayNode packages = config.putArray("packages");
        addPackage(packages, "strict_casts_probe", projectRoot, "3.10");
        addPackage(packages, "flutter", flutterSdk.resolve("packages/flutter"),
                "3.10");
        addPackage(packages, "sky_engine",
                flutterSdk.resolve("bin/cache/pkg/sky_engine"), "3.10");
        for (Map.Entry<String, String> entry
                : FLUTTER_3448_HOSTED_PACKAGES.entrySet()) {
            Path root = pubCache.resolve("hosted/pub.dev")
                    .resolve(entry.getKey() + '-' + entry.getValue());
            assumeTrue(Files.isDirectory(root),
                    "Flutter 3.44.8 dependency is absent: " + root);
            addPackage(packages, entry.getKey(), root, "3.4");
        }
        Path dartTool = Files.createDirectories(projectRoot.resolve(".dart_tool"));
        Files.writeString(
                dartTool.resolve("package_config.json"),
                JSON.writerWithDefaultPrettyPrinter().writeValueAsString(config),
                StandardCharsets.UTF_8);
        Files.writeString(projectRoot.resolve("pubspec.yaml"), """
                name: strict_casts_probe
                environment:
                  sdk: ^3.10.0
                dependencies:
                  flutter:
                    sdk: flutter
                """, StandardCharsets.UTF_8);
    }

    private static void addPackage(
            ArrayNode packages,
            String name,
            Path root,
            String languageVersion) {
        assumeTrue(Files.isDirectory(root), "Dart package root is absent: " + root);
        ObjectNode value = packages.addObject();
        value.put("name", name);
        value.put("rootUri", root.toAbsolutePath().normalize().toUri().toString());
        value.put("packageUri", "lib/");
        value.put("languageVersion", languageVersion);
    }

    private static Path configuredDartExecutable() {
        String configured = System.getProperty("dart.executable", "").trim();
        assumeTrue(!configured.isEmpty(), "set -Ddart.executable=<path-to-dart>");
        Path executable = Path.of(configured).toAbsolutePath().normalize();
        assumeTrue(Files.isRegularFile(executable),
                "Dart executable does not exist: " + executable);
        return executable;
    }

    private static Path configuredFlutter3448Sdk() throws Exception {
        String configured = System.getProperty("flutter.sdk", "").trim();
        assumeTrue(!configured.isEmpty(), "set -Dflutter.sdk=<path-to-flutter-3.44.8>");
        Path sdk = Path.of(configured).toAbsolutePath().normalize();
        Path version = sdk.resolve("bin/cache/flutter.version.json");
        assumeTrue(Files.isRegularFile(version),
                "Flutter version metadata does not exist: " + version);
        assertEquals("3.44.8",
                JSON.readTree(Files.readAllBytes(version))
                        .path("flutterVersion").asText());
        return sdk;
    }

    private static Path configuredPubCache() {
        String explicit = System.getProperty("pub.cache", "").trim();
        if (!explicit.isEmpty()) {
            Path value = Path.of(explicit).toAbsolutePath().normalize();
            assumeTrue(Files.isDirectory(value),
                    "Configured pub cache is absent: " + value);
            return value;
        }
        String environment = System.getenv("PUB_CACHE");
        if (environment != null && !environment.isBlank()) {
            Path value = Path.of(environment).toAbsolutePath().normalize();
            assumeTrue(Files.isDirectory(value), "PUB_CACHE is absent: " + value);
            return value;
        }
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            Path value = Path.of(localAppData, "Pub", "Cache")
                    .toAbsolutePath().normalize();
            if (Files.isDirectory(value)) {
                return value;
            }
        }
        Path value = Path.of(System.getProperty("user.home"), ".pub-cache")
                .toAbsolutePath().normalize();
        assumeTrue(Files.isDirectory(value), "Default pub cache is absent: " + value);
        return value;
    }
}
