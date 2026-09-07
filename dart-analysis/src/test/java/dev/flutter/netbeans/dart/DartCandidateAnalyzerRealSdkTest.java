package dev.flutter.netbeans.dart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Optional no-write candidate-overlay smoke test against an explicit real SDK. */
class DartCandidateAnalyzerRealSdkTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Map<String, String> FLUTTER_3448_HOSTED_PACKAGES =
            new LinkedHashMap<>(Map.of(
                    "characters", "1.4.1",
                    "collection", "1.19.1",
                    "material_color_utilities", "0.13.0",
                    "meta", "1.18.0",
                    "vector_math", "2.2.0"));

    @TempDir
    Path workspace;

    @Test
    void validatesErrorsAndNavigationWithoutChangingDisk() throws Exception {
        Path executable = configuredDartExecutable();
        Path sdkLib = executable.getParent().getParent().resolve("lib").normalize();
        assumeTrue(Files.isDirectory(sdkLib), "Dart SDK lib directory does not exist");
        Path lib = Files.createDirectories(workspace.resolve("lib"));
        Path file = lib.resolve("main.dart");
        String disk = "void main() {}\n";
        Files.writeString(file, disk, StandardCharsets.UTF_8);
        List<String> stderr = new ArrayList<>();
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(
                executable,
                line -> {
                    synchronized (stderr) {
                        stderr.add(line);
                    }
                });

        String broken = "void main() {\n  final value = ;\n}\n";
        DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(
                file, broken, 1, List.of())));

        assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(),
                () -> rejected + " stderr=" + stderr);
        assertTrue(rejected.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.severity() == DartCandidateDiagnosticSeverity.ERROR
                && diagnostic.blocking()));
        assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));

        String valid = "void main() {\n  print('overlay');\n}\n";
        int printOffset = valid.indexOf("print");
        DartSymbolProbe print = new DartSymbolProbe(
                "dart.print",
                printOffset,
                5,
                "print",
                "dart:core",
                sdkLib,
                Optional.of("FUNCTION"));
        DartCandidateAnalysisResult passed = await(analyzer.analyze(request(
                file, valid, 2, List.of(print))));

        assertEquals(DartCandidateAnalysisStatus.PASSED, passed.status(),
                () -> passed + " stderr=" + stderr);
        assertFalse(passed.symbolEvidence().isEmpty());
        assertTrue(passed.symbolEvidence().getFirst().accepted());
        assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void validatesProjectClipRRectReferencesAndRejectsInvalidClippers()
            throws Exception {
        Path executable = configuredDartExecutable();
        Path flutterSdk = configuredFlutter3448Sdk();
        Path projectRoot = Files.createDirectories(workspace.resolve("project"));
        Path dependencyRoot = Files.createDirectories(
                workspace.resolve("clipper_dependency"));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        Path dependencyLibrary = Files.createDirectories(
                dependencyRoot.resolve("lib"));
        writeFlutterPackageConfig(projectRoot, dependencyRoot, flutterSdk);
        Path importedLibrary = dependencyLibrary.resolve("clippers.dart");
        Files.writeString(importedLibrary, """
                import 'package:flutter/widgets.dart';

                class ImportedRRectClipper extends CustomClipper<RRect> {
                  const ImportedRRectClipper.compact();

                  @override
                  RRect getClip(Size size) => RRect.zero;

                  @override
                  bool shouldReclip(covariant ImportedRRectClipper oldClipper) => false;
                }
                """, StandardCharsets.UTF_8);
        Path file = lib.resolve("main.dart");
        String disk = """
                import 'package:flutter/widgets.dart';

                Widget diskVersion() => const SizedBox.shrink();
                """;
        Files.writeString(file, disk, StandardCharsets.UTF_8);
        List<String> stderr = new ArrayList<>();
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(
                executable,
                line -> {
                    synchronized (stderr) {
                        stderr.add(line);
                    }
                });

        String valid = clipperCandidate("""
                Widget buildCurrent() => ClipRRect(clipper: currentClipper);
                Widget buildRect() => ClipRect(clipper: currentRectClipper);
                Widget buildGenericCtor() => ClipRRect(
                  clipper: GenericCtorClipper(),
                );
                Widget buildGenericFactory() => const ClipRRect(
                  clipper: GenericFactoryClipper(),
                );
                Widget buildImported() => const ClipRRect(
                  clipper: const project_clippers.ImportedRRectClipper.compact(),
                );
                """);
        int currentOffset = valid.indexOf("currentClipper", valid.indexOf("clipper:"));
        int importedOffset = valid.indexOf(
                "ImportedRRectClipper", valid.indexOf("project_clippers."));
        int memberOffset = valid.indexOf("compact", importedOffset);
        int rectOffset = valid.indexOf("currentRectClipper", valid.indexOf("ClipRect"));
        String genericCtorExpression = "GenericCtorClipper()";
        int genericCtorOffset = valid.indexOf(
                genericCtorExpression, valid.indexOf("buildGenericCtor"));
        String genericFactoryExpression = "GenericFactoryClipper()";
        int genericFactoryOffset = valid.indexOf(
                genericFactoryExpression, valid.indexOf("buildGenericFactory"));
        String importedExpression =
                "const project_clippers.ImportedRRectClipper.compact()";
        int importedExpressionOffset = valid.indexOf(importedExpression);
        List<DartSymbolProbe> validProbes = List.of(
                typedProbe("current-reference", currentOffset, "currentClipper",
                        "project:current", lib,
                        currentOffset, "currentClipper".length(),
                        valid, "CustomClipper<RRect>"),
                typedProbe("current-rect-reference", rectOffset,
                        "currentRectClipper", "project:current", lib,
                        rectOffset, "currentRectClipper".length(),
                        valid, "CustomClipper<Rect>"),
                typedProbe("generic-ctor", genericCtorOffset,
                        "GenericCtorClipper", "project:current", lib,
                        genericCtorOffset, genericCtorExpression.length(),
                        valid, "CustomClipper<RRect>"),
                typedProbe("generic-factory", genericFactoryOffset,
                        "GenericFactoryClipper", "project:current", lib,
                        genericFactoryOffset, genericFactoryExpression.length(),
                        valid, "CustomClipper<RRect>"),
                probe("imported-root", importedOffset, "ImportedRRectClipper",
                        "package:clipper_dependency/clippers.dart",
                        dependencyLibrary),
                typedProbe("imported-member", memberOffset, "compact",
                        "package:clipper_dependency/clippers.dart",
                        dependencyLibrary,
                        importedExpressionOffset, importedExpression.length(),
                        valid, "CustomClipper<RRect>"));

        DartCandidateAnalysisResult passed = await(analyzer.analyze(request(
                projectRoot, file, valid, 10, validProbes)));

        assertEquals(DartCandidateAnalysisStatus.PASSED, passed.status(),
                () -> passed + " stderr=" + stderr);
        assertEquals(6, passed.symbolEvidence().size());
        assertTrue(passed.symbolEvidence().stream()
                .allMatch(DartSymbolEvidence::accepted));
        assertEquals(List.of(file.toRealPath(), file.toRealPath(),
                        file.toRealPath(), file.toRealPath(),
                        importedLibrary.toRealPath(),
                        importedLibrary.toRealPath()),
                passed.symbolEvidence().stream()
                        .map(evidence -> evidence.targets().getFirst().file())
                        .toList());
        assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));

        String validWithUnrelatedIgnore = "// ignore_for_file: unused_element\n"
                + clipperCandidate(
                        "Widget buildIgnoredButValid() => "
                        + "ClipRRect(clipper: currentClipper);\n");
        int ignoredValidOffset = validWithUnrelatedIgnore.indexOf(
                "currentClipper", validWithUnrelatedIgnore.indexOf("clipper:"));
        DartCandidateAnalysisResult ignoredButValid = await(analyzer.analyze(request(
                projectRoot,
                file,
                validWithUnrelatedIgnore,
                18,
                List.of(typedProbe(
                        "ignored-but-valid",
                        ignoredValidOffset,
                        "currentClipper",
                        "project:current",
                        lib,
                        ignoredValidOffset,
                        "currentClipper".length(),
                        validWithUnrelatedIgnore,
                        "CustomClipper<RRect>")))));
        assertEquals(DartCandidateAnalysisStatus.PASSED,
                ignoredButValid.status(),
                () -> ignoredButValid + " stderr=" + stderr);

        for (String expression : List.of(
                "dynamicClipper", "nullableClipper", "dynamicRectClipper")) {
            String invalid = clipperCandidate(
                    "Widget buildInvalid() => "
                    + (expression.endsWith("RectClipper")
                            ? "ClipRect" : "ClipRRect")
                    + "(clipper: " + expression + ");\n");
            int expressionOffset = invalid.indexOf(
                    expression, invalid.indexOf("clipper:"));
            String expectedType = expression.endsWith("RectClipper")
                    ? "CustomClipper<Rect>" : "CustomClipper<RRect>";
            DartSymbolProbe invalidProbe = typedProbe(
                    "invalid-" + expression,
                    expressionOffset,
                    expression,
                    "project:current",
                    lib,
                    expressionOffset,
                    expression.length(),
                    invalid,
                    expectedType);

            DartCandidateAnalysisResult staticRejected = await(analyzer.analyze(request(
                    projectRoot, file, invalid, 20 + expression.length(),
                    List.of(invalidProbe))));

            assertEquals(DartCandidateAnalysisStatus.REJECTED,
                    staticRejected.status(),
                    () -> expression + ": " + staticRejected + " stderr=" + stderr);
            assertTrue(staticRejected.diagnostics().stream()
                    .noneMatch(DartCandidateDiagnostic::blocking),
                    () -> expression + " must reach the proof overlay: "
                    + staticRejected.diagnostics());
            DartSymbolEvidence invalidEvidence = staticRejected
                    .symbolEvidence().getFirst();
            assertTrue(invalidEvidence.targets().size() == 1
                    && invalidEvidence.staticTypeEvidence().isPresent());
            assertFalse(invalidEvidence.staticTypeEvidence()
                    .orElseThrow().accepted());
        }

        String sealedWitnessOnly = clipperCandidate("""
                bool sealedWitnessOnly() => switch (sealedFamilyClipper) {
                  CustomClipper<RRect>() => true,
                };
                """);
        DartCandidateAnalysisResult witnessAlone = await(analyzer.analyze(request(
                projectRoot, file, sealedWitnessOnly, 30, List.of())));
        assertEquals(DartCandidateAnalysisStatus.PASSED, witnessAlone.status(),
                () -> witnessAlone + " stderr=" + stderr);

        List<String> invalidExpressions = List.of(
                "const NotAClipper()",
                "MissingClipper",
                "const RequiredRRectClipper()",
                "sealedFamilyClipper");
        long version = 11;
        for (String expression : invalidExpressions) {
            String invalid = clipperCandidate(
                    "Widget buildInvalid() => ClipRRect(clipper: "
                    + expression + ");\n");
            DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(
                    projectRoot, file, invalid, version++, List.of())));

            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(),
                    () -> expression + ": " + rejected + " stderr=" + stderr);
            assertTrue(rejected.diagnostics().stream().anyMatch(diagnostic ->
                    diagnostic.severity() == DartCandidateDiagnosticSeverity.ERROR
                    && diagnostic.blocking()),
                    () -> expression + ": " + rejected.diagnostics());
            assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        }

        String sourceSuppressed = "// ignore_for_file: "
                + "argument_type_not_assignable, invalid_assignment\n"
                + clipperCandidate(
                        "Widget buildSuppressed() => "
                        + "ClipRRect(clipper: dynamicClipper);\n");
        assertSuppressedStaticProofIsRejected(
                analyzer,
                projectRoot,
                file,
                lib,
                sourceSuppressed,
                "dynamicClipper",
                80,
                stderr);

        String requiredSuppressed = "// ignore_for_file: "
                + "not_enough_positional_arguments\n"
                + clipperCandidate(
                        "Widget buildSuppressed() => ClipRRect("
                        + "clipper: const RequiredRRectClipper(),\n"
                        + ");\n");
        assertSuppressedStaticProofIsRejected(
                analyzer,
                projectRoot,
                file,
                lib,
                requiredSuppressed,
                "const RequiredRRectClipper()",
                82,
                stderr);

        String invalidConstSuppressed = "// ignore_for_file: const_with_non_const\n"
                + clipperCandidate(
                        "Widget buildSuppressed() => ClipRRect("
                        + "clipper: const NonConstRRectClipper(),\n"
                        + ");\n");
        assertSuppressedStaticProofIsRejected(
                analyzer,
                projectRoot,
                file,
                lib,
                invalidConstSuppressed,
                "const NonConstRRectClipper()",
                83,
                stderr);

        String hostileOptions = """
                analyzer:
                  errors:
                    argument_type_not_assignable: ignore
                    invalid_assignment: ignore
                """;
        Path hostileOptionsFile = projectRoot.resolve("analysis_options.yaml");
        Files.writeString(hostileOptionsFile, hostileOptions,
                StandardCharsets.UTF_8);
        String optionsSuppressed = clipperCandidate(
                "Widget buildSuppressed() => "
                + "ClipRRect(clipper: dynamicClipper);\n");
        assertSuppressedStaticProofIsRejected(
                analyzer,
                projectRoot,
                file,
                lib,
                optionsSuppressed,
                "dynamicClipper",
                81,
                stderr);
        assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        assertEquals(hostileOptions,
                Files.readString(hostileOptionsFile, StandardCharsets.UTF_8));
        assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
    }

    @Test
    void validatesClipPathAndShapeHelperWithExactNonNullTypeProofs()
            throws Exception {
        Path executable = configuredDartExecutable();
        Path flutterSdk = configuredFlutter3448Sdk();
        Path projectRoot = Files.createDirectories(workspace.resolve("path_project"));
        Path dependencyRoot = Files.createDirectories(workspace.resolve("clipper_dependency"));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        Path dependencyLib = Files.createDirectories(dependencyRoot.resolve("lib"));
        writeFlutterPackageConfig(projectRoot, dependencyRoot, flutterSdk);
        Files.writeString(dependencyLib.resolve("clippers.dart"), """
                import 'package:flutter/widgets.dart';
                class ImportedPathClipper extends CustomClipper<Path> {
                  ImportedPathClipper.configured();
                  @override
                  Path getClip(Size size) => Path()..addRect(Offset.zero & size);
                  @override
                  bool shouldReclip(covariant ImportedPathClipper oldClipper) => false;
                }
                ShapeBorder configuredShape() => RoundedRectangleBorder(
                  borderRadius: BorderRadiusDirectional.only(topStart: Radius.circular(12)),
                );
                """, StandardCharsets.UTF_8);
        Path file = lib.resolve("main.dart");
        String disk = "void main() {}\n";
        Files.writeString(file, disk, StandardCharsets.UTF_8);
        List<String> stderr = new ArrayList<>();
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(executable,
                line -> { synchronized (stderr) { stderr.add(line); } });

        String valid = clipPathCandidate("""
                Widget defaultClip() => const ClipPath(child: SizedBox(width: 40, height: 30));
                Widget customClip() => ClipPath(clipper: currentPathClipper);
                Widget genericClip() => const ClipPath(clipper: GenericCtorClipper());
                Widget importedClip() => ClipPath(
                  clipper: project_clippers.ImportedPathClipper.configured(),
                );
                Widget projectShape() => ClipPath.shape(shape: currentShape);
                Widget builtInShape() => ClipPath.shape(shape: const CircleBorder());
                Widget configuredShape() => ClipPath.shape(
                  shape: project_clippers.configuredShape(), clipBehavior: Clip.hardEdge,
                  child: const SizedBox(width: 40, height: 30),
                );
                Widget nonConstAncestor() => Column(children: <Widget>[
                  ClipPath.shape(shape: const StadiumBorder()),
                ]);
                """);
        ArrayList<DartSymbolProbe> probes = new ArrayList<>();
        for (String[] spec : List.of(
                new String[]{"currentPathClipper", "currentPathClipper", "CustomClipper<Path>"},
                new String[]{"GenericCtorClipper()", "GenericCtorClipper", "CustomClipper<Path>"},
                new String[]{"currentShape", "currentShape", "ShapeBorder"})) {
            int offset = valid.lastIndexOf(spec[0]);
            probes.add(typedProbe(spec[1], offset, spec[1], "project:current", lib,
                    offset, spec[0].length(), valid, spec[2]));
        }
        for (String[] spec : List.of(
                new String[]{"project_clippers.ImportedPathClipper.configured()", "configured", "CustomClipper<Path>"},
                new String[]{"project_clippers.configuredShape()", "configuredShape", "ShapeBorder"})) {
            int offset = valid.lastIndexOf(spec[0]);
            probes.add(typedProbe("imported-" + spec[1], valid.indexOf(spec[1], offset),
                    spec[1], "package:clipper_dependency/clippers.dart", dependencyLib,
                    offset, spec[0].length(), valid, spec[2]));
        }
        Path flutterLib = flutterSdk.resolve("packages/flutter/lib");
        int circleOffset = valid.indexOf("const CircleBorder()");
        probes.add(typedProbe("sdk-shape", circleOffset + 6, "CircleBorder",
                "package:flutter/widgets.dart", flutterLib,
                circleOffset, "const CircleBorder()".length(), valid, "ShapeBorder"));
        int helperOffset = valid.indexOf("ClipPath.shape") + "ClipPath.".length();
        probes.add(probe("static-shape-helper", helperOffset, "shape",
                "package:flutter/widgets.dart", flutterLib));
        DartCandidateAnalysisResult passed = await(analyzer.analyze(request(
                projectRoot, file, valid, 100, probes)));
        assertEquals(DartCandidateAnalysisStatus.PASSED, passed.status(),
                () -> passed + " stderr=" + stderr);
        assertEquals(probes.size(), passed.symbolEvidence().size());
        assertTrue(passed.symbolEvidence().stream().allMatch(DartSymbolEvidence::accepted));
        assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));

        // Even source diagnostic suppression must not turn dynamic/nullable/wrong
        // object types into an accepted Designer property.
        long version = 101;
        for (String[] spec : List.of(
                new String[]{"dynamicPathClipper", "CustomClipper<Path>", "clipper"},
                new String[]{"nullablePathClipper", "CustomClipper<Path>", "clipper"},
                new String[]{"currentRectClipper", "CustomClipper<Path>", "clipper"},
                new String[]{"dynamicShape", "ShapeBorder", "shape"},
                new String[]{"nullableShape", "ShapeBorder", "shape"},
                new String[]{"currentPathClipper", "ShapeBorder", "shape"})) {
            String candidate = "// ignore_for_file: argument_type_not_assignable, invalid_assignment\n"
                    + clipPathCandidate("Widget invalid() => ClipPath"
                            + (spec[2].equals("shape") ? ".shape" : "")
                            + "(" + spec[2] + ": " + spec[0] + ");\n");
            int offset = candidate.lastIndexOf(spec[0]);
            DartSymbolProbe typeProbe = typedProbe("invalid-" + spec[0], offset,
                    spec[0], "project:current", lib, offset, spec[0].length(),
                    candidate, spec[1]);
            DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(
                    projectRoot, file, candidate, version++, List.of(typeProbe))));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(),
                    () -> spec[0] + ": " + rejected + " stderr=" + stderr);
            assertFalse(rejected.symbolEvidence().getFirst()
                    .staticTypeEvidence().orElseThrow().accepted());
            assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        }
        String invalidConst = clipPathCandidate(
                "Widget invalid() => const ClipPath.shape(shape: CircleBorder());\n");
        DartCandidateAnalysisResult constRejected = await(analyzer.analyze(request(
                projectRoot, file, invalidConst, version, List.of())));
        assertEquals(DartCandidateAnalysisStatus.REJECTED, constRejected.status());
        assertTrue(constRejected.diagnostics().stream().anyMatch(DartCandidateDiagnostic::blocking));
        assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
    }

    @Test
    void validatesClipRSuperellipseReferencesAndRejectsWrongGenericTypes()
            throws Exception {
        Path executable = configuredDartExecutable();
        Path flutterSdk = configuredFlutter3448Sdk();
        Path projectRoot = Files.createDirectories(workspace.resolve("superellipse_project"));
        Path dependencyRoot = Files.createDirectories(workspace.resolve("clipper_dependency"));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        Path dependencyLib = Files.createDirectories(dependencyRoot.resolve("lib"));
        writeFlutterPackageConfig(projectRoot, dependencyRoot, flutterSdk);
        Files.writeString(dependencyLib.resolve("clippers.dart"), """
                import 'package:flutter/widgets.dart';
                class ImportedSuperellipseClipper extends CustomClipper<RSuperellipse> {
                  const ImportedSuperellipseClipper.compact();
                  ImportedSuperellipseClipper.configured();
                  @override
                  RSuperellipse getClip(Size size) => throw UnimplementedError();
                  @override
                  bool shouldReclip(covariant ImportedSuperellipseClipper oldClipper) => false;
                }
                """, StandardCharsets.UTF_8);
        Path file = lib.resolve("main.dart");
        String disk = "void main() {}\n";
        Files.writeString(file, disk, StandardCharsets.UTF_8);
        List<String> stderr = new ArrayList<>();
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(executable,
                line -> { synchronized (stderr) { stderr.add(line); } });
        String valid = superellipseCandidate("""
                Widget defaultClip() => const ClipRSuperellipse();
                Widget directional() => const ClipRSuperellipse(
                  borderRadius: BorderRadiusDirectional.only(
                    topStart: Radius.elliptical(12, 24), bottomEnd: Radius.circular(8)),
                  clipBehavior: Clip.antiAliasWithSaveLayer,
                  child: SizedBox(width: 80, height: 60),
                );
                Widget currentClip() => ClipRSuperellipse(clipper: currentSuperellipseClipper);
                Widget genericClip() => const ClipRSuperellipse(clipper: GenericCtorClipper());
                Widget genericFactory() => const ClipRSuperellipse(clipper: GenericFactoryClipper());
                Widget importedConstClip() => const ClipRSuperellipse(
                  clipper: const project_clippers.ImportedSuperellipseClipper.compact(),
                );
                Widget importedNonConstClip() => ClipRSuperellipse(
                  clipper: project_clippers.ImportedSuperellipseClipper.configured(),
                  borderRadius: const BorderRadius.all(Radius.circular(999)),
                );
                """);
        String expectedType = "CustomClipper<RSuperellipse>";
        ArrayList<DartSymbolProbe> probes = new ArrayList<>();
        for (String[] spec : List.of(
                new String[]{"currentSuperellipseClipper", "currentSuperellipseClipper"},
                new String[]{"GenericCtorClipper()", "GenericCtorClipper"},
                new String[]{"GenericFactoryClipper()", "GenericFactoryClipper"})) {
            int offset = valid.lastIndexOf(spec[0]);
            probes.add(typedProbe(spec[1], offset, spec[1], "project:current", lib,
                    offset, spec[0].length(), valid, expectedType));
        }
        for (String member : List.of("compact", "configured")) {
            String expression = (member.equals("compact") ? "const " : "")
                    + "project_clippers.ImportedSuperellipseClipper." + member + "()";
            int offset = valid.lastIndexOf(expression);
            probes.add(typedProbe("imported-" + member, valid.indexOf(member, offset),
                    member, "package:clipper_dependency/clippers.dart", dependencyLib,
                    offset, expression.length(), valid, expectedType));
        }
        int widgetOffset = valid.indexOf("ClipRSuperellipse");
        probes.add(probe("superellipse-widget", widgetOffset, "ClipRSuperellipse",
                "package:flutter/widgets.dart", flutterSdk.resolve("packages/flutter/lib")));
        DartCandidateAnalysisResult passed = await(analyzer.analyze(request(
                projectRoot, file, valid, 200, probes)));
        assertEquals(DartCandidateAnalysisStatus.PASSED, passed.status(),
                () -> passed + " stderr=" + stderr);
        assertEquals(probes.size(), passed.symbolEvidence().size());
        assertTrue(passed.symbolEvidence().stream().allMatch(DartSymbolEvidence::accepted));

        long version = 201;
        for (String expression : List.of("dynamicSuperellipseClipper",
                "nullableSuperellipseClipper", "currentClipper", "currentRectClipper")) {
            String candidate = "// ignore_for_file: argument_type_not_assignable, invalid_assignment\n"
                    + superellipseCandidate("Widget invalid() => ClipRSuperellipse(clipper: "
                            + expression + ");\n");
            int offset = candidate.lastIndexOf(expression);
            DartSymbolProbe typed = typedProbe("invalid-" + expression, offset, expression,
                    "project:current", lib, offset, expression.length(), candidate, expectedType);
            DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(
                    projectRoot, file, candidate, version++, List.of(typed))));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(),
                    () -> expression + ": " + rejected + " stderr=" + stderr);
            assertFalse(rejected.symbolEvidence().getFirst()
                    .staticTypeEvidence().orElseThrow().accepted());
            assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        }
        assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
    }

    @Test
    void validatesPhysicalModelSurfaceAndRejectsInvalidSdkArguments() throws Exception {
        Path executable = configuredDartExecutable();
        Path flutterSdk = configuredFlutter3448Sdk();
        Path projectRoot = Files.createDirectories(workspace.resolve("physical_model_project"));
        Path dependencyRoot = Files.createDirectories(workspace.resolve("clipper_dependency"));
        Files.createDirectories(dependencyRoot.resolve("lib"));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        writeFlutterPackageConfig(projectRoot, dependencyRoot, flutterSdk);
        Path file = lib.resolve("main.dart");
        String disk = "void main() {}\n";
        Files.writeString(file, disk, StandardCharsets.UTF_8);
        List<String> stderr = new ArrayList<>();
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(executable,
                line -> { synchronized (stderr) { stderr.add(line); } });
        StringBuilder candidate = new StringBuilder("""
                import 'package:flutter/material.dart';
                Widget defaults() => const PhysicalModel(color: Color(0xFF2196F3));
                Widget themed(BuildContext context) => Center(child: PhysicalModel(
                  color: Theme.of(context).colorScheme.surface,
                  shadowColor: Theme.of(context).colorScheme.shadow,
                  elevation: 6.5,
                  child: const SizedBox(width: 120, height: 80),
                ));
                """);
        for (String shape : List.of("rectangle", "circle")) {
            for (String clip : List.of("none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer")) {
                candidate.append("""
                        Widget %s_%s() => const Center(child: PhysicalModel(
                          shape: BoxShape.%s,
                          clipBehavior: Clip.%s,
                          borderRadius: BorderRadius.only(
                            topLeft: Radius.elliptical(180, 240),
                            topRight: Radius.circular(20),
                            bottomRight: Radius.elliptical(6, 18),
                            bottomLeft: Radius.circular(0)),
                          elevation: 8.25,
                          color: Color(0x882196F3),
                          shadowColor: Color(0x55000000),
                          child: SizedBox(width: 120, height: 80),
                        ));
                        """.formatted(shape, clip, shape, clip));
            }
        }
        String valid = candidate.toString();
        ArrayList<DartSymbolProbe> probes = new ArrayList<>();
        for (String symbol : List.of("PhysicalModel", "BoxShape", "Clip", "BorderRadius",
                "Radius", "Color", "Theme")) {
            boolean engineSymbol = List.of("Clip", "Radius", "Color").contains(symbol);
            var occurrence = java.util.regex.Pattern.compile("\\b" + symbol + "\\b")
                    .matcher(valid);
            assertTrue(occurrence.find(), symbol);
            probes.add(probe("physical-model-" + symbol, occurrence.start(), symbol,
                    engineSymbol ? "dart:ui" : "package:flutter/material.dart",
                    flutterSdk.resolve(engineSymbol
                            ? "bin/cache/pkg/sky_engine/lib" : "packages/flutter/lib")));
        }
        DartCandidateAnalysisResult passed = await(analyzer.analyze(request(
                projectRoot, file, valid, 300, probes)));
        assertEquals(DartCandidateAnalysisStatus.PASSED, passed.status(),
                () -> passed + " stderr=" + stderr);
        assertEquals(probes.size(), passed.symbolEvidence().size());
        assertTrue(passed.symbolEvidence().stream().allMatch(DartSymbolEvidence::accepted));
        assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));

        long version = 301;
        for (String invalid : List.of(
                "const PhysicalModel()",
                "const PhysicalModel(color: Color(0xFF2196F3), elevation: -1)",
                "const PhysicalModel(color: Color(0xFF2196F3), borderRadius: "
                        + "BorderRadiusDirectional.all(Radius.circular(8)))",
                "const PhysicalModel(color: Color(0xFF2196F3), shape: BoxShape.circle, "
                        + "borderRadius: BorderRadiusDirectional.all(Radius.circular(8)))",
                "const PhysicalModel(color: Theme.of(context).colorScheme.surface)")) {
            String content = "import 'package:flutter/material.dart';\n"
                    + "Widget invalid(BuildContext context) => " + invalid + ";\n";
            DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(
                    projectRoot, file, content, version++, List.of())));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(),
                    () -> invalid + ": " + rejected + " stderr=" + stderr);
            assertTrue(rejected.diagnostics().stream().anyMatch(diagnostic ->
                    diagnostic.blocking()
                    && diagnostic.severity() == DartCandidateDiagnosticSeverity.ERROR));
            assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        }
        assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
    }

    @Test
    void validatesPhysicalShapePresetsAndRequiredCustomClipperTypes() throws Exception {
        Path executable = configuredDartExecutable();
        Path flutterSdk = configuredFlutter3448Sdk();
        Path projectRoot = Files.createDirectories(workspace.resolve("physical_shape_project"));
        Path dependencyRoot = Files.createDirectories(workspace.resolve("clipper_dependency"));
        Path dependencyLib = Files.createDirectories(dependencyRoot.resolve("lib"));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        writeFlutterPackageConfig(projectRoot, dependencyRoot, flutterSdk);
        Files.writeString(dependencyLib.resolve("clippers.dart"), """
                import 'package:flutter/widgets.dart';
                class ImportedPathClipper extends CustomClipper<Path> {
                  const ImportedPathClipper.compact();
                  ImportedPathClipper.configured();
                  @override
                  Path getClip(Size size) => Path()..addRect(Offset.zero & size);
                  @override
                  bool shouldReclip(covariant ImportedPathClipper oldClipper) => false;
                }
                """, StandardCharsets.UTF_8);
        Path file = lib.resolve("main.dart");
        String disk = "void main() {}\n";
        Files.writeString(file, disk, StandardCharsets.UTF_8);
        List<String> stderr = new ArrayList<>();
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(executable,
                line -> { synchronized (stderr) { stderr.add(line); } });
        StringBuilder methods = new StringBuilder("""
                Widget defaults() => const PhysicalShape(
                  clipper: ShapeBorderClipper(shape: RoundedRectangleBorder()),
                  color: Color(0xFF2196F3),
                );
                Widget themed(BuildContext context) => PhysicalShape(
                  clipper: const ShapeBorderClipper(shape: StadiumBorder()),
                  color: Theme.of(context).colorScheme.surface,
                  shadowColor: Theme.of(context).colorScheme.shadow,
                  elevation: 6.5,
                  child: const SizedBox(width: 120, height: 80),
                );
                Widget current() => PhysicalShape(
                  clipper: currentPathClipper, color: const Color(0xFF2196F3));
                Widget genericConstructor() => const PhysicalShape(
                  clipper: GenericCtorClipper(), color: Color(0xFF2196F3));
                Widget genericFactory() => const PhysicalShape(
                  clipper: GenericFactoryClipper(), color: Color(0xFF2196F3));
                Widget importedConst() => const PhysicalShape(
                  clipper: const project_clippers.ImportedPathClipper.compact(),
                  color: Color(0xFF2196F3));
                Widget importedNonConst() => PhysicalShape(
                  clipper: project_clippers.ImportedPathClipper.configured(),
                  color: const Color(0xFF2196F3));
                """);
        for (String shape : List.of("RoundedRectangleBorder", "BeveledRectangleBorder",
                "ContinuousRectangleBorder", "RoundedSuperellipseBorder", "CircleBorder", "StadiumBorder")) {
            for (String clip : List.of("none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer")) {
                for (boolean directional : List.of(false, true)) {
                    String radius = shape.equals("CircleBorder") || shape.equals("StadiumBorder") ? ""
                            : directional
                                    ? "borderRadius: BorderRadiusDirectional.only(topStart: Radius.elliptical(180, 240), bottomEnd: Radius.circular(8))"
                                    : "borderRadius: BorderRadius.only(topLeft: Radius.elliptical(180, 240), bottomRight: Radius.circular(8))";
                    methods.append("""
                            Widget %s_%s_%s() => const PhysicalShape(
                              clipper: ShapeBorderClipper(shape: %s(%s), textDirection: TextDirection.rtl),
                              color: Color(0x882196F3), shadowColor: Color(0x55000000),
                              elevation: 8.25, clipBehavior: Clip.%s,
                              child: SizedBox(width: 120, height: 80),
                            );
                            """.formatted(shape, clip, directional, shape, radius, clip));
                }
            }
        }
        String valid = "import 'package:flutter/material.dart';\n" + clipPathCandidate(methods.toString());
        ArrayList<DartSymbolProbe> probes = new ArrayList<>();
        for (String symbol : List.of("PhysicalShape", "ShapeBorderClipper", "RoundedRectangleBorder",
                "BeveledRectangleBorder", "ContinuousRectangleBorder", "RoundedSuperellipseBorder",
                "CircleBorder", "StadiumBorder", "BorderRadius", "BorderRadiusDirectional",
                "Radius", "TextDirection", "Clip", "Color", "Theme")) {
            var occurrence = java.util.regex.Pattern.compile("\\b" + symbol + "\\b").matcher(valid);
            assertTrue(occurrence.find(), symbol);
            boolean engineSymbol = List.of("Radius", "TextDirection", "Clip", "Color").contains(symbol);
            probes.add(probe("physical-shape-" + symbol, occurrence.start(), symbol,
                    engineSymbol ? "dart:ui" : "package:flutter/material.dart",
                    flutterSdk.resolve(engineSymbol ? "bin/cache/pkg/sky_engine/lib" : "packages/flutter/lib")));
        }
        for (String expression : List.of("currentPathClipper", "GenericCtorClipper()", "GenericFactoryClipper()")) {
            int offset = valid.lastIndexOf(expression);
            String symbol = expression.replace("()", "");
            probes.add(typedProbe("current-" + symbol, offset, symbol, "project:current", lib,
                    offset, expression.length(), valid, "CustomClipper<Path>"));
        }
        for (String member : List.of("compact", "configured")) {
            String expression = (member.equals("compact") ? "const " : "")
                    + "project_clippers.ImportedPathClipper." + member + "()";
            int offset = valid.lastIndexOf(expression);
            probes.add(typedProbe("imported-" + member, valid.indexOf(member, offset), member,
                    "package:clipper_dependency/clippers.dart", dependencyLib,
                    offset, expression.length(), valid, "CustomClipper<Path>"));
        }
        DartCandidateAnalysisResult passed = await(analyzer.analyze(request(projectRoot, file, valid, 400, probes)));
        assertEquals(DartCandidateAnalysisStatus.PASSED, passed.status(), () -> passed + " stderr=" + stderr);
        assertEquals(probes.size(), passed.symbolEvidence().size());
        assertTrue(passed.symbolEvidence().stream().allMatch(DartSymbolEvidence::accepted));
        long version = 401;
        for (String expression : List.of("dynamicPathClipper", "nullablePathClipper", "currentClipper", "currentRectClipper")) {
            String content = "// ignore_for_file: argument_type_not_assignable, invalid_assignment\n"
                    + clipPathCandidate("Widget invalid() => PhysicalShape(clipper: " + expression
                            + ", color: const Color(0xFF2196F3));\n");
            int offset = content.lastIndexOf(expression);
            DartSymbolProbe typed = typedProbe("invalid-" + expression, offset, expression, "project:current", lib,
                    offset, expression.length(), content, "CustomClipper<Path>");
            DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(projectRoot, file, content, version++, List.of(typed))));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> expression + ": " + rejected);
            assertFalse(rejected.symbolEvidence().getFirst().staticTypeEvidence().orElseThrow().accepted());
            assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        }
        for (String invalid : List.of(
                "const PhysicalShape(color: Color(0xFF2196F3))",
                "const PhysicalShape(clipper: ShapeBorderClipper(shape: CircleBorder()))",
                "const PhysicalShape(clipper: ShapeBorderClipper(shape: CircleBorder()), color: Color(0xFF2196F3), elevation: -1)",
                "const PhysicalShape(clipper: ShapeBorderClipper(shape: CircleBorder()), color: Theme.of(context).colorScheme.surface)")) {
            String content = "import 'package:flutter/material.dart';\nWidget invalid(BuildContext context) => " + invalid + ";\n";
            DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(projectRoot, file, content, version++, List.of())));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> invalid + ": " + rejected);
            assertTrue(rejected.diagnostics().stream().anyMatch(diagnostic -> diagnostic.blocking()
                    && diagnostic.severity() == DartCandidateDiagnosticSeverity.ERROR));
            assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        }
        assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
    }

    @Test
    void validatesRepaintBoundaryCompleteConstructorAndKeyOnlyHelpers() throws Exception {
        Path executable = configuredDartExecutable();
        Path flutterSdk = configuredFlutter3448Sdk();
        Path projectRoot = Files.createDirectories(workspace.resolve("repaint_boundary_project"));
        Path dependencyRoot = Files.createDirectories(workspace.resolve("clipper_dependency"));
        Files.createDirectories(dependencyRoot.resolve("lib"));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        writeFlutterPackageConfig(projectRoot, dependencyRoot, flutterSdk);
        Path file = lib.resolve("main.dart");
        String disk = "void main() {}\n";
        Files.writeString(file, disk, StandardCharsets.UTF_8);
        List<String> stderr = new ArrayList<>();
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(executable,
                line -> { synchronized (stderr) { stderr.add(line); } });
        String valid = """
                import 'package:flutter/material.dart';
                Widget empty() => const RepaintBoundary();
                Widget emptyExplicit() => const RepaintBoundary(child: null);
                Widget populated() => const RepaintBoundary(
                  child: SizedBox(width: 120, height: 80, child: Text('Paint boundary')),
                );
                Widget nested() => const Center(child: RepaintBoundary(
                  child: RepaintBoundary(child: ColoredBox(color: Color(0x882196F3))),
                ));
                Widget themed(BuildContext context) => RepaintBoundary(
                  child: ColoredBox(color: Theme.of(context).colorScheme.surface),
                );
                Widget nonConstDescendant() => RepaintBoundary(child: Text(DateTime.now().toString()));
                // The helpers derive keys only; Designer uses its own stable identity.
                Widget helper() => RepaintBoundary.wrap(const Text('child'), 0);
                List<RepaintBoundary> helpers() => RepaintBoundary.wrapAll(
                  const <Widget>[Text('first'), Text('second')],
                );
                """;
        Path flutterLibrary = flutterSdk.resolve("packages/flutter/lib");
        List<DartSymbolProbe> probes = List.of(
                probe("repaint-boundary", valid.indexOf("RepaintBoundary"), "RepaintBoundary",
                        "package:flutter/widgets.dart", flutterLibrary),
                probe("repaint-boundary-wrap", valid.indexOf("RepaintBoundary.wrap(")
                                + "RepaintBoundary.".length(), "wrap",
                        "package:flutter/widgets.dart", flutterLibrary),
                probe("repaint-boundary-wrap-all", valid.indexOf("RepaintBoundary.wrapAll(")
                                + "RepaintBoundary.".length(), "wrapAll",
                        "package:flutter/widgets.dart", flutterLibrary));
        DartCandidateAnalysisResult passed = await(analyzer.analyze(request(
                projectRoot, file, valid, 500, probes)));
        assertEquals(DartCandidateAnalysisStatus.PASSED, passed.status(), () -> passed + " stderr=" + stderr);
        assertEquals(probes.size(), passed.symbolEvidence().size());
        assertTrue(passed.symbolEvidence().stream().allMatch(DartSymbolEvidence::accepted));
        assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        long version = 501;
        for (String invalid : List.of(
                "const RepaintBoundary(elevation: 1)",
                "const RepaintBoundary(childIndex: 0)",
                "const RepaintBoundary(child: 'not a widget')",
                "const RepaintBoundary(child: Text(DateTime.now().toString()))")) {
            String content = "import 'package:flutter/material.dart';\nWidget invalid() => " + invalid + ";\n";
            DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(
                    projectRoot, file, content, version++, List.of())));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> invalid + ": " + rejected);
            assertTrue(rejected.diagnostics().stream().anyMatch(diagnostic -> diagnostic.blocking()
                    && diagnostic.severity() == DartCandidateDiagnosticSeverity.ERROR));
            assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        }
        assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
    }

    @Test
    void validatesIgnorePointerCompleteConstructorIncludingDeprecatedSemantics() throws Exception {
        Path executable = configuredDartExecutable();
        Path flutterSdk = configuredFlutter3448Sdk();
        Path projectRoot = Files.createDirectories(workspace.resolve("ignore_pointer_project"));
        Path dependencyRoot = Files.createDirectories(workspace.resolve("clipper_dependency"));
        Files.createDirectories(dependencyRoot.resolve("lib"));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        writeFlutterPackageConfig(projectRoot, dependencyRoot, flutterSdk);
        Path file = lib.resolve("main.dart");
        String disk = "void main() {}\n";
        Files.writeString(file, disk, StandardCharsets.UTF_8);
        List<String> stderr = new ArrayList<>();
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(executable,
                line -> { synchronized (stderr) { stderr.add(line); } });
        String valid = """
                import 'package:flutter/material.dart';
                Widget empty() => const IgnorePointer();
                Widget nullableDefaults() => const IgnorePointer(ignoringSemantics: null, child: null);
                Widget nested() => const Center(child: IgnorePointer(
                  ignoring: false, child: IgnorePointer(child: Text('Nested pointer filter')),
                ));
                Widget themed(BuildContext context) => IgnorePointer(
                  child: ColoredBox(color: Theme.of(context).colorScheme.surface),
                );
                Widget nonConstDescendant() => IgnorePointer(child: Text(DateTime.now().toString()));
                List<Widget> completeBooleanMatrix() => const <Widget>[
                  IgnorePointer(ignoring: true, child: Text('default semantics')),
                  IgnorePointer(ignoring: false, child: Text('default semantics')),
                  IgnorePointer(ignoring: true, ignoringSemantics: false, child: Text('legacy retained')),
                  IgnorePointer(ignoring: false, ignoringSemantics: false, child: Text('legacy retained')),
                  IgnorePointer(ignoring: true, ignoringSemantics: true, child: Text('excluded')),
                  IgnorePointer(ignoring: false, ignoringSemantics: true, child: Text('excluded')),
                ];
                """;
        Path flutterLibrary = flutterSdk.resolve("packages/flutter/lib");
        List<DartSymbolProbe> probes = List.of(probe("ignore-pointer",
                valid.indexOf("IgnorePointer"), "IgnorePointer",
                "package:flutter/widgets.dart", flutterLibrary));
        DartCandidateAnalysisResult passed = await(analyzer.analyze(request(
                projectRoot, file, valid, 600, probes)));
        assertEquals(DartCandidateAnalysisStatus.PASSED, passed.status(), () -> passed + " stderr=" + stderr);
        assertEquals(probes.size(), passed.symbolEvidence().size());
        assertTrue(passed.symbolEvidence().stream().allMatch(DartSymbolEvidence::accepted));
        assertFalse(passed.diagnostics().stream().anyMatch(DartCandidateDiagnostic::blocking));
        assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        long version = 601;
        for (String invalid : List.of(
                "const IgnorePointer(ignoring: null)",
                "const IgnorePointer(ignoring: 'true')",
                "const IgnorePointer(ignoringSemantics: 1)",
                "const IgnorePointer(absorbing: true)",
                "const IgnorePointer(child: 'not a widget')",
                "const IgnorePointer(child: Text(DateTime.now().toString()))")) {
            String content = "import 'package:flutter/material.dart';\nWidget invalid() => " + invalid + ";\n";
            DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(
                    projectRoot, file, content, version++, List.of())));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> invalid + ": " + rejected);
            assertTrue(rejected.diagnostics().stream().anyMatch(diagnostic -> diagnostic.blocking()
                    && diagnostic.severity() == DartCandidateDiagnosticSeverity.ERROR));
            assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        }
        assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
    }

    @Test
    void validatesAbsorbPointerCompleteConstructorIncludingDeprecatedSemantics() throws Exception {
        Path executable = configuredDartExecutable();
        Path flutterSdk = configuredFlutter3448Sdk();
        Path projectRoot = Files.createDirectories(workspace.resolve("absorb_pointer_project"));
        Path dependencyRoot = Files.createDirectories(workspace.resolve("clipper_dependency"));
        Files.createDirectories(dependencyRoot.resolve("lib"));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        writeFlutterPackageConfig(projectRoot, dependencyRoot, flutterSdk);
        Path file = lib.resolve("main.dart");
        String disk = "void main() {}\n";
        Files.writeString(file, disk, StandardCharsets.UTF_8);
        List<String> stderr = new ArrayList<>();
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(executable,
                line -> { synchronized (stderr) { stderr.add(line); } });
        String valid = """
                import 'package:flutter/material.dart';
                Widget empty() => const AbsorbPointer();
                Widget nullableDefaults() => const AbsorbPointer(ignoringSemantics: null, child: null);
                Widget nested() => const Center(child: AbsorbPointer(
                  absorbing: false, child: AbsorbPointer(child: Text('Nested pointer filter')),
                ));
                Widget themed(BuildContext context) => AbsorbPointer(
                  child: ColoredBox(color: Theme.of(context).colorScheme.surface),
                );
                Widget nonConstDescendant() => AbsorbPointer(child: Text(DateTime.now().toString()));
                List<Widget> completeBooleanMatrix() => const <Widget>[
                  AbsorbPointer(absorbing: true, child: Text('default semantics')),
                  AbsorbPointer(absorbing: false, child: Text('default semantics')),
                  AbsorbPointer(absorbing: true, ignoringSemantics: false, child: Text('legacy retained')),
                  AbsorbPointer(absorbing: false, ignoringSemantics: false, child: Text('legacy retained')),
                  AbsorbPointer(absorbing: true, ignoringSemantics: true, child: Text('excluded')),
                  AbsorbPointer(absorbing: false, ignoringSemantics: true, child: Text('excluded')),
                ];
                """;
        Path flutterLibrary = flutterSdk.resolve("packages/flutter/lib");
        List<DartSymbolProbe> probes = List.of(probe("absorb-pointer",
                valid.indexOf("AbsorbPointer"), "AbsorbPointer",
                "package:flutter/widgets.dart", flutterLibrary));
        DartCandidateAnalysisResult passed = await(analyzer.analyze(request(
                projectRoot, file, valid, 700, probes)));
        assertEquals(DartCandidateAnalysisStatus.PASSED, passed.status(), () -> passed + " stderr=" + stderr);
        assertEquals(probes.size(), passed.symbolEvidence().size());
        assertTrue(passed.symbolEvidence().stream().allMatch(DartSymbolEvidence::accepted));
        assertFalse(passed.diagnostics().stream().anyMatch(DartCandidateDiagnostic::blocking));
        assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        long version = 701;
        for (String invalid : List.of(
                "const AbsorbPointer(absorbing: null)",
                "const AbsorbPointer(absorbing: 'true')",
                "const AbsorbPointer(ignoringSemantics: 1)",
                "const AbsorbPointer(ignoring: true)",
                "const AbsorbPointer(child: 'not a widget')",
                "const AbsorbPointer(child: Text(DateTime.now().toString()))")) {
            String content = "import 'package:flutter/material.dart';\nWidget invalid() => " + invalid + ";\n";
            DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(
                    projectRoot, file, content, version++, List.of())));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> invalid + ": " + rejected);
            assertTrue(rejected.diagnostics().stream().anyMatch(diagnostic -> diagnostic.blocking()
                    && diagnostic.severity() == DartCandidateDiagnosticSeverity.ERROR));
            assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        }
        assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
    }

    @Test
    void validatesBlockSemanticsCompleteConstructor() throws Exception {
        Path executable = configuredDartExecutable();
        Path flutterSdk = configuredFlutter3448Sdk();
        Path projectRoot = Files.createDirectories(workspace.resolve("block_semantics_project"));
        Path dependencyRoot = Files.createDirectories(workspace.resolve("clipper_dependency"));
        Files.createDirectories(dependencyRoot.resolve("lib"));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        writeFlutterPackageConfig(projectRoot, dependencyRoot, flutterSdk);
        Path file = lib.resolve("main.dart");
        String disk = "void main() {}\n";
        Files.writeString(file, disk, StandardCharsets.UTF_8);
        List<String> stderr = new ArrayList<>();
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(executable,
                line -> { synchronized (stderr) { stderr.add(line); } });
        String valid = """
                import 'package:flutter/material.dart';
                Widget empty() => const BlockSemantics();
                Widget nullableChild() => const BlockSemantics(child: null);
                Widget explicitDefault() => const BlockSemantics(blocking: true);
                Widget disabledEmpty() => const BlockSemantics(blocking: false);
                Widget nested() => const Center(child: BlockSemantics(
                  blocking: false, child: BlockSemantics(child: Text('Nested semantics')),
                ));
                Widget themed(BuildContext context) => BlockSemantics(
                  child: ColoredBox(color: Theme.of(context).colorScheme.surface),
                );
                Widget nonConstDescendant() => BlockSemantics(child: Text(DateTime.now().toString()));
                List<Widget> paintOrder() => const <Widget>[
                  Text('Before'),
                  BlockSemantics(blocking: true, child: Text('Retained child')),
                  BlockSemantics(blocking: false, child: Text('No blocking')),
                  Text('After'),
                ];
                """;
        Path flutterLibrary = flutterSdk.resolve("packages/flutter/lib");
        List<DartSymbolProbe> probes = List.of(probe("block-semantics",
                valid.indexOf("BlockSemantics"), "BlockSemantics",
                "package:flutter/widgets.dart", flutterLibrary));
        DartCandidateAnalysisResult passed = await(analyzer.analyze(request(
                projectRoot, file, valid, 800, probes)));
        assertEquals(DartCandidateAnalysisStatus.PASSED, passed.status(), () -> passed + " stderr=" + stderr);
        assertEquals(probes.size(), passed.symbolEvidence().size());
        assertTrue(passed.symbolEvidence().stream().allMatch(DartSymbolEvidence::accepted));
        assertFalse(passed.diagnostics().stream().anyMatch(DartCandidateDiagnostic::blocking));
        assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        long version = 801;
        for (String invalid : List.of(
                "const BlockSemantics(blocking: null)",
                "const BlockSemantics(blocking: 'true')",
                "const BlockSemantics(blocking: 1)",
                "const BlockSemantics(excluding: true)",
                "const BlockSemantics(absorbing: true)",
                "const BlockSemantics(child: 'not a widget')",
                "const BlockSemantics(child: Text(DateTime.now().toString()))")) {
            String content = "import 'package:flutter/material.dart';\nWidget invalid() => " + invalid + ";\n";
            DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(
                    projectRoot, file, content, version++, List.of())));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> invalid + ": " + rejected);
            assertTrue(rejected.diagnostics().stream().anyMatch(diagnostic -> diagnostic.blocking()
                    && diagnostic.severity() == DartCandidateDiagnosticSeverity.ERROR));
            assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        }
        assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
    }

    @Test
    void validatesMergeSemanticsCompleteStructuralConstructor() throws Exception {
        Path executable = configuredDartExecutable();
        Path flutterSdk = configuredFlutter3448Sdk();
        Path projectRoot = Files.createDirectories(workspace.resolve("merge_semantics_project"));
        Path dependencyRoot = Files.createDirectories(workspace.resolve("clipper_dependency"));
        Files.createDirectories(dependencyRoot.resolve("lib"));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        writeFlutterPackageConfig(projectRoot, dependencyRoot, flutterSdk);
        Path file = lib.resolve("main.dart");
        String disk = "void main() {}\n";
        Files.writeString(file, disk, StandardCharsets.UTF_8);
        List<String> stderr = new ArrayList<>();
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(executable,
                line -> { synchronized (stderr) { stderr.add(line); } });
        String valid = """
                import 'package:flutter/material.dart';
                Widget empty() => const MergeSemantics();
                Widget nullableChild() => const MergeSemantics(child: null);
                Widget nested() => const Center(child: MergeSemantics(
                  child: MergeSemantics(child: Row(children: [Text('First'), Text('Second')])),
                ));
                Widget themed(BuildContext context) => MergeSemantics(
                  child: ColoredBox(color: Theme.of(context).colorScheme.surface),
                );
                Widget nonConstDescendant() => MergeSemantics(child: Text(DateTime.now().toString()));
                Widget controls() => MergeSemantics(child: Row(children: [
                  Checkbox(value: true, onChanged: (value) {}),
                  const Text('Settings'),
                ]));
                Widget actions() => MergeSemantics(child: Row(children: [
                  ElevatedButton(onPressed: () {}, child: const Text('First action')),
                  ElevatedButton(onPressed: () {}, child: const Text('Second action')),
                ]));
                Widget editable() => const MergeSemantics(child: TextField());
                """;
        Path flutterLibrary = flutterSdk.resolve("packages/flutter/lib");
        List<DartSymbolProbe> probes = List.of(probe("merge-semantics",
                valid.indexOf("MergeSemantics"), "MergeSemantics",
                "package:flutter/widgets.dart", flutterLibrary));
        DartCandidateAnalysisResult passed = await(analyzer.analyze(request(
                projectRoot, file, valid, 900, probes)));
        assertEquals(DartCandidateAnalysisStatus.PASSED, passed.status(), () -> passed + " stderr=" + stderr);
        assertEquals(probes.size(), passed.symbolEvidence().size());
        assertTrue(passed.symbolEvidence().stream().allMatch(DartSymbolEvidence::accepted));
        assertFalse(passed.diagnostics().stream().anyMatch(DartCandidateDiagnostic::blocking));
        assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        long version = 901;
        for (String invalid : List.of(
                "const MergeSemantics(blocking: true)",
                "const MergeSemantics(excluding: true)",
                "const MergeSemantics(mergeAllDescendantsIntoThisNode: true)",
                "const MergeSemantics(children: <Widget>[])",
                "const MergeSemantics(child: 'not a widget')",
                "const MergeSemantics(child: Text(DateTime.now().toString()))")) {
            String content = "import 'package:flutter/material.dart';\nWidget invalid() => " + invalid + ";\n";
            DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(
                    projectRoot, file, content, version++, List.of())));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> invalid + ": " + rejected);
            assertTrue(rejected.diagnostics().stream().anyMatch(diagnostic -> diagnostic.blocking()
                    && diagnostic.severity() == DartCandidateDiagnosticSeverity.ERROR));
            assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        }
        assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
    }

    @Test
    void validatesIndexedSemanticsCompleteConstructor() throws Exception {
        Path executable = configuredDartExecutable();
        Path flutterSdk = configuredFlutter3448Sdk();
        Path projectRoot = Files.createDirectories(workspace.resolve("indexed_semantics_project"));
        Path dependencyRoot = Files.createDirectories(workspace.resolve("clipper_dependency"));
        Files.createDirectories(dependencyRoot.resolve("lib"));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        writeFlutterPackageConfig(projectRoot, dependencyRoot, flutterSdk);
        Path file = lib.resolve("main.dart");
        String disk = "void main() {}\n";
        Files.writeString(file, disk, StandardCharsets.UTF_8);
        List<String> stderr = new ArrayList<>();
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(executable,
                line -> { synchronized (stderr) { stderr.add(line); } });
        String valid = """
                import 'package:flutter/material.dart';
                Widget empty() => const IndexedSemantics(index: 0);
                Widget nullableChild() => const IndexedSemantics(index: 1, child: null);
                Widget negative() => const IndexedSemantics(index: -1, child: Text('Signed'));
                Widget lowerBoundary() => const IndexedSemantics(index: -9007199254740991);
                Widget upperBoundary() => const IndexedSemantics(index: 9007199254740991);
                Widget nested() => const IndexedSemantics(index: 4,
                  child: IndexedSemantics(index: 5, child: Text('Nested')));
                Widget themed(BuildContext context) => IndexedSemantics(index: 3,
                  child: ColoredBox(color: Theme.of(context).colorScheme.surface));
                Widget changing(int index) => IndexedSemantics(index: index,
                  child: Text(DateTime.now().toString()));
                Widget scrollable() => ListView(addSemanticIndexes: false,
                  semanticChildCount: 2, children: const [
                    IndexedSemantics(index: 0, child: Text('First')),
                    SizedBox(height: 12),
                    IndexedSemantics(index: 1, child: Text('Second')),
                  ]);
                Widget merged() => const MergeSemantics(child: IndexedSemantics(
                  index: 2, child: Row(children: [Text('Label'), Text('Value')])));
                """;
        Path flutterLibrary = flutterSdk.resolve("packages/flutter/lib");
        List<DartSymbolProbe> probes = List.of(probe("indexed-semantics",
                valid.indexOf("IndexedSemantics"), "IndexedSemantics",
                "package:flutter/widgets.dart", flutterLibrary));
        DartCandidateAnalysisResult passed = await(analyzer.analyze(request(
                projectRoot, file, valid, 1000, probes)));
        assertEquals(DartCandidateAnalysisStatus.PASSED, passed.status(), () -> passed + " stderr=" + stderr);
        assertEquals(probes.size(), passed.symbolEvidence().size());
        assertTrue(passed.symbolEvidence().stream().allMatch(DartSymbolEvidence::accepted));
        assertFalse(passed.diagnostics().stream().anyMatch(DartCandidateDiagnostic::blocking));
        assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        long version = 1001;
        for (String invalid : List.of(
                "const IndexedSemantics()",
                "const IndexedSemantics(index: null)",
                "const IndexedSemantics(index: 1.5)",
                "const IndexedSemantics(index: true)",
                "const IndexedSemantics(index: '1')",
                "const IndexedSemantics(index: 0, excluding: true)",
                "const IndexedSemantics(index: 0, children: <Widget>[])",
                "const IndexedSemantics(index: 0, child: 'not a widget')",
                "const IndexedSemantics(index: 0, child: Text(DateTime.now().toString()))")) {
            String content = "import 'package:flutter/material.dart';\nWidget invalid() => " + invalid + ";\n";
            DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(
                    projectRoot, file, content, version++, List.of())));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> invalid + ": " + rejected);
            assertTrue(rejected.diagnostics().stream().anyMatch(diagnostic -> diagnostic.blocking()
                    && diagnostic.severity() == DartCandidateDiagnosticSeverity.ERROR));
            assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        }
        assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
    }

    @Test
    void validatesExcludeFocusCompleteConstructor() throws Exception {
        Path executable = configuredDartExecutable();
        Path flutterSdk = configuredFlutter3448Sdk();
        Path projectRoot = Files.createDirectories(workspace.resolve("exclude_focus_project"));
        Path dependencyRoot = Files.createDirectories(workspace.resolve("clipper_dependency"));
        Files.createDirectories(dependencyRoot.resolve("lib"));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        writeFlutterPackageConfig(projectRoot, dependencyRoot, flutterSdk);
        Path file = lib.resolve("main.dart");
        String disk = "void main() {}\n";
        Files.writeString(file, disk, StandardCharsets.UTF_8);
        List<String> stderr = new ArrayList<>();
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(executable,
                line -> { synchronized (stderr) { stderr.add(line); } });
        String valid = """
                import 'package:flutter/material.dart';
                Widget omitted() => const ExcludeFocus(child: Text('Default true'));
                Widget enabled() => const ExcludeFocus(excluding: false, child: TextField());
                Widget excluded() => const ExcludeFocus(excluding: true, child: TextField());
                Widget emptyGeometry() => const ExcludeFocus(child: SizedBox.shrink());
                Widget nested() => const ExcludeFocus(excluding: false,
                  child: ExcludeFocus(child: TextField(autofocus: true)));
                Widget themed(BuildContext context) => ExcludeFocus(
                  child: ColoredBox(color: Theme.of(context).colorScheme.surface));
                Widget changing(bool excluding) => ExcludeFocus(excluding: excluding,
                  child: Text(DateTime.now().toString()));
                Widget controls() => ExcludeFocus(child: Row(children: [
                  ElevatedButton(onPressed: () {}, child: const Text('Action')),
                  const SizedBox(width: 100, child: TextField()),
                ]));
                Widget semantics() => const IndexedSemantics(index: 0,
                  child: MergeSemantics(child: ExcludeFocus(child: Text('Retained label'))));
                Widget scrolling() => ListView(children: const [
                  ExcludeFocus(excluding: false, child: Text('First')),
                  ExcludeFocus(child: Text('Second')),
                ]);
                """;
        Path flutterLibrary = flutterSdk.resolve("packages/flutter/lib");
        List<DartSymbolProbe> probes = List.of(probe("exclude-focus",
                valid.indexOf("ExcludeFocus"), "ExcludeFocus",
                "package:flutter/widgets.dart", flutterLibrary));
        DartCandidateAnalysisResult passed = await(analyzer.analyze(request(
                projectRoot, file, valid, 1100, probes)));
        assertEquals(DartCandidateAnalysisStatus.PASSED, passed.status(), () -> passed + " stderr=" + stderr);
        assertEquals(probes.size(), passed.symbolEvidence().size());
        assertTrue(passed.symbolEvidence().stream().allMatch(DartSymbolEvidence::accepted));
        assertFalse(passed.diagnostics().stream().anyMatch(DartCandidateDiagnostic::blocking));
        assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        long version = 1101;
        for (String invalid : List.of(
                "const ExcludeFocus()",
                "const ExcludeFocus(child: null)",
                "const ExcludeFocus(excluding: null, child: Text('Bad'))",
                "const ExcludeFocus(excluding: 1, child: Text('Bad'))",
                "const ExcludeFocus(excluding: 'true', child: Text('Bad'))",
                "const ExcludeFocus(descendantsAreFocusable: false, child: Text('Bad'))",
                "const ExcludeFocus(children: <Widget>[])",
                "const ExcludeFocus(child: 'not a widget')",
                "const ExcludeFocus(child: Text(DateTime.now().toString()))")) {
            String content = "import 'package:flutter/material.dart';\nWidget invalid() => " + invalid + ";\n";
            DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(
                    projectRoot, file, content, version++, List.of())));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> invalid + ": " + rejected);
            assertTrue(rejected.diagnostics().stream().anyMatch(diagnostic -> diagnostic.blocking()
                    && diagnostic.severity() == DartCandidateDiagnosticSeverity.ERROR));
            assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        }
        assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
    }

    @Test
    void validatesExcludeFocusTraversalCompleteConstructor() throws Exception {
        Path executable = configuredDartExecutable();
        Path flutterSdk = configuredFlutter3448Sdk();
        Path projectRoot = Files.createDirectories(workspace.resolve("exclude_focus_traversal_project"));
        Path dependencyRoot = Files.createDirectories(workspace.resolve("clipper_dependency"));
        Files.createDirectories(dependencyRoot.resolve("lib"));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        writeFlutterPackageConfig(projectRoot, dependencyRoot, flutterSdk);
        Path file = lib.resolve("main.dart");
        String disk = "void main() {}\n";
        Files.writeString(file, disk, StandardCharsets.UTF_8);
        List<String> stderr = new ArrayList<>();
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(executable,
                line -> { synchronized (stderr) { stderr.add(line); } });
        String valid = """
                import 'package:flutter/material.dart';
                Widget omitted() => const ExcludeFocusTraversal(child: Text('Default true'));
                Widget traversable() => const ExcludeFocusTraversal(excluding: false, child: TextField());
                Widget excluded() => const ExcludeFocusTraversal(excluding: true, child: TextField());
                Widget emptyGeometry() => const ExcludeFocusTraversal(child: SizedBox.shrink());
                Widget nested() => const ExcludeFocusTraversal(
                  child: ExcludeFocusTraversal(excluding: false, child: TextField(autofocus: true)));
                Widget focusEligibility() => const ExcludeFocus(excluding: false,
                  child: ExcludeFocusTraversal(child: TextField()));
                Widget ancestorFocusExclusion() => const ExcludeFocus(
                  child: ExcludeFocusTraversal(excluding: false, child: TextField()));
                Widget themed(BuildContext context) => ExcludeFocusTraversal(
                  child: ColoredBox(color: Theme.of(context).colorScheme.surface));
                Widget changing(bool excluding) => ExcludeFocusTraversal(excluding: excluding,
                  child: Text(DateTime.now().toString()));
                Widget controls(FocusNode node) => ExcludeFocusTraversal(child: Row(children: [
                  ElevatedButton(onPressed: node.requestFocus, child: const Text('Request focus')),
                  SizedBox(width: 100, child: TextField(focusNode: node)),
                ]));
                Widget semantics() => const IndexedSemantics(index: 0,
                  child: MergeSemantics(child: ExcludeFocusTraversal(child: Text('Retained label'))));
                Widget scrolling() => ListView(children: const [
                  ExcludeFocusTraversal(excluding: false, child: Text('First')),
                  ExcludeFocusTraversal(child: Text('Second')),
                ]);
                """;
        Path flutterLibrary = flutterSdk.resolve("packages/flutter/lib");
        List<DartSymbolProbe> probes = List.of(probe("exclude-focus-traversal",
                valid.indexOf("ExcludeFocusTraversal"), "ExcludeFocusTraversal",
                "package:flutter/widgets.dart", flutterLibrary));
        DartCandidateAnalysisResult passed = await(analyzer.analyze(request(
                projectRoot, file, valid, 1200, probes)));
        assertEquals(DartCandidateAnalysisStatus.PASSED, passed.status(), () -> passed + " stderr=" + stderr);
        assertEquals(probes.size(), passed.symbolEvidence().size());
        assertTrue(passed.symbolEvidence().stream().allMatch(DartSymbolEvidence::accepted));
        assertFalse(passed.diagnostics().stream().anyMatch(DartCandidateDiagnostic::blocking));
        assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        long version = 1201;
        for (String invalid : List.of(
                "const ExcludeFocusTraversal()",
                "const ExcludeFocusTraversal(child: null)",
                "const ExcludeFocusTraversal(excluding: null, child: Text('Bad'))",
                "const ExcludeFocusTraversal(excluding: 1, child: Text('Bad'))",
                "const ExcludeFocusTraversal(excluding: 'true', child: Text('Bad'))",
                "const ExcludeFocusTraversal(descendantsAreTraversable: false, child: Text('Bad'))",
                "const ExcludeFocusTraversal(descendantsAreFocusable: false, child: Text('Bad'))",
                "const ExcludeFocusTraversal(skipTraversal: true, child: Text('Bad'))",
                "const ExcludeFocusTraversal(children: <Widget>[])",
                "const ExcludeFocusTraversal(child: 'not a widget')",
                "const ExcludeFocusTraversal(child: Text(DateTime.now().toString()))")) {
            String content = "import 'package:flutter/material.dart';\nWidget invalid() => " + invalid + ";\n";
            DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(
                    projectRoot, file, content, version++, List.of())));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> invalid + ": " + rejected);
            assertTrue(rejected.diagnostics().stream().anyMatch(diagnostic -> diagnostic.blocking()
                    && diagnostic.severity() == DartCandidateDiagnosticSeverity.ERROR));
            assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        }
        assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
    }

    @Test
    void validatesVisibilityCompleteConstructorAndMaintainEquivalent() throws Exception {
        Path executable = configuredDartExecutable();
        Path flutterSdk = configuredFlutter3448Sdk();
        Path projectRoot = Files.createDirectories(workspace.resolve("visibility_project"));
        Path dependencyRoot = Files.createDirectories(workspace.resolve("clipper_dependency"));
        Files.createDirectories(dependencyRoot.resolve("lib"));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        writeFlutterPackageConfig(projectRoot, dependencyRoot, flutterSdk);
        Path file = lib.resolve("main.dart");
        String disk = "void main() {}\n";
        Files.writeString(file, disk, StandardCharsets.UTF_8);
        List<String> stderr = new ArrayList<>();
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(executable,
                line -> { synchronized (stderr) { stderr.add(line); } });
        StringBuilder valid = new StringBuilder("""
                import 'package:flutter/material.dart';
                Widget omitted() => const Visibility(child: Text('Default visible'));
                Widget replacement() => const Visibility(visible: false,
                  child: Text('Hidden'), replacement: Icon(Icons.info));
                Widget maintainNamed() => const Visibility.maintain(visible: false, child: TextField());
                Widget maintainEquivalent() => const Visibility(visible: false, maintainState: true,
                  maintainAnimation: true, maintainSize: true, maintainSemantics: true,
                  maintainInteractivity: true, maintainFocusability: true, child: TextField());
                Widget ignoredReplacement() => const Visibility(visible: false, maintainState: true,
                  replacement: Text('Unused'), child: Text('State retained'));
                Widget nested() => const Visibility(visible: false,
                  child: Visibility.maintain(child: Text('Nested')));
                Widget themed(BuildContext context) => Visibility(
                  child: Text('Theme', style: TextStyle(color: Theme.of(context).colorScheme.primary)));
                Widget changing(bool visible) => Visibility(visible: visible,
                  child: Text(DateTime.now().toString()));
                Widget focusAndSemantics() => const IndexedSemantics(index: 0,
                  child: ExcludeFocusTraversal(child: Visibility.maintain(child: TextField())));
                Widget scrolling() => ListView(children: const [
                  Visibility(child: Text('First')), Visibility(visible: false, child: Text('Second')),
                ]);
                List<Widget> completeBooleanMatrix() => const <Widget>[
                """);
        List<String> flags = List.of("maintainState", "maintainAnimation", "maintainSize",
                "maintainSemantics", "maintainInteractivity", "maintainFocusability");
        int validStates = 0;
        for (int mask = 0; mask < 64; mask++) {
            boolean state = (mask & 1) != 0, animation = (mask & 2) != 0, size = (mask & 4) != 0;
            boolean semantics = (mask & 8) != 0, interactivity = (mask & 16) != 0, focus = (mask & 32) != 0;
            if ((!state && (animation || focus)) || (!animation && size) || (!size && (semantics || interactivity))) {
                continue;
            }
            validStates++;
            for (boolean visible : List.of(false, true)) {
                valid.append("Visibility(child: Text('Matrix'), replacement: SizedBox.shrink(), visible: ")
                        .append(visible);
                for (int bit = 0; bit < flags.size(); bit++) {
                    valid.append(", ").append(flags.get(bit)).append(": ").append((mask & (1 << bit)) != 0);
                }
                valid.append("),\n");
            }
        }
        assertEquals(13, validStates);
        valid.append("];\n");
        String candidate = valid.toString();
        Path flutterLibrary = flutterSdk.resolve("packages/flutter/lib");
        List<DartSymbolProbe> probes = List.of(probe("visibility", candidate.indexOf("Visibility"),
                "Visibility", "package:flutter/widgets.dart", flutterLibrary));
        DartCandidateAnalysisResult passed = await(analyzer.analyze(request(projectRoot, file, candidate, 1300, probes)));
        assertEquals(DartCandidateAnalysisStatus.PASSED, passed.status(), () -> passed + " stderr=" + stderr);
        assertEquals(probes.size(), passed.symbolEvidence().size());
        assertTrue(passed.symbolEvidence().stream().allMatch(DartSymbolEvidence::accepted));
        assertFalse(passed.diagnostics().stream().anyMatch(DartCandidateDiagnostic::blocking));
        assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        long version = 1301;
        for (String invalid : List.of(
                "const Visibility()", "const Visibility(child: null)",
                "const Visibility(child: Text('Bad'), replacement: null)",
                "const Visibility(child: Text('Bad'), visible: null)",
                "const Visibility(child: Text('Bad'), maintainState: 'true')",
                "const Visibility(child: Text('Bad'), maintainAnimation: true)",
                "const Visibility(child: Text('Bad'), maintainState: true, maintainSize: true)",
                "const Visibility(child: Text('Bad'), maintainState: true, maintainAnimation: true, maintainSemantics: true)",
                "const Visibility(child: Text('Bad'), maintainState: true, maintainAnimation: true, maintainInteractivity: true)",
                "const Visibility(child: Text('Bad'), maintainFocusability: true)",
                "const Visibility(child: Text('Bad'), maintain: true)",
                "const Visibility(child: Text('Bad'), replacement: 'not a widget')",
                "const Visibility.maintain(child: Text('Bad'), replacement: Text('Invalid named field'))",
                "const Visibility(child: Text(DateTime.now().toString()))")) {
            String content = "import 'package:flutter/material.dart';\nWidget invalid() => " + invalid + ";\n";
            DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(projectRoot, file, content, version++, List.of())));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> invalid + ": " + rejected);
            assertTrue(rejected.diagnostics().stream().anyMatch(diagnostic -> diagnostic.blocking()
                    && diagnostic.severity() == DartCandidateDiagnosticSeverity.ERROR));
            assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        }
        assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
    }

    @Test
    void validatesTickerModeCompleteConstructorAndMergeBehavior() throws Exception {
        Path executable = configuredDartExecutable();
        Path flutterSdk = configuredFlutter3448Sdk();
        Path projectRoot = Files.createDirectories(workspace.resolve("ticker_mode_project"));
        Path dependencyRoot = Files.createDirectories(workspace.resolve("clipper_dependency"));
        Files.createDirectories(dependencyRoot.resolve("lib"));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        writeFlutterPackageConfig(projectRoot, dependencyRoot, flutterSdk);
        Path file = lib.resolve("main.dart");
        String disk = "void main() {}\n";
        Files.writeString(file, disk, StandardCharsets.UTF_8);
        List<String> stderr = new ArrayList<>();
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(executable,
                line -> { synchronized (stderr) { stderr.add(line); } });
        String candidate = """
                import 'package:flutter/material.dart';
                Widget enabledDefault() => const TickerMode(enabled: true, child: Text('Enabled'));
                Widget disabledDefault() => const TickerMode(enabled: false, child: Text('Muted'));
                List<Widget> matrix() => const <Widget>[
                  TickerMode(enabled: true, forceFrames: false, child: Text('Regular')),
                  TickerMode(enabled: true, forceFrames: true, child: Text('Forced')),
                  TickerMode(enabled: false, forceFrames: false, child: Text('Muted')),
                  TickerMode(enabled: false, forceFrames: true, child: Text('Muted forced request')),
                ];
                Widget nested() => const TickerMode(enabled: false, forceFrames: true,
                  child: TickerMode(enabled: true, forceFrames: false, child: TextField()));
                Widget visibility() => const Visibility(visible: false, maintainState: true,
                  child: TickerMode(enabled: true, forceFrames: true, child: TextField()));
                Widget inheritedMerge() => TickerMode.merge(child: const Text('Inherited'));
                Widget nullMerge() => TickerMode.merge(enabled: null, forceFrames: null,
                  child: const Text('Inherited'));
                Widget canonicalMergeBehavior() => const TickerMode(enabled: true, child: Text('Inherited'));
                Widget explicitMerge() => TickerMode.merge(enabled: false, forceFrames: true,
                  child: const Text('Explicit requests'));
                Widget themed(BuildContext context) => TickerMode(enabled: true,
                  child: Text('Theme', style: TextStyle(color: Theme.of(context).colorScheme.primary)));
                Widget changing(bool enabled, bool forceFrames) => TickerMode(enabled: enabled,
                  forceFrames: forceFrames, child: Text(DateTime.now().toString()));
                Widget semanticsAndFocus() => const IndexedSemantics(index: 0,
                  child: ExcludeFocusTraversal(child: TickerMode(enabled: false, child: TextField())));
                Widget scrolling() => ListView(children: const [
                  TickerMode(enabled: true, child: Text('First')),
                  TickerMode(enabled: false, child: Text('Second')),
                ]);
                """;
        Path flutterLibrary = flutterSdk.resolve("packages/flutter/lib");
        List<DartSymbolProbe> probes = List.of(
                probe("ticker-mode", candidate.indexOf("TickerMode"), "TickerMode",
                        "package:flutter/widgets.dart", flutterLibrary),
                probe("ticker-mode-merge", candidate.indexOf("TickerMode.merge") + "TickerMode.".length(),
                        "merge", "package:flutter/widgets.dart", flutterLibrary));
        DartCandidateAnalysisResult passed = await(analyzer.analyze(request(projectRoot, file, candidate, 1400, probes)));
        assertEquals(DartCandidateAnalysisStatus.PASSED, passed.status(), () -> passed + " stderr=" + stderr);
        assertEquals(probes.size(), passed.symbolEvidence().size());
        assertTrue(passed.symbolEvidence().stream().allMatch(DartSymbolEvidence::accepted));
        assertFalse(passed.diagnostics().stream().anyMatch(DartCandidateDiagnostic::blocking));
        assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        long version = 1401;
        for (String invalid : List.of(
                "const TickerMode()", "const TickerMode(child: Text('Missing enabled'))",
                "const TickerMode(enabled: true)", "const TickerMode(enabled: null, child: Text('Bad'))",
                "const TickerMode(enabled: 'true', child: Text('Bad'))",
                "const TickerMode(enabled: true, child: null)",
                "const TickerMode(enabled: true, child: 'not a widget')",
                "const TickerMode(enabled: true, forceFrames: null, child: Text('Bad'))",
                "const TickerMode(enabled: true, forceFrames: 1, child: Text('Bad'))",
                "const TickerMode(enabled: true, paused: true, child: Text('Bad'))",
                "const TickerMode(enabled: true, child: Text(DateTime.now().toString()))")) {
            String content = "import 'package:flutter/material.dart';\nWidget invalid() => " + invalid + ";\n";
            DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(projectRoot, file, content, version++, List.of())));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> invalid + ": " + rejected);
            assertTrue(rejected.diagnostics().stream().anyMatch(diagnostic -> diagnostic.blocking()
                    && diagnostic.severity() == DartCandidateDiagnosticSeverity.ERROR));
            assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        }
        assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
    }

    @Test
    void validatesDefaultTextHeightBehaviorCompleteCompositeAndInheritance() throws Exception {
        Path executable = configuredDartExecutable();
        Path flutterSdk = configuredFlutter3448Sdk();
        Path projectRoot = Files.createDirectories(workspace.resolve("default_text_height_behavior_project"));
        Path dependencyRoot = Files.createDirectories(workspace.resolve("clipper_dependency"));
        Files.createDirectories(dependencyRoot.resolve("lib"));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        writeFlutterPackageConfig(projectRoot, dependencyRoot, flutterSdk);
        Path file = lib.resolve("main.dart");
        String disk = "void main() {}\n";
        Files.writeString(file, disk, StandardCharsets.UTF_8);
        List<String> stderr = new ArrayList<>();
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(executable,
                line -> { synchronized (stderr) { stderr.add(line); } });
        StringBuilder valid = new StringBuilder("""
                import 'package:flutter/material.dart';
                Widget defaultComposite() => const DefaultTextHeightBehavior(
                  textHeightBehavior: TextHeightBehavior(), child: Text('Required default'));
                Widget nestedReset() => const DefaultTextHeightBehavior(
                  textHeightBehavior: TextHeightBehavior(applyHeightToFirstAscent: false,
                    applyHeightToLastDescent: false, leadingDistribution: TextLeadingDistribution.even),
                  child: DefaultTextHeightBehavior(textHeightBehavior: TextHeightBehavior(),
                    child: Text('Nearest defaults', style: TextStyle(height: 2))));
                Widget localText() => const DefaultTextHeightBehavior(
                  textHeightBehavior: TextHeightBehavior(applyHeightToFirstAscent: false),
                  child: Text('Local wins', textHeightBehavior: TextHeightBehavior(),
                    style: TextStyle(height: 2)));
                Widget defaultStyle() => const DefaultTextHeightBehavior(
                  textHeightBehavior: TextHeightBehavior(applyHeightToFirstAscent: false),
                  child: DefaultTextStyle(style: TextStyle(height: 2),
                    textHeightBehavior: TextHeightBehavior(applyHeightToLastDescent: false),
                    child: Text('Style precedence')));
                Widget input() => const DefaultTextHeightBehavior(
                  textHeightBehavior: TextHeightBehavior(leadingDistribution: TextLeadingDistribution.even),
                  child: TextField(style: TextStyle(height: 2)));
                Widget changing(bool first, bool last, TextLeadingDistribution leading) => DefaultTextHeightBehavior(
                  textHeightBehavior: TextHeightBehavior(applyHeightToFirstAscent: first,
                    applyHeightToLastDescent: last, leadingDistribution: leading),
                  child: Text(DateTime.now().toString()));
                Widget themed(BuildContext context) => DefaultTextHeightBehavior(
                  textHeightBehavior: const TextHeightBehavior(),
                  child: Text('Theme', style: TextStyle(color: Theme.of(context).colorScheme.primary)));
                Widget composition() => const TickerMode(enabled: false,
                  child: Visibility.maintain(child: IndexedSemantics(index: 0,
                    child: DefaultTextHeightBehavior(textHeightBehavior: TextHeightBehavior(), child: Text('Nested')))));
                Widget scrolling() => ListView(children: const [DefaultTextHeightBehavior(
                  textHeightBehavior: TextHeightBehavior(), child: Text('Scroll'))]);
                List<Widget> matrix() => const <Widget>[
                """);
        for (boolean first : List.of(false, true)) {
            for (boolean last : List.of(false, true)) {
                for (String distribution : List.of("proportional", "even")) {
                    valid.append("DefaultTextHeightBehavior(textHeightBehavior: TextHeightBehavior(")
                            .append("applyHeightToFirstAscent: ").append(first)
                            .append(", applyHeightToLastDescent: ").append(last)
                            .append(", leadingDistribution: TextLeadingDistribution.").append(distribution)
                            .append("), child: Text('Matrix')),\n");
                }
            }
        }
        valid.append("];\n");
        String candidate = valid.toString();
        ArrayList<DartSymbolProbe> probes = new ArrayList<>();
        for (String symbol : List.of("DefaultTextHeightBehavior", "TextHeightBehavior", "TextLeadingDistribution")) {
            var occurrence = java.util.regex.Pattern.compile("\\b" + symbol + "\\b").matcher(candidate);
            assertTrue(occurrence.find(), symbol);
            // Generated composites use widgets.dart re-exports; their real targets
            // can be in the pinned SDK's sky_engine library rather than framework/lib.
            probes.add(probe("default-height-" + symbol, occurrence.start(), symbol,
                    "package:flutter/widgets.dart", flutterSdk));
        }
        DartCandidateAnalysisResult passed = await(analyzer.analyze(request(projectRoot, file, candidate, 1500, probes)));
        assertEquals(DartCandidateAnalysisStatus.PASSED, passed.status(), () -> passed + " stderr=" + stderr);
        assertEquals(probes.size(), passed.symbolEvidence().size());
        assertTrue(passed.symbolEvidence().stream().allMatch(DartSymbolEvidence::accepted));
        assertFalse(passed.diagnostics().stream().anyMatch(DartCandidateDiagnostic::blocking));
        assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        long version = 1501;
        for (String invalid : List.of(
                "const DefaultTextHeightBehavior()",
                "const DefaultTextHeightBehavior(child: Text('Missing behavior'))",
                "const DefaultTextHeightBehavior(textHeightBehavior: TextHeightBehavior())",
                "const DefaultTextHeightBehavior(textHeightBehavior: null, child: Text('Bad'))",
                "const DefaultTextHeightBehavior(textHeightBehavior: 'bad', child: Text('Bad'))",
                "const DefaultTextHeightBehavior(textHeightBehavior: TextHeightBehavior(), child: null)",
                "const DefaultTextHeightBehavior(textHeightBehavior: TextHeightBehavior(), child: 'bad')",
                "const DefaultTextHeightBehavior(textHeightBehavior: TextHeightBehavior(applyHeightToFirstAscent: null), child: Text('Bad'))",
                "const DefaultTextHeightBehavior(textHeightBehavior: TextHeightBehavior(applyHeightToLastDescent: 'false'), child: Text('Bad'))",
                "const DefaultTextHeightBehavior(textHeightBehavior: TextHeightBehavior(leadingDistribution: TextAlign.start), child: Text('Bad'))",
                "const DefaultTextHeightBehavior(textHeightBehavior: TextHeightBehavior(leadingDistribution: null), child: Text('Bad'))",
                "const DefaultTextHeightBehavior(textHeightBehavior: TextHeightBehavior(), textHeightApplyFirstAscent: false, child: Text('Bad'))",
                "const DefaultTextHeightBehavior(textHeightBehavior: TextHeightBehavior(), child: Text(DateTime.now().toString()))")) {
            String content = "import 'package:flutter/material.dart';\nWidget invalid() => " + invalid + ";\n";
            DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(projectRoot, file, content, version++, List.of())));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> invalid + ": " + rejected);
            assertTrue(rejected.diagnostics().stream().anyMatch(diagnostic -> diagnostic.blocking()
                    && diagnostic.severity() == DartCandidateDiagnosticSeverity.ERROR));
            assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        }
        assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
    }

    @Test
    void validatesDefaultSelectionStyleAndMergeWithEverySdkCursorPreset() throws Exception {
        Path executable = configuredDartExecutable();
        Path flutterSdk = configuredFlutter3448Sdk();
        Path projectRoot = Files.createDirectories(workspace.resolve("default_selection_style_project"));
        Path dependencyRoot = Files.createDirectories(workspace.resolve("clipper_dependency"));
        Files.createDirectories(dependencyRoot.resolve("lib"));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        writeFlutterPackageConfig(projectRoot, dependencyRoot, flutterSdk);
        Path file = lib.resolve("main.dart");
        String disk = "void main() {}\n";
        Files.writeString(file, disk, StandardCharsets.UTF_8);
        List<String> stderr = new ArrayList<>();
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(executable,
                line -> { synchronized (stderr) { stderr.add(line); } });
        StringBuilder valid = new StringBuilder("""
                import 'package:flutter/material.dart';
                Widget defaults() => const DefaultSelectionStyle(child: Text('Defaults'));
                Widget explicitNulls() => const DefaultSelectionStyle(cursorColor: null,
                  selectionColor: null, mouseCursor: null, child: Text('Clear inherited fields'));
                Widget inherited() => DefaultSelectionStyle.merge(child: const Text('Inherit all'));
                Widget nested() => DefaultSelectionStyle(cursorColor: const Color(0xFF123456),
                  selectionColor: const Color(0x40876543), mouseCursor: SystemMouseCursors.click,
                  child: DefaultSelectionStyle.merge(cursorColor: const Color(0xFF000000),
                    child: const DefaultSelectionStyle(child: Text('Nearest null clears'))));
                Widget selection() => const DefaultSelectionStyle(cursorColor: Color(0xFF123456),
                  selectionColor: Color(0x80123456), mouseCursor: SystemMouseCursors.text,
                  child: SelectionArea(child: Text('Selectable', selectionColor: Color(0x40876543))));
                Widget input() => const DefaultSelectionStyle(cursorColor: Color(0xFF123456),
                  selectionColor: Color(0x40123456), child: TextField(cursorColor: Color(0xFFABCDEF)));
                Widget themed(BuildContext context) => DefaultSelectionStyle.merge(
                  cursorColor: Theme.of(context).colorScheme.primary,
                  selectionColor: Theme.of(context).colorScheme.secondary,
                  child: Text(DateTime.now().toString()));
                Widget composition() => TickerMode(enabled: false,
                  child: DefaultSelectionStyle.merge(child: const DefaultTextHeightBehavior(
                    textHeightBehavior: TextHeightBehavior(), child: Text('Nested'))));
                DefaultSelectionStyle fallbackValueOnly() => const DefaultSelectionStyle.fallback();
                List<Widget> cursorMatrix() => <Widget>[
                """);
        List<String> systemPresets = List.of("none", "basic", "click", "forbidden", "wait", "progress",
                "contextMenu", "help", "text", "verticalText", "cell", "precise", "move", "grab", "grabbing",
                "noDrop", "alias", "copy", "disappearing", "allScroll", "resizeLeftRight", "resizeUpDown",
                "resizeUpLeftDownRight", "resizeUpRightDownLeft", "resizeUp", "resizeDown", "resizeLeft",
                "resizeRight", "resizeUpLeft", "resizeUpRight", "resizeDownLeft", "resizeDownRight",
                "resizeColumn", "resizeRow", "zoomIn", "zoomOut");
        ArrayList<String> cursors = new ArrayList<>();
        systemPresets.forEach(value -> cursors.add("SystemMouseCursors." + value));
        cursors.addAll(List.of("MouseCursor.defer", "MouseCursor.uncontrolled", "WidgetStateMouseCursor.clickable",
                "WidgetStateMouseCursor.adaptiveClickable", "WidgetStateMouseCursor.textable"));
        assertEquals(41, cursors.size());
        for (String cursor : cursors) {
            valid.append("const DefaultSelectionStyle(mouseCursor: ").append(cursor)
                    .append(", child: Text('Direct')),\n");
            valid.append("DefaultSelectionStyle.merge(mouseCursor: ").append(cursor)
                    .append(", child: const Text('Merge')),\n");
        }
        valid.append("];\n");
        String candidate = valid.toString();
        ArrayList<DartSymbolProbe> probes = new ArrayList<>();
        for (String symbol : List.of("DefaultSelectionStyle", "Color", "SystemMouseCursors", "MouseCursor",
                "WidgetStateMouseCursor")) {
            var occurrence = java.util.regex.Pattern.compile("\\b" + symbol + "\\b").matcher(candidate);
            assertTrue(occurrence.find(), symbol);
            probes.add(probe("selection-style-" + symbol, occurrence.start(), symbol,
                    "package:flutter/widgets.dart", flutterSdk));
        }
        probes.add(probe("selection-style-merge", candidate.indexOf(".merge(") + 1, "merge",
                "package:flutter/widgets.dart", flutterSdk));
        for (String member : List.of("defer", "uncontrolled", "clickable", "adaptiveClickable", "textable")) {
            probes.add(probe("selection-cursor-" + member, candidate.indexOf('.' + member + ',') + 1, member,
                    "package:flutter/widgets.dart", flutterSdk));
        }
        DartCandidateAnalysisResult passed = await(analyzer.analyze(request(projectRoot, file, candidate, 1600, probes)));
        assertEquals(DartCandidateAnalysisStatus.PASSED, passed.status(), () -> passed + " stderr=" + stderr);
        assertEquals(probes.size(), passed.symbolEvidence().size());
        assertTrue(passed.symbolEvidence().stream().allMatch(DartSymbolEvidence::accepted));
        assertFalse(passed.diagnostics().stream().anyMatch(DartCandidateDiagnostic::blocking));
        assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        long version = 1601;
        for (String invalid : List.of(
                "const DefaultSelectionStyle()",
                "const DefaultSelectionStyle(child: null)",
                "const DefaultSelectionStyle(cursorColor: 123, child: Text('Bad'))",
                "const DefaultSelectionStyle(selectionColor: 'red', child: Text('Bad'))",
                "const DefaultSelectionStyle(mouseCursor: 'click', child: Text('Bad'))",
                "const DefaultSelectionStyle(mouseCursor: SystemMouseCursors.invented, child: Text('Bad'))",
                "const DefaultSelectionStyle(merge: true, child: Text('Leaked Designer field'))",
                "DefaultSelectionStyle.merge()",
                "DefaultSelectionStyle.merge(child: null)",
                "DefaultSelectionStyle.merge(mouseCursor: 1, child: const Text('Bad'))",
                "DefaultSelectionStyle.merge(merge: false, child: const Text('Bad'))",
                "const DefaultSelectionStyle.merge(child: Text('Not a const constructor'))",
                "const DefaultSelectionStyle(child: Text(DateTime.now().toString()))")) {
            String content = "import 'package:flutter/material.dart';\nWidget invalid() => " + invalid + ";\n";
            DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(projectRoot, file, content, version++, List.of())));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> invalid + ": " + rejected);
            assertTrue(rejected.diagnostics().stream().anyMatch(diagnostic -> diagnostic.blocking()
                    && diagnostic.severity() == DartCandidateDiagnosticSeverity.ERROR));
            assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        }
        assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
    }

    @Test
    void validatesIconThemeCompleteDataAndMergeWithPinnedSdkEvidence() throws Exception {
        Path executable = configuredDartExecutable();
        Path flutterSdk = configuredFlutter3448Sdk();
        Path projectRoot = Files.createDirectories(workspace.resolve("icon_theme_project"));
        Path dependencyRoot = Files.createDirectories(workspace.resolve("clipper_dependency"));
        Files.createDirectories(dependencyRoot.resolve("lib"));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        writeFlutterPackageConfig(projectRoot, dependencyRoot, flutterSdk);
        Path file = lib.resolve("main.dart");
        String disk = "void main() {}\n";
        Files.writeString(file, disk, StandardCharsets.UTF_8);
        List<String> stderr = new ArrayList<>();
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(executable,
                line -> { synchronized (stderr) { stderr.add(line); } });
        StringBuilder valid = new StringBuilder("""
                import 'package:flutter/material.dart';
                Widget defaults() => const IconTheme(data: IconThemeData(), child: Icon(Icons.star));
                Widget inherited() => IconTheme.merge(data: const IconThemeData(), child: const Icon(Icons.star));
                Widget allFields() => const IconTheme(data: IconThemeData(size: 32, fill: 1,
                  weight: 500, grade: -25, opticalSize: 48, color: Color(0x80123456), opacity: 0.5,
                  shadows: [Shadow(color: Color(0xFFABCDEF), offset: Offset(-2, 3), blurRadius: 4)],
                  applyTextScaling: true), child: Icon(Icons.star));
                Widget fallback() => const IconTheme(data: IconThemeData.fallback(), child: Icon(Icons.star));
                Widget explicitFallback() => const IconTheme(data: IconThemeData(size: 24, fill: 0,
                  weight: 400, grade: 0, opticalSize: 48, color: Color(0xFF000000), opacity: 1,
                  applyTextScaling: false), child: Icon(Icons.star));
                Widget explicitNulls() => const IconTheme(data: IconThemeData(size: null, fill: null,
                  weight: null, grade: null, opticalSize: null, color: null, opacity: null,
                  shadows: null, applyTextScaling: null), child: Icon(Icons.star));
                Widget nested() => IconTheme(data: const IconThemeData(size: 48, color: Color(0xFF123456),
                  shadows: [Shadow(blurRadius: 2)], applyTextScaling: true),
                  child: IconTheme.merge(data: const IconThemeData(shadows: [], applyTextScaling: false),
                    child: const IconTheme(data: IconThemeData(), child: Icon(Icons.star))));
                Widget localOverride() => const IconTheme(data: IconThemeData(size: 48, opacity: 0.25,
                  color: Color(0xFF123456), shadows: [Shadow(blurRadius: 2)], applyTextScaling: true),
                  child: Icon(Icons.star, size: 20, color: Color(0x80ABCDEF), shadows: [],
                    applyTextScaling: false, fontWeight: FontWeight.w700, blendMode: BlendMode.srcOver));
                Widget themed(BuildContext context) => IconTheme.merge(data: IconThemeData(
                  color: Theme.of(context).colorScheme.primary,
                  shadows: [Shadow(color: Theme.of(context).colorScheme.primary, offset: const Offset(1, 2))]),
                  child: Text(DateTime.now().toString()));
                List<Widget> dataMatrix() => <Widget>[
                """);
        List<String> dataValues = List.of("size: 0", "size: 28.5", "fill: 0", "fill: 1",
                "weight: 0.5", "weight: 32767.5", "grade: -32768", "grade: 32767.5",
                "opticalSize: 0.5", "opticalSize: 32767.5", "color: Color(0x00000000)",
                "opacity: -2", "opacity: 0", "opacity: 0.5", "opacity: 1", "opacity: 2",
                "shadows: []", "shadows: [Shadow(), Shadow(offset: Offset(-1, 1), blurRadius: 2)]",
                "applyTextScaling: false", "applyTextScaling: true");
        for (String data : dataValues) {
            valid.append("const IconTheme(data: IconThemeData(").append(data)
                    .append("), child: Icon(Icons.star)),\n");
            valid.append("IconTheme.merge(data: const IconThemeData(").append(data)
                    .append("), child: const Icon(Icons.star)),\n");
        }
        valid.append("];\n");
        String candidate = valid.toString();
        ArrayList<DartSymbolProbe> probes = new ArrayList<>();
        for (String symbol : List.of("IconTheme", "IconThemeData", "Icon", "Color", "Shadow", "Offset")) {
            var occurrence = java.util.regex.Pattern.compile("\\b" + symbol + "\\b").matcher(candidate);
            assertTrue(occurrence.find(), symbol);
            probes.add(probe("icon-theme-" + symbol, occurrence.start(), symbol,
                    "package:flutter/widgets.dart", flutterSdk));
        }
        probes.add(probe("icon-theme-merge", candidate.indexOf("IconTheme.merge(") + "IconTheme.".length(),
                "merge", "package:flutter/widgets.dart", flutterSdk));
        probes.add(probe("icon-theme-data-fallback", candidate.indexOf("IconThemeData.fallback(")
                + "IconThemeData.".length(), "fallback", "package:flutter/widgets.dart", flutterSdk));
        DartCandidateAnalysisResult passed = await(analyzer.analyze(request(projectRoot, file, candidate, 1700, probes)));
        assertEquals(DartCandidateAnalysisStatus.PASSED, passed.status(), () -> passed + " stderr=" + stderr);
        assertEquals(probes.size(), passed.symbolEvidence().size());
        assertTrue(passed.symbolEvidence().stream().allMatch(DartSymbolEvidence::accepted));
        assertFalse(passed.diagnostics().stream().anyMatch(DartCandidateDiagnostic::blocking));
        assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        long version = 1701;
        for (String invalid : List.of(
                "const IconTheme(child: Icon(Icons.star))",
                "const IconTheme(data: IconThemeData())",
                "const IconTheme(data: null, child: Icon(Icons.star))",
                "const IconTheme(data: IconThemeData(), child: null)",
                "const IconTheme(data: IconThemeData(size: 'large'), child: Icon(Icons.star))",
                "const IconTheme(data: IconThemeData(fill: 2), child: Icon(Icons.star))",
                "const IconTheme(data: IconThemeData(weight: 0), child: Icon(Icons.star))",
                "const IconTheme(data: IconThemeData(opticalSize: 0), child: Icon(Icons.star))",
                "const IconTheme(data: IconThemeData(color: 123), child: Icon(Icons.star))",
                "const IconTheme(data: IconThemeData(opacity: true), child: Icon(Icons.star))",
                "const IconTheme(data: IconThemeData(shadows: [1]), child: Icon(Icons.star))",
                "const IconTheme(data: IconThemeData(applyTextScaling: 'false'), child: Icon(Icons.star))",
                "const IconTheme(data: IconThemeData(fontWeight: FontWeight.w700), child: Icon(Icons.star))",
                "const IconTheme(merge: true, data: IconThemeData(), child: Icon(Icons.star))",
                "IconTheme.merge(child: const Icon(Icons.star))",
                "IconTheme.merge(data: const IconThemeData())",
                "IconTheme.merge(merge: false, data: const IconThemeData(), child: const Icon(Icons.star))",
                "const IconTheme.merge(data: IconThemeData(), child: Icon(Icons.star))")) {
            String content = "import 'package:flutter/material.dart';\nWidget invalid() => " + invalid + ";\n";
            DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(projectRoot, file, content, version++, List.of())));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> invalid + ": " + rejected);
            assertTrue(rejected.diagnostics().stream().anyMatch(diagnostic -> diagnostic.blocking()
                    && diagnostic.severity() == DartCandidateDiagnosticSeverity.ERROR));
            assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        }
        assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
    }

    @Test
    void validatesImageIconNullableAssetProvidersAndPinnedInheritanceContract() throws Exception {
        Path executable = configuredDartExecutable();
        Path flutterSdk = configuredFlutter3448Sdk();
        Path projectRoot = Files.createDirectories(workspace.resolve("image_icon_project"));
        Path dependencyRoot = Files.createDirectories(workspace.resolve("clipper_dependency"));
        Files.createDirectories(dependencyRoot.resolve("lib"));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        writeFlutterPackageConfig(projectRoot, dependencyRoot, flutterSdk);
        Path pubspec = projectRoot.resolve("pubspec.yaml");
        String originalPubspec = Files.readString(pubspec, StandardCharsets.UTF_8);
        assertFalse(originalPubspec.contains("uses-material-design"));
        Path file = lib.resolve("main.dart");
        String disk = "void main() {}\n";
        Files.writeString(file, disk, StandardCharsets.UTF_8);
        List<String> stderr = new ArrayList<>();
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(executable,
                line -> { synchronized (stderr) { stderr.add(line); } });
        StringBuilder valid = new StringBuilder("""
                import 'package:flutter/material.dart';
                Widget empty() => const ImageIcon(null);
                Widget labelledEmpty() => const ImageIcon(null, size: 0, semanticLabel: 'Empty image icon');
                Widget explicitNulls() => const ImageIcon(null, size: null, color: null, semanticLabel: null);
                Widget complete() => const ImageIcon(AssetImage('assets/star.png'), size: 32,
                  color: Color(0x80123456), semanticLabel: 'Star');
                Widget inherited() => const IconTheme(data: IconThemeData(size: 48,
                  color: Color(0xCC123456), opacity: 0.5, applyTextScaling: true,
                  shadows: [Shadow(blurRadius: 4)]), child: ImageIcon(AssetImage('assets/star.png')));
                Widget merged() => IconTheme.merge(data: const IconThemeData(size: 40, opacity: 0.25),
                  child: const ImageIcon(AssetImage('assets/star.png'), size: 20, color: Color(0x80123456)));
                Widget themed(BuildContext context) => ImageIcon(const AssetImage('assets/star.png'),
                  color: Theme.of(context).colorScheme.primary, semanticLabel: DateTime.now().toString());
                Widget composition() => const Directionality(textDirection: TextDirection.rtl,
                  child: Row(children: [ImageIcon(null), ImageIcon(AssetImage('assets/star.png'))]));
                List<Widget> providerMatrix() => <Widget>[
                """);
        List<String> providers = List.of("null", "AssetImage('assets/star.png')",
                "AssetImage('assets/star.png', package: 'example_icons')",
                "ExactAssetImage('assets/star.png', scale: 2)",
                "ExactAssetImage('assets/star.png', scale: 1.5, package: 'example_icons')",
                "ResizeImage(AssetImage('assets/star.png'), width: 24)",
                "ResizeImage(AssetImage('assets/star.png'), height: 48)",
                "ResizeImage(ExactAssetImage('assets/star.png', scale: 2), width: 24, height: 32, policy: ResizeImagePolicy.exact)",
                "ResizeImage(AssetImage('assets/star.png', package: 'example_icons'), width: 32, height: 48, policy: ResizeImagePolicy.fit, allowUpscaling: true)");
        for (String provider : providers) {
            valid.append("const ImageIcon(").append(provider).append("),\n");
            valid.append("const ImageIcon(").append(provider)
                    .append(", size: 28.5, color: Color(0x00123456), semanticLabel: 'Provider'),\n");
        }
        valid.append("];\n");
        String candidate = valid.toString();
        ArrayList<DartSymbolProbe> probes = new ArrayList<>();
        for (String symbol : List.of("ImageIcon", "AssetImage", "ExactAssetImage", "ResizeImage",
                "ResizeImagePolicy", "IconTheme", "IconThemeData", "Color")) {
            var occurrence = java.util.regex.Pattern.compile("\\b" + symbol + "\\b").matcher(candidate);
            assertTrue(occurrence.find(), symbol);
            probes.add(probe("image-icon-" + symbol, occurrence.start(), symbol,
                    "package:flutter/widgets.dart", flutterSdk));
        }
        for (String policy : List.of("exact", "fit")) {
            probes.add(probe("image-icon-resize-" + policy,
                    candidate.indexOf("ResizeImagePolicy." + policy) + "ResizeImagePolicy.".length(),
                    policy, "package:flutter/widgets.dart", flutterSdk));
        }
        probes.add(probe("image-icon-theme-merge", candidate.indexOf("IconTheme.merge(") + "IconTheme.".length(),
                "merge", "package:flutter/widgets.dart", flutterSdk));
        DartCandidateAnalysisResult passed = await(analyzer.analyze(request(projectRoot, file, candidate, 1800, probes)));
        assertEquals(DartCandidateAnalysisStatus.PASSED, passed.status(), () -> passed + " stderr=" + stderr);
        assertEquals(probes.size(), passed.symbolEvidence().size());
        assertTrue(passed.symbolEvidence().stream().allMatch(DartSymbolEvidence::accepted));
        assertFalse(passed.diagnostics().stream().anyMatch(DartCandidateDiagnostic::blocking));
        assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        long version = 1801;
        for (String invalid : List.of(
                "const ImageIcon()",
                "const ImageIcon(image: null)",
                "const ImageIcon('assets/star.png')",
                "const ImageIcon(null, size: 'large')",
                "const ImageIcon(null, color: 123)",
                "const ImageIcon(null, semanticLabel: 123)",
                "const ImageIcon(null, child: Text('Not a wrapper'))",
                "const ImageIcon(null, applyTextScaling: true)",
                "const ImageIcon(null, shadows: [Shadow()])",
                "const ImageIcon(null, useOriginalColors: true)",
                "const ImageIcon(null, fit: BoxFit.cover)",
                "const ImageIcon(AssetImage(123))",
                "const ImageIcon(ExactAssetImage('assets/star.png', scale: '2'))",
                "const ImageIcon(ResizeImage(AssetImage('assets/star.png')))",
                "const ImageIcon(ResizeImage(AssetImage('assets/star.png'), width: 24, policy: 'fit'))",
                "const ImageIcon.merge(null)")) {
            String content = "import 'package:flutter/material.dart';\nWidget invalid() => " + invalid + ";\n";
            DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(projectRoot, file, content, version++, List.of())));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> invalid + ": " + rejected);
            assertTrue(rejected.diagnostics().stream().anyMatch(diagnostic -> diagnostic.blocking()
                    && diagnostic.severity() == DartCandidateDiagnosticSeverity.ERROR));
            assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        }
        assertEquals(originalPubspec, Files.readString(pubspec, StandardCharsets.UTF_8));
        assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
    }

    @Test
    void validatesDividerCompleteGeometryAndPinnedMaterialThemeContract() throws Exception {
        Path executable = configuredDartExecutable();
        Path flutterSdk = configuredFlutter3448Sdk();
        Path projectRoot = Files.createDirectories(workspace.resolve("divider_project"));
        Path dependencyRoot = Files.createDirectories(workspace.resolve("clipper_dependency"));
        Files.createDirectories(dependencyRoot.resolve("lib"));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        writeFlutterPackageConfig(projectRoot, dependencyRoot, flutterSdk);
        Path file = lib.resolve("main.dart");
        String disk = "void main() {}\n";
        Files.writeString(file, disk, StandardCharsets.UTF_8);
        Path pubspec = projectRoot.resolve("pubspec.yaml");
        String originalPubspec = Files.readString(pubspec, StandardCharsets.UTF_8);
        List<String> stderr = new ArrayList<>();
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(executable,
                line -> { synchronized (stderr) { stderr.add(line); } });
        StringBuilder valid = new StringBuilder("""
                import 'package:flutter/material.dart';
                Widget defaults() => const Divider();
                Widget explicitNulls() => const Divider(height: null, thickness: null,
                  indent: null, endIndent: null, color: null, radius: null);
                Widget complete() => const Divider(height: 20, thickness: 4, indent: 3,
                  endIndent: 7, color: Color(0x80123456), radius: BorderRadius.only(
                    topLeft: Radius.elliptical(2, 3), topRight: Radius.elliptical(4, 5),
                    bottomLeft: Radius.elliptical(6, 7), bottomRight: Radius.elliptical(8, 9)));
                Widget directional() => const Directionality(textDirection: TextDirection.rtl,
                  child: Divider(height: 24, thickness: 2, indent: 3, endIndent: 9,
                    radius: BorderRadiusDirectional.only(topStart: Radius.elliptical(2, 3),
                      topEnd: Radius.elliptical(4, 5), bottomStart: Radius.elliptical(6, 7),
                      bottomEnd: Radius.elliptical(8, 9))));
                Widget inherited() => const DividerTheme(data: DividerThemeData(space: 24,
                  thickness: 3, indent: 2, endIndent: 4, color: Color(0xFF123456),
                  radius: BorderRadius.all(Radius.circular(2))), child: Divider());
                Widget localOverride() => const DividerTheme(data: DividerThemeData(space: 24,
                  thickness: 3, color: Color(0xFF123456)), child: Divider(height: 16,
                    thickness: 1, color: Color(0xFFABCDEF), radius: BorderRadius.zero));
                Widget themed(BuildContext context) => Divider(
                  color: Theme.of(context).colorScheme.outlineVariant);
                Widget material2() => Theme(data: ThemeData(useMaterial3: false), child: const Divider());
                Widget material3() => Theme(data: ThemeData(useMaterial3: true), child: const Divider());
                // Accepted by the constructor; the pinned paint-time hairline/radius
                // limitation is separately exercised by the real Flutter Canvas tests.
                Widget paintTimeCombination() => const Divider(thickness: 0,
                  radius: BorderRadius.all(Radius.circular(4)));
                List<Widget> fieldMatrix() => <Widget>[
                """);
        for (String field : List.of("height", "thickness", "indent", "endIndent")) {
            for (String value : List.of("0", "0.5", "32")) {
                valid.append("const Divider(").append(field).append(": ").append(value).append("),\n");
            }
        }
        for (String radius : List.of("BorderRadius.zero", "BorderRadiusDirectional.zero",
                "BorderRadius.all(Radius.circular(4))", "BorderRadiusDirectional.all(Radius.elliptical(2, 3))",
                "BorderRadius.only(bottomLeft: Radius.elliptical(4, 6))",
                "BorderRadiusDirectional.only(bottomEnd: Radius.elliptical(4, 6))")) {
            valid.append("const Divider(thickness: 2, radius: ").append(radius).append("),\n");
        }
        valid.append("];\n");
        String candidate = valid.toString();
        ArrayList<DartSymbolProbe> probes = new ArrayList<>();
        for (String symbol : List.of("Divider", "DividerTheme", "DividerThemeData", "BorderRadius",
                "BorderRadiusDirectional", "Radius", "Color")) {
            var occurrence = java.util.regex.Pattern.compile("\\b" + symbol + "\\b").matcher(candidate);
            assertTrue(occurrence.find(), symbol);
            probes.add(probe("divider-" + symbol, occurrence.start(), symbol,
                    "package:flutter/material.dart", flutterSdk));
        }
        for (String factory : List.of("BorderRadius.only", "BorderRadiusDirectional.only",
                "Radius.elliptical", "Radius.circular")) {
            String member = factory.substring(factory.indexOf('.') + 1);
            probes.add(probe("divider-" + factory, candidate.indexOf(factory) + factory.indexOf('.') + 1,
                    member, "package:flutter/material.dart", flutterSdk));
        }
        DartCandidateAnalysisResult passed = await(analyzer.analyze(request(projectRoot, file, candidate, 1900, probes)));
        assertEquals(DartCandidateAnalysisStatus.PASSED, passed.status(), () -> passed + " stderr=" + stderr);
        assertEquals(probes.size(), passed.symbolEvidence().size());
        assertTrue(passed.symbolEvidence().stream().allMatch(DartSymbolEvidence::accepted));
        assertFalse(passed.diagnostics().stream().anyMatch(DartCandidateDiagnostic::blocking));
        long version = 1901;
        for (String invalid : List.of("const Divider(height: -1)", "const Divider(thickness: -1)",
                "const Divider(indent: -1)", "const Divider(endIndent: -1)",
                "const Divider(height: '20')", "const Divider(thickness: true)",
                "const Divider(indent: '3')", "const Divider(endIndent: false)",
                "const Divider(color: 123)", "const Divider(radius: Radius.circular(2))",
                "const Divider(radius: 'round')", "const Divider(child: Text('Not a wrapper'))",
                "const Divider(width: 20)", "const Divider(semanticLabel: 'Line')",
                "const Divider(clipBehavior: Clip.antiAlias)", "const Divider.vertical()")) {
            String content = "import 'package:flutter/material.dart';\nWidget invalid() => " + invalid + ";\n";
            DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(projectRoot, file, content, version++, List.of())));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> invalid + ": " + rejected);
            assertTrue(rejected.diagnostics().stream().anyMatch(diagnostic -> diagnostic.blocking()
                    && diagnostic.severity() == DartCandidateDiagnosticSeverity.ERROR));
            assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        }
        assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        assertEquals(originalPubspec, Files.readString(pubspec, StandardCharsets.UTF_8));
        assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
    }

    @Test
    void validatesVerticalDividerCompleteGeometryAndPinnedMaterialThemeContract() throws Exception {
        Path executable = configuredDartExecutable();
        Path flutterSdk = configuredFlutter3448Sdk();
        Path projectRoot = Files.createDirectories(workspace.resolve("vertical_divider_project"));
        Path dependencyRoot = Files.createDirectories(workspace.resolve("clipper_dependency"));
        Files.createDirectories(dependencyRoot.resolve("lib"));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        writeFlutterPackageConfig(projectRoot, dependencyRoot, flutterSdk);
        Path file = lib.resolve("main.dart");
        String disk = "void main() {}\n";
        Files.writeString(file, disk, StandardCharsets.UTF_8);
        Path pubspec = projectRoot.resolve("pubspec.yaml");
        String originalPubspec = Files.readString(pubspec, StandardCharsets.UTF_8);
        List<String> stderr = new ArrayList<>();
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(executable,
                line -> { synchronized (stderr) { stderr.add(line); } });
        StringBuilder valid = new StringBuilder("""
                import 'package:flutter/material.dart';
                Widget defaults() => const VerticalDivider();
                Widget explicitNulls() => const VerticalDivider(width: null, thickness: null,
                  indent: null, endIndent: null, color: null, radius: null);
                Widget complete() => const VerticalDivider(width: 20, thickness: 4, indent: 3,
                  endIndent: 7, color: Color(0x80123456), radius: BorderRadius.only(
                    topLeft: Radius.elliptical(2, 3), topRight: Radius.elliptical(4, 5),
                    bottomLeft: Radius.elliptical(6, 7), bottomRight: Radius.elliptical(8, 9)));
                Widget directional() => const Directionality(textDirection: TextDirection.rtl,
                  child: VerticalDivider(width: 24, thickness: 2, indent: 3, endIndent: 9,
                    radius: BorderRadiusDirectional.only(topStart: Radius.elliptical(2, 3),
                      topEnd: Radius.elliptical(4, 5), bottomStart: Radius.elliptical(6, 7),
                      bottomEnd: Radius.elliptical(8, 9))));
                Widget inherited() => const DividerTheme(data: DividerThemeData(space: 24,
                  thickness: 3, indent: 2, endIndent: 4, color: Color(0xFF123456),
                  radius: BorderRadius.all(Radius.circular(2))), child: VerticalDivider());
                Widget localOverride() => const DividerTheme(data: DividerThemeData(space: 24,
                  thickness: 3, color: Color(0xFF123456)), child: VerticalDivider(width: 16,
                    thickness: 1, color: Color(0xFFABCDEF), radius: BorderRadius.zero));
                Widget themed(BuildContext context) => VerticalDivider(
                  color: Theme.of(context).colorScheme.outlineVariant);
                Widget boundedRow() => const SizedBox(height: 80, child: Row(children: [
                  Text('Before'), VerticalDivider(width: 20, thickness: 2, indent: 4, endIndent: 8),
                  Text('After')]));
                Widget intrinsicRow() => const IntrinsicHeight(child: Row(children: [
                  Text('Before'), VerticalDivider(), Text('After')]));
                Widget horizontalList() => SizedBox(height: 80, child: ListView(
                  scrollDirection: Axis.horizontal, children: const [
                    SizedBox(width: 40), VerticalDivider(), SizedBox(width: 40)]));
                Widget material2() => Theme(data: ThemeData(useMaterial3: false), child: const VerticalDivider());
                Widget material3() => Theme(data: ThemeData(useMaterial3: true), child: const VerticalDivider());
                // Accepted by the constructor; the pinned paint-time hairline/radius
                // limitation is separately exercised by the real Flutter Canvas tests.
                Widget paintTimeCombination() => const VerticalDivider(thickness: 0,
                  radius: BorderRadius.all(Radius.circular(4)));
                List<Widget> fieldMatrix() => <Widget>[
                """);
        for (String field : List.of("width", "thickness", "indent", "endIndent")) {
            for (String value : List.of("0", "0.5", "32")) {
                valid.append("const VerticalDivider(").append(field).append(": ").append(value).append("),\n");
            }
        }
        for (String radius : List.of("BorderRadius.zero", "BorderRadiusDirectional.zero",
                "BorderRadius.all(Radius.circular(4))", "BorderRadiusDirectional.all(Radius.elliptical(2, 3))",
                "BorderRadius.only(bottomLeft: Radius.elliptical(4, 6))",
                "BorderRadiusDirectional.only(bottomEnd: Radius.elliptical(4, 6))")) {
            valid.append("const VerticalDivider(thickness: 2, radius: ").append(radius).append("),\n");
        }
        valid.append("];\n");
        String candidate = valid.toString();
        ArrayList<DartSymbolProbe> probes = new ArrayList<>();
        for (String symbol : List.of("VerticalDivider", "DividerTheme", "DividerThemeData", "BorderRadius",
                "BorderRadiusDirectional", "Radius", "Color")) {
            var occurrence = java.util.regex.Pattern.compile("\\b" + symbol + "\\b").matcher(candidate);
            assertTrue(occurrence.find(), symbol);
            probes.add(probe("vertical-divider-" + symbol, occurrence.start(), symbol,
                    "package:flutter/material.dart", flutterSdk));
        }
        for (String factory : List.of("BorderRadius.only", "BorderRadiusDirectional.only",
                "Radius.elliptical", "Radius.circular")) {
            String member = factory.substring(factory.indexOf('.') + 1);
            probes.add(probe("vertical-divider-" + factory, candidate.indexOf(factory) + factory.indexOf('.') + 1,
                    member, "package:flutter/material.dart", flutterSdk));
        }
        DartCandidateAnalysisResult passed = await(analyzer.analyze(request(projectRoot, file, candidate, 2000, probes)));
        assertEquals(DartCandidateAnalysisStatus.PASSED, passed.status(), () -> passed + " stderr=" + stderr);
        assertEquals(probes.size(), passed.symbolEvidence().size());
        assertTrue(passed.symbolEvidence().stream().allMatch(DartSymbolEvidence::accepted));
        assertFalse(passed.diagnostics().stream().anyMatch(DartCandidateDiagnostic::blocking));
        long version = 2001;
        for (String invalid : List.of("const VerticalDivider(width: -1)", "const VerticalDivider(thickness: -1)",
                "const VerticalDivider(indent: -1)", "const VerticalDivider(endIndent: -1)",
                "const VerticalDivider(width: '20')", "const VerticalDivider(thickness: true)",
                "const VerticalDivider(indent: '3')", "const VerticalDivider(endIndent: false)",
                "const VerticalDivider(color: 123)", "const VerticalDivider(radius: Radius.circular(2))",
                "const VerticalDivider(radius: 'round')", "const VerticalDivider(child: Text('Not a wrapper'))",
                "const VerticalDivider(height: 20)", "const VerticalDivider(semanticLabel: 'Line')",
                "const VerticalDivider(clipBehavior: Clip.antiAlias)", "const VerticalDivider.horizontal()")) {
            String content = "import 'package:flutter/material.dart';\nWidget invalid() => " + invalid + ";\n";
            DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(projectRoot, file, content, version++, List.of())));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> invalid + ": " + rejected);
            assertTrue(rejected.diagnostics().stream().anyMatch(diagnostic -> diagnostic.blocking()
                    && diagnostic.severity() == DartCandidateDiagnosticSeverity.ERROR));
            assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        }
        assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        assertEquals(originalPubspec, Files.readString(pubspec, StandardCharsets.UTF_8));
        assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
    }

    @Test
    void validatesCardAllVariantsCompleteFieldsShapesAndTypedReferences() throws Exception {
        Path executable = configuredDartExecutable();
        Path flutterSdk = configuredFlutter3448Sdk();
        Path projectRoot = Files.createDirectories(workspace.resolve("card_project"));
        Path dependencyRoot = Files.createDirectories(workspace.resolve("clipper_dependency"));
        Path dependencyLib = Files.createDirectories(dependencyRoot.resolve("lib"));
        Files.writeString(dependencyLib.resolve("shapes.dart"), """
                import 'package:flutter/painting.dart';
                class Shapes {
                  static ShapeBorder get outline => const BorderDirectional(
                    start: BorderSide(width: 3), end: BorderSide(width: 1));
                }
                """, StandardCharsets.UTF_8);
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        writeFlutterPackageConfig(projectRoot, dependencyRoot, flutterSdk);
        Path file = lib.resolve("main.dart");
        String disk = "void main() {}\n";
        Files.writeString(file, disk, StandardCharsets.UTF_8);
        Path pubspec = projectRoot.resolve("pubspec.yaml");
        String originalPubspec = Files.readString(pubspec, StandardCharsets.UTF_8);
        List<String> stderr = new ArrayList<>();
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(executable,
                line -> { synchronized (stderr) { stderr.add(line); } });
        StringBuilder valid = new StringBuilder("""
                import 'package:flutter/material.dart';
                import 'package:clipper_dependency/shapes.dart' as project_shapes;
                const currentCardShape = RoundedRectangleBorder();
                ShapeBorder makeCardShape() => const CircleBorder() + const StadiumBorder();
                void analyzerStaticTypeProofScope() {
                  // analyzer static-type proof insertion
                }
                Widget localReference() => const Card(shape: currentCardShape);
                Widget localFactory() => Card.filled(shape: makeCardShape());
                Widget importedReference() => Card.outlined(shape: project_shapes.Shapes.outline);
                Widget inherited() => const CardTheme(data: CardThemeData(
                  color: Color(0xFF123456), shadowColor: Color(0x80123456),
                  surfaceTintColor: Color(0x20123456), elevation: 3,
                  shape: CircleBorder(eccentricity: 0.5),
                  margin: EdgeInsetsDirectional.only(start: 2, end: 8),
                  clipBehavior: Clip.antiAlias), child: Card.outlined(child: Text('Inherited')));
                Widget localOverride() => const CardTheme(data: CardThemeData(elevation: 9,
                  color: Color(0xFF123456)), child: Card.filled(elevation: 0,
                    color: Color(0x00000000), margin: EdgeInsets.zero, shape: StadiumBorder()));
                Widget semanticColors(BuildContext context) => Card.outlined(
                  color: Theme.of(context).colorScheme.surface,
                  shadowColor: Theme.of(context).colorScheme.shadow,
                  surfaceTintColor: Theme.of(context).colorScheme.surfaceTint,
                  shape: RoundedRectangleBorder(side: BorderSide(
                    color: Theme.of(context).colorScheme.outlineVariant, width: 2)));
                Widget material2() => Theme(data: ThemeData(useMaterial3: false),
                  child: const Row(children: [Card(), Card.filled(), Card.outlined()]));
                Widget material3() => Theme(data: ThemeData(useMaterial3: true),
                  child: const Column(children: [Card(), Card.filled(), Card.outlined()]));
                List<Widget> fieldAndShapeMatrix() => <Widget>[
                """);
        List<String> shapes = List.of(
                "RoundedRectangleBorder(borderRadius: BorderRadius.only(topLeft: Radius.elliptical(2, 3)))",
                "BeveledRectangleBorder(borderRadius: BorderRadiusDirectional.only(bottomEnd: Radius.elliptical(4, 5)))",
                "ContinuousRectangleBorder(borderRadius: BorderRadius.all(Radius.elliptical(2, 3)))",
                "RoundedSuperellipseBorder(borderRadius: BorderRadiusDirectional.all(Radius.elliptical(2, 3)))",
                "CircleBorder(eccentricity: 0.25)", "OvalBorder(eccentricity: 0.75)", "StadiumBorder()",
                "LinearBorder(start: LinearBorderEdge(size: 0.25, alignment: -3), end: LinearBorderEdge(size: 1, alignment: 4), top: LinearBorderEdge(size: 0), bottom: LinearBorderEdge(alignment: 0.5))",
                "StarBorder(points: 5.5, innerRadiusRatio: 0.4, pointRounding: 0.2, valleyRounding: 0.3, rotation: -45, squash: 0.5)",
                "StarBorder.polygon(sides: 6.5, pointRounding: 0.5, rotation: 450, squash: 0.25)");
        int combinations = 0;
        for (String constructor : List.of("Card", "Card.filled", "Card.outlined")) {
            valid.append("const ").append(constructor).append("(),\n");
            valid.append("const ").append(constructor).append("(color: null, shadowColor: null, surfaceTintColor: null, elevation: null, shape: null, margin: null, clipBehavior: null, child: null),\n");
            for (String shape : shapes) {
                combinations++;
                // Every OutlinedBorder family accepts the same complete BorderSide.
                String completeShape = shape.replaceFirst("\\(", "(side: BorderSide(color: Color(0x80123456), width: 2.5, style: BorderStyle.solid, strokeAlign: 2), ");
                valid.append("const ").append(constructor).append("(color: Color(0xFF123456), shadowColor: Color(0x80123456), surfaceTintColor: Color(0x20123456), elevation: 3.5, borderOnForeground: false, margin: EdgeInsetsDirectional.only(start: 2, top: 3, end: 4, bottom: 5), clipBehavior: Clip.antiAliasWithSaveLayer, semanticContainer: false, child: Text('Card'), shape: ")
                        .append(completeShape).append("),\n");
            }
            for (String value : List.of("0", "0.5", "24")) {
                valid.append("const ").append(constructor).append("(elevation: ").append(value).append("),\n");
            }
        }
        // These are valid Dart; Canvas applies its separate explicit path-complexity budget.
        valid.append("const Card(shape: StarBorder(points: 4097)),\nconst Card(shape: StarBorder.polygon(sides: 1e100)),\n];\n");
        assertEquals(30, combinations);
        String candidate = valid.toString();
        ArrayList<DartSymbolProbe> probes = new ArrayList<>();
        for (String symbol : List.of("Card", "CardTheme", "CardThemeData", "Color", "BorderSide", "BorderStyle",
                "BorderRadius", "BorderRadiusDirectional", "Radius", "EdgeInsetsDirectional", "RoundedRectangleBorder",
                "BeveledRectangleBorder", "ContinuousRectangleBorder", "RoundedSuperellipseBorder", "CircleBorder",
                "OvalBorder", "StadiumBorder", "LinearBorder", "LinearBorderEdge", "StarBorder")) {
            var occurrence = java.util.regex.Pattern.compile("\\b" + symbol + "\\b").matcher(candidate);
            assertTrue(occurrence.find(), symbol);
            probes.add(probe("card-" + symbol, occurrence.start(), symbol, "package:flutter/material.dart", flutterSdk));
        }
        for (String factory : List.of("Card.filled", "Card.outlined", "StarBorder.polygon", "Radius.elliptical")) {
            String member = factory.substring(factory.indexOf('.') + 1);
            probes.add(probe("card-" + factory, candidate.indexOf(factory) + factory.indexOf('.') + 1,
                    member, "package:flutter/material.dart", flutterSdk));
        }
        for (String expression : List.of("currentCardShape", "makeCardShape()")) {
            int offset = candidate.indexOf("shape: " + expression) + "shape: ".length();
            String symbol = rootSymbol(expression);
            probes.add(typedProbe("card-local-" + symbol, offset, symbol, "project:current", lib,
                    offset, expression.length(), candidate, "ShapeBorder"));
        }
        String imported = "project_shapes.Shapes.outline";
        int importedOffset = candidate.indexOf("shape: " + imported) + "shape: ".length();
        probes.add(typedProbe("card-imported-outline", importedOffset + imported.lastIndexOf('.') + 1,
                "outline", "package:clipper_dependency/shapes.dart", dependencyLib,
                importedOffset, imported.length(), candidate, "ShapeBorder"));
        DartCandidateAnalysisResult passed = await(analyzer.analyze(request(projectRoot, file, candidate, 2100, probes)));
        assertEquals(DartCandidateAnalysisStatus.PASSED, passed.status(), () -> passed + " stderr=" + stderr);
        assertEquals(27, probes.size());
        assertEquals(probes.size(), passed.symbolEvidence().size());
        assertTrue(passed.symbolEvidence().stream().allMatch(DartSymbolEvidence::accepted));
        assertFalse(passed.diagnostics().stream().anyMatch(DartCandidateDiagnostic::blocking));
        long version = 2101;
        for (String invalid : List.of("const Card(elevation: -1)", "const Card(color: true)",
                "const Card(shadowColor: 3)", "const Card(surfaceTintColor: 'red')", "const Card(elevation: '3')",
                "const Card(borderOnForeground: null)", "const Card(semanticContainer: null)",
                "const Card(clipBehavior: true)", "const Card(child: 'text')", "const Card(shape: Radius.circular(2))",
                "const Card(width: 20)", "const Card(variant: 'filled')", "const Card.elevated()",
                "const Card(shapeRadius: BorderRadius.zero)", "const Card(shape: CircleBorder(eccentricity: 2))",
                "const Card(shape: OvalBorder(eccentricity: -1))", "const Card(shape: StarBorder(points: 1.5))",
                "const Card(shape: StarBorder(innerRadiusRatio: 1.1))", "const Card(shape: StarBorder(pointRounding: -0.1))",
                "const Card(shape: StarBorder(valleyRounding: 1.1))", "const Card(shape: StarBorder(squash: 1.1))",
                "const Card(shape: StarBorder(pointRounding: 0.6, valleyRounding: 0.5))",
                "const Card(shape: StarBorder.polygon(sides: 1.5))", "const Card(shape: StarBorder.polygon(valleyRounding: 0.1))",
                "const Card(shape: StarBorder.polygon(innerRadiusRatio: 0.4))",
                "const Card(shape: LinearBorder(top: LinearBorderEdge(size: -0.1)))",
                "const Card(shape: LinearBorder(top: LinearBorderEdge(size: 1.1)))")) {
            String content = "import 'package:flutter/material.dart';\nWidget invalid() => " + invalid + ";\n";
            DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(projectRoot, file, content, version++, List.of())));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> invalid + ": " + rejected);
            assertTrue(rejected.diagnostics().stream().anyMatch(diagnostic -> diagnostic.blocking()
                    && diagnostic.severity() == DartCandidateDiagnosticSeverity.ERROR));
            assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        }
        assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        assertEquals(originalPubspec, Files.readString(pubspec, StandardCharsets.UTF_8));
        assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
    }

    @Test
    void validatesBadgeBothConstructorsCompleteTextStyleAndCountBranches() throws Exception {
        Path executable = configuredDartExecutable();
        Path flutterSdk = configuredFlutter3448Sdk();
        Path projectRoot = Files.createDirectories(workspace.resolve("badge_project"));
        Path dependencyRoot = Files.createDirectories(workspace.resolve("clipper_dependency"));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        writeFlutterPackageConfig(projectRoot, dependencyRoot, flutterSdk);
        Path file = lib.resolve("main.dart");
        String disk = "void main() {}\n";
        Files.writeString(file, disk, StandardCharsets.UTF_8);
        Path pubspec = projectRoot.resolve("pubspec.yaml");
        String originalPubspec = Files.readString(pubspec, StandardCharsets.UTF_8);
        List<String> stderr = new ArrayList<>();
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(executable,
                line -> { synchronized (stderr) { stderr.add(line); } });
        String candidate = """
                import 'package:flutter/material.dart';
                const completeBadgeStyle = TextStyle(
                  inherit: false, color: Color(0xFF123456), backgroundColor: Color(0x20123456),
                  fontSize: 13, fontWeight: FontWeight.w600, fontStyle: FontStyle.italic,
                  letterSpacing: -0.5, wordSpacing: 1.5, textBaseline: TextBaseline.ideographic,
                  height: 1.25, leadingDistribution: TextLeadingDistribution.even,
                  locale: Locale.fromSubtags(languageCode: 'uk', scriptCode: 'Cyrl', countryCode: 'UA'),
                  shadows: <Shadow>[Shadow(color: Color(0x80445566), offset: Offset(-1, 2), blurRadius: 3)],
                  fontFeatures: <FontFeature>[FontFeature('smcp'), FontFeature('tnum', 0)],
                  fontVariations: <FontVariation>[FontVariation('wght', 650)],
                  decoration: TextDecoration.underline, decorationColor: Color(0xFF334455),
                  decorationStyle: TextDecorationStyle.wavy, decorationThickness: 1.2,
                  debugLabel: 'Complete badge style', fontFamily: 'Example',
                  fontFamilyFallback: <String>['Fallback One', 'Fallback Two'],
                  package: 'sample_fonts', overflow: TextOverflow.ellipsis);
                Widget completeLabel() => const Badge(
                  backgroundColor: Color(0xFF224466), textColor: Color(0xFFFEDCBA),
                  smallSize: 8.5, largeSize: 24, textStyle: completeBadgeStyle,
                  padding: EdgeInsetsDirectional.only(start: 3, top: 1, end: 7, bottom: 2),
                  alignment: AlignmentDirectional(-2, 1.5), offset: Offset(-6, 3),
                  label: Text('New'), isLabelVisible: true, child: Icon(Icons.mail));
                Widget completeCount() => Badge.count(
                  backgroundColor: const Color(0xFF224466), textColor: const Color(0xFFFEDCBA),
                  smallSize: 0, largeSize: 32.5, textStyle: completeBadgeStyle,
                  padding: const EdgeInsets.fromLTRB(1, 2, 3, 4),
                  alignment: const Alignment(2, -3), offset: const Offset(5, -8),
                  count: 1234, maxCount: 99, isLabelVisible: false, child: const Icon(Icons.mail));
                Widget inherited() => const BadgeTheme(data: BadgeThemeData(
                  backgroundColor: Color(0xFF123456), textColor: Color(0xFFFEDCBA),
                  smallSize: 7, largeSize: 19, textStyle: completeBadgeStyle,
                  padding: EdgeInsetsDirectional.only(start: 2, end: 9),
                  alignment: AlignmentDirectional.bottomStart, offset: Offset(1, -3)),
                  child: Badge(label: Text('Inherited'), child: Icon(Icons.mail)));
                Widget semanticTheme(BuildContext context) => Badge.count(count: 10,
                  backgroundColor: Theme.of(context).colorScheme.error,
                  textColor: Theme.of(context).colorScheme.onError,
                  textStyle: Theme.of(context).textTheme.labelSmall!.copyWith(
                    fontSize: 18, decoration: TextDecoration.combine(
                      [TextDecoration.underline, TextDecoration.overline, TextDecoration.lineThrough])));
                Widget paintStyle() => Badge(label: const Text('Custom paint'), textStyle: TextStyle(
                  foreground: Paint()..color = const Color(0xFF123456)..strokeWidth = 2..style = PaintingStyle.stroke,
                  background: Paint()..color = const Color(0x20334455),
                  shadows: const [], fontFeatures: const [], fontVariations: const [],
                  decoration: TextDecoration.none));
                Widget material2() => Theme(data: ThemeData(useMaterial3: false), child: const Badge());
                Widget material3() => Theme(data: ThemeData(useMaterial3: true), child: Badge.count(count: 999));
                List<Widget> boundaries() => <Widget>[
                  const Badge(), const Badge(label: Text('Label only')),
                  const Badge(child: Icon(Icons.mail)), const Badge(isLabelVisible: false),
                  const Badge(backgroundColor: null, textColor: null, smallSize: null, largeSize: null,
                    textStyle: null, padding: null, alignment: null, offset: null, label: null, child: null),
                  Badge.count(count: 0), Badge.count(count: 1, maxCount: 1),
                  Badge.count(count: 99, maxCount: 99), Badge.count(count: 100, maxCount: 99),
                  Badge.count(count: 9007199254740991, maxCount: 9007199254740991),
                  Badge.count(count: 9007199254740991, maxCount: 1),
                  const Badge(smallSize: 0, largeSize: 0, padding: EdgeInsets.zero),
                ];
                """;
        ArrayList<DartSymbolProbe> probes = new ArrayList<>();
        for (String symbol : List.of("Badge", "BadgeTheme", "BadgeThemeData", "TextStyle", "TextDecoration",
                "TextDecorationStyle", "TextOverflow", "TextBaseline", "TextLeadingDistribution", "FontWeight",
                "FontStyle", "Locale", "Shadow", "Offset", "Alignment", "AlignmentDirectional",
                "EdgeInsets", "EdgeInsetsDirectional", "Color", "Paint", "PaintingStyle")) {
            var occurrence = java.util.regex.Pattern.compile("\\b" + symbol + "\\b").matcher(candidate);
            assertTrue(occurrence.find(), symbol);
            probes.add(probe("badge-" + symbol, occurrence.start(), symbol, "package:flutter/material.dart", flutterSdk));
        }
        for (String member : List.of("Badge.count", "Locale.fromSubtags", "TextDecoration.combine")) {
            int separator = member.indexOf('.');
            probes.add(probe("badge-" + member, candidate.indexOf(member) + separator + 1,
                    member.substring(separator + 1), "package:flutter/material.dart", flutterSdk));
        }
        DartCandidateAnalysisResult passed = await(analyzer.analyze(request(projectRoot, file, candidate, 2200, probes)));
        assertEquals(DartCandidateAnalysisStatus.PASSED, passed.status(), () -> passed + " stderr=" + stderr);
        assertEquals(24, probes.size());
        assertEquals(probes.size(), passed.symbolEvidence().size());
        assertTrue(passed.symbolEvidence().stream().allMatch(DartSymbolEvidence::accepted));
        assertFalse(passed.diagnostics().stream().anyMatch(DartCandidateDiagnostic::blocking));
        long version = 2201;
        for (String invalid : List.of("const Badge.count(count: 1)", "Badge.count()", "Badge(count: 1)",
                "Badge(maxCount: 99)", "Badge.count(count: 1, label: const Text('Conflict'))",
                "Badge.count(count: 1.5)", "Badge.count(count: '1')", "Badge.count(count: null)",
                "Badge.count(count: 1, maxCount: 1.5)", "Badge.count(count: 1, maxCount: null)",
                "const Badge(backgroundColor: true)", "const Badge(textColor: 'red')",
                "const Badge(smallSize: '6')", "const Badge(largeSize: true)",
                "const Badge(textStyle: 'style')", "const Badge(padding: 4)",
                "const Badge(alignment: Offset.zero)", "const Badge(offset: Alignment.center)",
                "const Badge(isLabelVisible: null)", "const Badge(label: 'text')",
                "const Badge(child: false)", "const Badge(variant: 'count')",
                "const Badge(textStyle: TextStyle(fontSize: '12'))")) {
            String content = "import 'package:flutter/material.dart';\nWidget invalid() => " + invalid + ";\n";
            DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(projectRoot, file, content, version++, List.of())));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> invalid + ": " + rejected);
            assertTrue(rejected.diagnostics().stream().anyMatch(diagnostic -> diagnostic.blocking()
                    && diagnostic.severity() == DartCandidateDiagnosticSeverity.ERROR));
            assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        }
        // Badge.count is non-const: these assertions run at runtime, not in candidate analysis.
        // Negative sizes/padding also belong to layout validation, not Badge's constructor API.
        String runtimeAssertions = """
                import 'package:flutter/material.dart';
                List<Widget> runtimeOnly() => [Badge.count(count: -1), Badge.count(count: 0, maxCount: 0),
                  const Badge(smallSize: -1, largeSize: -2, padding: EdgeInsets.all(-1))];
                """;
        DartCandidateAnalysisResult runtimePassed = await(analyzer.analyze(request(
                projectRoot, file, runtimeAssertions, version++, List.of())));
        assertEquals(DartCandidateAnalysisStatus.PASSED, runtimePassed.status(), () -> runtimePassed.toString());
        assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        assertEquals(originalPubspec, Files.readString(pubspec, StandardCharsets.UTF_8));
        assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
    }

    @Test
    void validatesCircleAvatarAllFieldsProvidersCallbacksAndInfiniteRadii() throws Exception {
        Path executable = configuredDartExecutable();
        Path flutterSdk = configuredFlutter3448Sdk();
        Path coreLibrary = flutterSdk.resolve("bin/cache/pkg/sky_engine/lib/core");
        Path projectRoot = Files.createDirectories(workspace.resolve("circle_avatar_project"));
        Path dependencyRoot = Files.createDirectories(workspace.resolve("clipper_dependency"));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        writeFlutterPackageConfig(projectRoot, dependencyRoot, flutterSdk);
        Path file = lib.resolve("main.dart");
        String disk = "void main() {}\n";
        Files.writeString(file, disk, StandardCharsets.UTF_8);
        Path pubspec = projectRoot.resolve("pubspec.yaml");
        String originalPubspec = Files.readString(pubspec, StandardCharsets.UTF_8);
        List<String> stderr = new ArrayList<>();
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(executable,
                line -> { synchronized (stderr) { stderr.add(line); } });
        String candidate = """
                import 'package:flutter/material.dart';
                String unchangedUserCoreScope(String value, Object object) => value;
                void imageError(Object exception, StackTrace? stackTrace) {}
                Widget complete() => const CircleAvatar(
                  child: Text('VH'), backgroundColor: Color(0xFF123456),
                  backgroundImage: AssetImage('assets/fallback.png'),
                  foregroundImage: ResizeImage(ExactAssetImage('assets/avatar.png', scale: 2),
                    width: 64, height: 64, policy: ResizeImagePolicy.fit, allowUpscaling: false),
                  onBackgroundImageError: imageError, onForegroundImageError: imageError,
                  foregroundColor: Color(0xFFFEDCBA), minRadius: 12.5, maxRadius: 32);
                Widget semanticColors(BuildContext context) => CircleAvatar(
                  backgroundColor: Theme.of(context).colorScheme.primaryContainer,
                  foregroundColor: Theme.of(context).colorScheme.onPrimaryContainer,
                  radius: 20.5, child: const Icon(Icons.person));
                List<Widget> boundaries() => <Widget>[
                  const CircleAvatar(), const CircleAvatar(child: Text('Only child')),
                  const CircleAvatar(backgroundImage: AssetImage('assets/fallback.png')),
                  const CircleAvatar(foregroundImage: ExactAssetImage('avatar.png', package: 'avatars')),
                  const CircleAvatar(backgroundColor: null, foregroundColor: null,
                    backgroundImage: null, foregroundImage: null,
                    onBackgroundImageError: null, onForegroundImageError: null,
                    radius: null, minRadius: null, maxRadius: null, child: null),
                  const CircleAvatar(radius: 0), const CircleAvatar(minRadius: 0, maxRadius: 0),
                  const CircleAvatar(minRadius: 10), const CircleAvatar(maxRadius: 30),
                  const CircleAvatar(maxRadius: (1.0 / 0.0)),
                  const CircleAvatar(radius: (1.0 / 0.0)),
                  const CircleAvatar(minRadius: (1.0 / 0.0)),
                  const CircleAvatar(minRadius: (1.0 / 0.0), maxRadius: (1.0 / 0.0)),
                  const CircleAvatar(radius: 1.7976931348623157e308),
                  const CircleAvatar(minRadius: 1.7e308, maxRadius: 1.6e308),
                  Theme(data: ThemeData(useMaterial3: false), child: const CircleAvatar()),
                  Theme(data: ThemeData(useMaterial3: true), child: const CircleAvatar()),
                ];
                // Constant evaluation must take the null branch, otherwise CircleAvatar's
                // radius/minRadius assertion makes this entire candidate invalid.
                const exactPositiveInfinityProof = CircleAvatar(radius: 20,
                  minRadius: (1.0 / 0.0) == double.infinity && (1.0 / 0.0) > 0 ? null : 1);
                """;
        ArrayList<DartSymbolProbe> probes = new ArrayList<>();
        for (String symbol : List.of("CircleAvatar", "AssetImage", "ExactAssetImage", "ResizeImage",
                "ResizeImagePolicy", "Color", "Text", "Icon", "Theme", "ThemeData")) {
            var occurrence = Pattern.compile("\\b" + symbol + "\\b").matcher(candidate);
            assertTrue(occurrence.find(), symbol);
            probes.add(probe("circle-avatar-" + symbol, occurrence.start(), symbol,
                    "package:flutter/material.dart", flutterSdk));
        }
        // Prove the exact SDK identity used by the lowering proof. Generated radii
        // need no new core import: a closed constant expression avoids scope changes.
        int infinityOffset = candidate.indexOf("double.infinity");
        probes.add(probe("circle-avatar-double", infinityOffset,
                "double", "dart:core", coreLibrary));
        probes.add(probe("circle-avatar-infinity", infinityOffset + "double.".length(),
                "infinity", "dart:core", coreLibrary));
        DartCandidateAnalysisResult passed = await(analyzer.analyze(request(
                projectRoot, file, candidate, 2300, probes)));
        assertEquals(DartCandidateAnalysisStatus.PASSED, passed.status(), () -> passed + " stderr=" + stderr);
        assertEquals(12, probes.size());
        assertEquals(probes.size(), passed.symbolEvidence().size());
        assertTrue(passed.symbolEvidence().stream().allMatch(DartSymbolEvidence::accepted));
        long version = 2301;
        DartCandidateAnalysisResult lostImplicitCore = await(analyzer.analyze(request(
                projectRoot, file, "import 'dart:core' as core;\n" + candidate, version++, List.of())));
        assertEquals(DartCandidateAnalysisStatus.REJECTED, lostImplicitCore.status(),
                "A new prefixed core import alone must not silently remove String/Object from user scope");
        assertTrue(lostImplicitCore.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.blocking() && diagnostic.code().filter("undefined_class"::equals).isPresent()));
        for (String invalid : List.of(
                "const CircleAvatar(radius: 20, minRadius: 10)",
                "const CircleAvatar(radius: 20, maxRadius: 30)",
                "const CircleAvatar(onBackgroundImageError: imageError)",
                "const CircleAvatar(onForegroundImageError: imageError)",
                "const CircleAvatar(backgroundColor: true)",
                "const CircleAvatar(foregroundColor: 'red')",
                "const CircleAvatar(backgroundImage: 'asset.png')",
                "const CircleAvatar(foregroundImage: Color(0xFF000000))",
                "const CircleAvatar(radius: 'infinity')",
                "const CircleAvatar(minRadius: false)",
                "const CircleAvatar(maxRadius: Alignment.center)",
                "const CircleAvatar(child: 'VH')",
                "const CircleAvatar(textStyle: TextStyle())",
                "const CircleAvatar(backgroundImage: AssetImage('a.png'), onBackgroundImageError: wrongError)",
                "const CircleAvatar(foregroundImage: AssetImage('a.png'), onForegroundImageError: wrongError)")) {
            String content = "import 'package:flutter/material.dart';\n"
                    + "void imageError(Object exception, StackTrace? stackTrace) {}\n"
                    + "void wrongError() {}\nWidget invalid() => " + invalid + ";\n";
            DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(
                    projectRoot, file, content, version++, List.of())));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> invalid + ": " + rejected);
            assertTrue(rejected.diagnostics().stream().anyMatch(diagnostic -> diagnostic.blocking()
                    && diagnostic.severity() == DartCandidateDiagnosticSeverity.ERROR));
            assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        }
        // CircleAvatar's constructor does not validate its derived BoxConstraints;
        // negative, NaN and inverted finite sizes are layout failures, not analyzer errors.
        String runtimeOnly = """
                import 'package:flutter/material.dart';
                List<Widget> runtimeOnly() => [const CircleAvatar(radius: -1),
                  const CircleAvatar(minRadius: 20, maxRadius: 10),
                  const CircleAvatar(radius: double.nan)];
                """;
        DartCandidateAnalysisResult runtimePassed = await(analyzer.analyze(request(
                projectRoot, file, runtimeOnly, version, List.of())));
        assertEquals(DartCandidateAnalysisStatus.PASSED, runtimePassed.status(), () -> runtimePassed.toString());
        assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        assertEquals(originalPubspec, Files.readString(pubspec, StandardCharsets.UTF_8));
        assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
    }

    @Test
    void validatesLinearProgressAllFieldsNullableColorAnimationAndControllerProofs() throws Exception {
        Path executable = configuredDartExecutable();
        Path flutterSdk = configuredFlutter3448Sdk();
        Path projectRoot = Files.createDirectories(workspace.resolve("linear_progress_project"));
        Path dependencyRoot = Files.createDirectories(workspace.resolve("clipper_dependency"));
        Path dependencyLibrary = Files.createDirectories(dependencyRoot.resolve("lib"));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        writeFlutterPackageConfig(projectRoot, dependencyRoot, flutterSdk);
        Path importedFile = dependencyLibrary.resolve("progress.dart");
        String importedSource = """
                import 'package:flutter/widgets.dart';
                const Animation<Color?> importedColor = AlwaysStoppedAnimation<Color?>(null);
                late AnimationController importedController;
                """;
        Files.writeString(importedFile, importedSource, StandardCharsets.UTF_8);
        Path file = lib.resolve("main.dart");
        String disk = "void main() {}\n";
        Files.writeString(file, disk, StandardCharsets.UTF_8);
        Path pubspec = projectRoot.resolve("pubspec.yaml");
        String originalPubspec = Files.readString(pubspec, StandardCharsets.UTF_8);
        List<String> stderr = new ArrayList<>();
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(executable,
                line -> { synchronized (stderr) { stderr.add(line); } });
        String prelude = """
                import 'package:flutter/material.dart';
                import 'package:clipper_dependency/progress.dart' as project_progress;
                String unchangedCoreScope(String value, Object object) => value;
                const Animation<Color?> localColor = AlwaysStoppedAnimation<Color?>(null);
                Animation<Color?> colorFactory() => localColor;
                late AnimationController localController;
                AnimationController controllerFactory() => localController;
                dynamic dynamicColor = localColor;
                Animation<Color?>? nullableColor = localColor;
                Object objectColor = localColor;
                const Animation<Object?> wideColor = AlwaysStoppedAnimation<Object?>(null);
                dynamic dynamicController = null;
                AnimationController? nullableController;
                Object objectController = Object();
                void analyzerStaticTypeProofScope() {
                  // analyzer static-type proof insertion
                }
                """;
        String candidate = prelude + """
                Widget complete() => const LinearProgressIndicator(
                  value: 0.45, backgroundColor: Color(0xFF123456), color: Color(0xFF234567),
                  valueColor: AlwaysStoppedAnimation<Color>(Color(0xFFABCDEF)), minHeight: 6,
                  semanticsLabel: 'Upload', semanticsValue: '45%',
                  borderRadius: BorderRadiusDirectional.only(topStart: Radius.elliptical(2, 3)),
                  stopIndicatorColor: Color(0xFF987654), stopIndicatorRadius: 2, trackGap: 4,
                  year2023: false);
                Widget currentColor() => LinearProgressIndicator(valueColor: localColor);
                Widget factoryColor() => LinearProgressIndicator(valueColor: colorFactory());
                Widget currentController() => LinearProgressIndicator(controller: localController);
                Widget factoryController() => LinearProgressIndicator(controller: controllerFactory());
                Widget importedColor() => LinearProgressIndicator(valueColor: project_progress.importedColor);
                Widget importedController() => LinearProgressIndicator(controller: project_progress.importedController);
                Widget themeColors(BuildContext context) => LinearProgressIndicator(
                  color: Theme.of(context).colorScheme.primary,
                  backgroundColor: Theme.of(context).colorScheme.surfaceContainerHighest,
                  valueColor: AlwaysStoppedAnimation<Color>(Theme.of(context).colorScheme.secondary),
                  stopIndicatorColor: Theme.of(context).colorScheme.tertiary);
                List<Widget> boundaries() => <Widget>[
                  const LinearProgressIndicator(), const LinearProgressIndicator(value: 0),
                  const LinearProgressIndicator(value: 1), const LinearProgressIndicator(value: -1),
                  const LinearProgressIndicator(value: 2),
                  const LinearProgressIndicator(valueColor: AlwaysStoppedAnimation<Color?>(null)),
                  const LinearProgressIndicator(minHeight: (1.0 / 0.0)),
                  const LinearProgressIndicator(stopIndicatorRadius: (1.0 / 0.0), trackGap: (1.0 / 0.0)),
                  const LinearProgressIndicator(stopIndicatorRadius: -1, trackGap: -2),
                  const LinearProgressIndicator(borderRadius: BorderRadius.all(Radius.circular(9))),
                  const LinearProgressIndicator(value: null, backgroundColor: null, color: null,
                    valueColor: null, minHeight: null, semanticsLabel: null, semanticsValue: null,
                    borderRadius: null, stopIndicatorColor: null, stopIndicatorRadius: null,
                    trackGap: null, year2023: null, controller: null),
                  Theme(data: ThemeData(useMaterial3: false), child: const LinearProgressIndicator()),
                  Theme(data: ThemeData(useMaterial3: true), child: const LinearProgressIndicator(year2023: false)),
                ];
                """;
        ArrayList<DartSymbolProbe> probes = new ArrayList<>();
        for (String symbol : List.of("LinearProgressIndicator", "AlwaysStoppedAnimation", "Animation",
                "AnimationController", "Color", "BorderRadiusDirectional", "BorderRadius", "Radius",
                "Theme", "ThemeData")) {
            var occurrence = Pattern.compile("\\b" + symbol + "\\b").matcher(candidate);
            assertTrue(occurrence.find(), symbol);
            probes.add(probe("linear-progress-" + symbol, occurrence.start(), symbol,
                    "package:flutter/material.dart", flutterSdk));
        }
        for (String[] spec : List.of(
                new String[]{"localColor", "localColor", "valueColor", "Animation<Color?>"},
                new String[]{"colorFactory()", "colorFactory", "valueColor", "Animation<Color?>"},
                new String[]{"localController", "localController", "controller", "AnimationController"},
                new String[]{"controllerFactory()", "controllerFactory", "controller", "AnimationController"},
                new String[]{"project_progress.importedColor", "importedColor", "valueColor", "Animation<Color?>"},
                new String[]{"project_progress.importedController", "importedController", "controller", "AnimationController"})) {
            String expression = spec[0];
            int offset = candidate.indexOf(spec[2] + ": " + expression) + spec[2].length() + 2;
            boolean imported = expression.startsWith("project_progress.");
            probes.add(typedProbe("linear-reference-" + spec[1], offset + (imported ? "project_progress.".length() : 0),
                    spec[1], imported ? "package:clipper_dependency/progress.dart" : "project:current",
                    imported ? dependencyLibrary : lib, offset, expression.length(), candidate, spec[3]));
        }
        DartCandidateAnalysisResult passed = await(analyzer.analyze(request(projectRoot, file, candidate, 2400, probes)));
        assertEquals(DartCandidateAnalysisStatus.PASSED, passed.status(), () -> passed + " stderr=" + stderr);
        assertEquals(16, passed.symbolEvidence().size());
        assertTrue(passed.symbolEvidence().stream().allMatch(DartSymbolEvidence::accepted));
        assertEquals(6, passed.symbolEvidence().stream().filter(e -> e.staticTypeEvidence().isPresent()).count());
        long version = 2401;
        for (String[] spec : List.of(
                new String[]{"dynamicColor", "valueColor", "Animation<Color?>"},
                new String[]{"nullableColor", "valueColor", "Animation<Color?>"},
                new String[]{"objectColor", "valueColor", "Animation<Color?>"},
                new String[]{"wideColor", "valueColor", "Animation<Color?>"},
                new String[]{"dynamicController", "controller", "AnimationController"},
                new String[]{"nullableController", "controller", "AnimationController"},
                new String[]{"objectController", "controller", "AnimationController"})) {
            String invalid = "// ignore_for_file: argument_type_not_assignable, invalid_assignment\n" + prelude
                    + "Widget invalid() => LinearProgressIndicator(" + spec[1] + ": " + spec[0] + ");\n";
            int offset = invalid.indexOf(spec[0], invalid.indexOf("Widget invalid()"));
            DartSymbolProbe typeProbe = typedProbe("linear-invalid-" + spec[0], offset, spec[0],
                    "project:current", lib, offset, spec[0].length(), invalid, spec[2]);
            DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(
                    projectRoot, file, invalid, version++, List.of(typeProbe))));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> spec[0] + ": " + rejected);
            assertFalse(rejected.symbolEvidence().getFirst().staticTypeEvidence().orElseThrow().accepted());
        }
        for (String invalid : List.of(
                "const LinearProgressIndicator(minHeight: 0)", "const LinearProgressIndicator(minHeight: -1)",
                "const LinearProgressIndicator(minHeight: double.nan)", "const LinearProgressIndicator(value: true)",
                "const LinearProgressIndicator(backgroundColor: 'red')", "const LinearProgressIndicator(color: true)",
                "const LinearProgressIndicator(valueColor: Color(0xFF000000))",
                "const LinearProgressIndicator(semanticsLabel: 1)", "const LinearProgressIndicator(semanticsValue: false)",
                "const LinearProgressIndicator(borderRadius: Radius.circular(2))",
                "const LinearProgressIndicator(stopIndicatorColor: 0)", "const LinearProgressIndicator(stopIndicatorRadius: '2')",
                "const LinearProgressIndicator(trackGap: false)", "const LinearProgressIndicator(year2023: 2023)",
                "const LinearProgressIndicator(controller: AlwaysStoppedAnimation<double>(0))",
                "const LinearProgressIndicator(child: Text('No child'))")) {
            String content = "import 'package:flutter/material.dart';\nWidget invalid() => " + invalid + ";\n";
            DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(
                    projectRoot, file, content, version++, List.of())));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> invalid + ": " + rejected);
        }
        // Non-const assertions and context-dependent painter errors require the
        // model/Canvas guards, not a false claim that analysis proves layout.
        String runtimeOnly = prelude + """
                Widget conflicting() => LinearProgressIndicator(value: 0.5, controller: localController);
                Widget invalidSemanticsText() => const LinearProgressIndicator(value: 0.5, semanticsValue: 'Half done');
                Widget missingM2StopColor() => Theme(data: ThemeData(useMaterial3: false),
                  child: const LinearProgressIndicator(value: 0.5, year2023: false, stopIndicatorRadius: 2));
                """;
        DartCandidateAnalysisResult runtimePassed = await(analyzer.analyze(request(
                projectRoot, file, runtimeOnly, version, List.of())));
        assertEquals(DartCandidateAnalysisStatus.PASSED, runtimePassed.status(), () -> runtimePassed.toString());
        assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        assertEquals(originalPubspec, Files.readString(pubspec, StandardCharsets.UTF_8));
        assertEquals(importedSource, Files.readString(importedFile, StandardCharsets.UTF_8));
        assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
    }

    @Test
    void validatesCircularProgressBothConstructorsAllFieldsAndAnimationProofs() throws Exception {
        Path executable = configuredDartExecutable();
        Path flutterSdk = configuredFlutter3448Sdk();
        Path projectRoot = Files.createDirectories(workspace.resolve("circular_progress_project"));
        Path dependencyRoot = Files.createDirectories(workspace.resolve("clipper_dependency"));
        Path dependencyLibrary = Files.createDirectories(dependencyRoot.resolve("lib"));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        writeFlutterPackageConfig(projectRoot, dependencyRoot, flutterSdk);
        Path importedFile = dependencyLibrary.resolve("circular.dart");
        String importedSource = """
                import 'package:flutter/widgets.dart';
                const Animation<Color?> importedColor = AlwaysStoppedAnimation<Color?>(null);
                late AnimationController importedController;
                """;
        Files.writeString(importedFile, importedSource, StandardCharsets.UTF_8);
        Path file = lib.resolve("main.dart");
        String disk = "void main() {}\n";
        Files.writeString(file, disk, StandardCharsets.UTF_8);
        Path pubspec = projectRoot.resolve("pubspec.yaml");
        String originalPubspec = Files.readString(pubspec, StandardCharsets.UTF_8);
        List<String> stderr = new ArrayList<>();
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(executable,
                line -> { synchronized (stderr) { stderr.add(line); } });
        String prelude = """
                import 'package:flutter/material.dart';
                import 'package:clipper_dependency/circular.dart' as project_progress;
                String unchangedCoreScope(String value, Object object) => value;
                const Animation<Color?> localColor = AlwaysStoppedAnimation<Color?>(null);
                Animation<Color?> colorFactory() => localColor;
                late AnimationController localController;
                AnimationController controllerFactory() => localController;
                dynamic dynamicColor = localColor;
                Animation<Color?>? nullableColor = localColor;
                Object objectColor = localColor;
                const Animation<Object?> wideColor = AlwaysStoppedAnimation<Object?>(null);
                dynamic dynamicController = null;
                AnimationController? nullableController;
                Object objectController = Object();
                void analyzerStaticTypeProofScope() {
                  // analyzer static-type proof insertion
                }
                """;
        List<String[]> references = List.of(
                new String[]{"localColor", "localColor", "valueColor", "Animation<Color?>"},
                new String[]{"colorFactory()", "colorFactory", "valueColor", "Animation<Color?>"},
                new String[]{"localController", "localController", "controller", "AnimationController"},
                new String[]{"controllerFactory()", "controllerFactory", "controller", "AnimationController"},
                new String[]{"project_progress.importedColor", "importedColor", "valueColor", "Animation<Color?>"},
                new String[]{"project_progress.importedController", "importedController", "controller", "AnimationController"});
        StringBuilder valid = new StringBuilder(prelude);
        for (String variant : List.of("material", "adaptive")) {
            String constructor = variant.equals("material") ? "CircularProgressIndicator" : "CircularProgressIndicator.adaptive";
            String color = variant.equals("material") ? "color: Color(0xFF234567), " : "";
            valid.append("Widget ").append(variant).append("Complete() => const ").append(constructor)
                    .append("(value: 0.45, backgroundColor: Color(0xFF123456), ").append(color)
                    .append("valueColor: AlwaysStoppedAnimation<Color>(Color(0xFFABCDEF)), strokeWidth: 6, strokeAlign: -2, ")
                    .append("semanticsLabel: 'Upload', semanticsValue: '45%', strokeCap: StrokeCap.round, ")
                    .append("constraints: BoxConstraints(minWidth: 36, maxWidth: 80, minHeight: 40, maxHeight: 90), ")
                    .append("trackGap: 4, year2023: false, padding: EdgeInsetsDirectional.fromSTEB(1, 2, 3, 4));\n");
            for (String[] spec : references) {
                valid.append("Widget ").append(variant).append(spec[1]).append("() => ").append(constructor)
                        .append('(').append(spec[2]).append(": ").append(spec[0]).append(");\n");
            }
            valid.append("Widget ").append(variant).append("Theme(BuildContext context) => ").append(constructor)
                    .append("(backgroundColor: Theme.of(context).colorScheme.surfaceContainerHighest, ")
                    .append("valueColor: AlwaysStoppedAnimation<Color>(Theme.of(context).colorScheme.secondary));\n");
            valid.append("List<Widget> ").append(variant).append("Boundaries() => <Widget>[\n");
            for (String args : List.of("", "value: 0", "value: 1", "value: -2", "value: 3",
                    "valueColor: AlwaysStoppedAnimation<Color?>(null)",
                    "strokeWidth: -3, strokeAlign: 3, trackGap: -2", "strokeWidth: 0, strokeAlign: 0, trackGap: 0",
                    "strokeWidth: 1e308, strokeAlign: -1e308, trackGap: (1.0 / 0.0)",
                    "strokeCap: StrokeCap.butt, padding: EdgeInsets.fromLTRB(0, 2, 3, 4)",
                    "strokeCap: StrokeCap.square, constraints: BoxConstraints()",
                    "constraints: BoxConstraints(minWidth: (1.0 / 0.0), maxWidth: (1.0 / 0.0))")) {
                valid.append("const ").append(constructor).append('(').append(args).append("),\n");
            }
            valid.append("const ").append(constructor).append("(value: null, backgroundColor: null, ")
                    .append(variant.equals("material") ? "color: null, " : "")
                    .append("valueColor: null, strokeWidth: null, strokeAlign: null, semanticsLabel: null, semanticsValue: null, ")
                    .append("strokeCap: null, constraints: null, trackGap: null, year2023: null, padding: null, controller: null),\n");
            for (String platform : List.of("iOS", "macOS", "android", "windows")) {
                valid.append("Theme(data: ThemeData(platform: TargetPlatform.").append(platform)
                        .append(", useMaterial3: false), child: const ").append(constructor).append("(year2023: false)),\n");
            }
            valid.append("];\n");
        }
        valid.append("""
                Widget inherited() => const ProgressIndicatorTheme(
                  data: ProgressIndicatorThemeData(color: Color(0xFF123456), circularTrackColor: Color(0xFF234567),
                    strokeWidth: 3, strokeAlign: -2, strokeCap: StrokeCap.square, trackGap: 2, year2023: false,
                    constraints: BoxConstraints(minWidth: 40, minHeight: 40),
                    circularTrackPadding: EdgeInsetsDirectional.only(start: 2)),
                  child: CircularProgressIndicator());
                """);
        String candidate = valid.toString();
        ArrayList<DartSymbolProbe> probes = new ArrayList<>();
        for (String symbol : List.of("CircularProgressIndicator", "AlwaysStoppedAnimation", "Animation", "AnimationController",
                "Color", "StrokeCap", "BoxConstraints", "EdgeInsets", "EdgeInsetsDirectional", "Theme", "ThemeData",
                "TargetPlatform", "ProgressIndicatorTheme", "ProgressIndicatorThemeData")) {
            var occurrence = Pattern.compile("\\b" + symbol + "\\b").matcher(candidate);
            assertTrue(occurrence.find(), symbol);
            probes.add(probe("circular-" + symbol, occurrence.start(), symbol, "package:flutter/material.dart", flutterSdk));
        }
        probes.add(probe("circular-adaptive", candidate.indexOf("CircularProgressIndicator.adaptive")
                + "CircularProgressIndicator.".length(), "adaptive", "package:flutter/material.dart", flutterSdk));
        for (String variant : List.of("material", "adaptive")) {
            for (String[] spec : references) {
                String expression = spec[0];
                int methodOffset = candidate.indexOf("Widget " + variant + spec[1] + "()");
                int offset = candidate.indexOf(spec[2] + ": " + expression, methodOffset) + spec[2].length() + 2;
                boolean imported = expression.startsWith("project_progress.");
                probes.add(typedProbe("circular-" + variant + '-' + spec[1],
                        offset + (imported ? "project_progress.".length() : 0), spec[1],
                        imported ? "package:clipper_dependency/circular.dart" : "project:current",
                        imported ? dependencyLibrary : lib, offset, expression.length(), candidate, spec[3]));
            }
        }
        DartCandidateAnalysisResult passed = await(analyzer.analyze(request(projectRoot, file, candidate, 2500, probes)));
        assertEquals(DartCandidateAnalysisStatus.PASSED, passed.status(), () -> passed + " stderr=" + stderr);
        assertEquals(27, passed.symbolEvidence().size());
        assertTrue(passed.symbolEvidence().stream().allMatch(DartSymbolEvidence::accepted));
        assertEquals(12, passed.symbolEvidence().stream().filter(e -> e.staticTypeEvidence().isPresent()).count());
        long version = 2501;
        for (String constructor : List.of("CircularProgressIndicator", "CircularProgressIndicator.adaptive")) {
            for (String[] spec : List.of(
                    new String[]{"dynamicColor", "valueColor", "Animation<Color?>"},
                    new String[]{"nullableColor", "valueColor", "Animation<Color?>"},
                    new String[]{"objectColor", "valueColor", "Animation<Color?>"},
                    new String[]{"wideColor", "valueColor", "Animation<Color?>"},
                    new String[]{"dynamicController", "controller", "AnimationController"},
                    new String[]{"nullableController", "controller", "AnimationController"},
                    new String[]{"objectController", "controller", "AnimationController"})) {
                String invalid = "// ignore_for_file: argument_type_not_assignable, invalid_assignment\n" + prelude
                        + "Widget invalid() => " + constructor + '(' + spec[1] + ": " + spec[0] + ");\n";
                int offset = invalid.indexOf(spec[0], invalid.indexOf("Widget invalid()"));
                DartSymbolProbe typeProbe = typedProbe("circular-invalid-" + spec[0], offset, spec[0],
                        "project:current", lib, offset, spec[0].length(), invalid, spec[2]);
                DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(
                        projectRoot, file, invalid, version++, List.of(typeProbe))));
                assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> constructor + ' ' + spec[0] + ": " + rejected);
                assertFalse(rejected.symbolEvidence().getFirst().staticTypeEvidence().orElseThrow().accepted());
            }
        }
        for (String invalid : List.of(
                "const CircularProgressIndicator(value: true)", "const CircularProgressIndicator(backgroundColor: 'red')",
                "const CircularProgressIndicator(color: true)", "const CircularProgressIndicator(valueColor: Color(0xFF000000))",
                "const CircularProgressIndicator(strokeWidth: '4')", "const CircularProgressIndicator(strokeAlign: false)",
                "const CircularProgressIndicator(semanticsLabel: 1)", "const CircularProgressIndicator(semanticsValue: false)",
                "const CircularProgressIndicator(strokeCap: Clip.none)", "const CircularProgressIndicator(constraints: Size(20, 20))",
                "const CircularProgressIndicator(trackGap: true)", "const CircularProgressIndicator(year2023: 2023)",
                "const CircularProgressIndicator(padding: 4)",
                "const CircularProgressIndicator(controller: AlwaysStoppedAnimation<double>(0))",
                "const CircularProgressIndicator.adaptive(color: Color(0xFF000000))",
                "const CircularProgressIndicator.adaptive(color: null)",
                "const CircularProgressIndicator(variant: 'adaptive')", "const CircularProgressIndicator.material()",
                "const CircularProgressIndicator(child: Text('No child'))")) {
            String content = "import 'package:flutter/material.dart';\nWidget invalid() => " + invalid + ";\n";
            DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(projectRoot, file, content, version++, List.of())));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> invalid + ": " + rejected);
        }
        // Static analysis cannot prove contextual layout, semantics or non-const
        // constructor assertions; the model and resolved Canvas guards remain necessary.
        String runtimeOnly = prelude + """
                Widget conflict() => CircularProgressIndicator(value: 0.5, controller: localController);
                Widget adaptiveConflict() => CircularProgressIndicator.adaptive(value: 0.5, controller: localController);
                Widget materialSemantics() => const CircularProgressIndicator(value: 0.5, semanticsValue: 'Half done');
                Widget adaptiveSemantics() => const CircularProgressIndicator.adaptive(value: 0.5, semanticsValue: 'Half done');
                Widget paintOverflow() => const CircularProgressIndicator(strokeWidth: 1e308, strokeAlign: 1e308);
                """;
        DartCandidateAnalysisResult runtimePassed = await(analyzer.analyze(request(projectRoot, file, runtimeOnly, version, List.of())));
        assertEquals(DartCandidateAnalysisStatus.PASSED, runtimePassed.status(), () -> runtimePassed.toString());
        assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        assertEquals(originalPubspec, Files.readString(pubspec, StandardCharsets.UTF_8));
        assertEquals(importedSource, Files.readString(importedFile, StandardCharsets.UTF_8));
        assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
    }

    @Test
    void validatesRefreshProgressAllFieldsNullableStrokeWidthAndColorAnimationProofs() throws Exception {
        Path executable = configuredDartExecutable();
        Path flutterSdk = configuredFlutter3448Sdk();
        Path projectRoot = Files.createDirectories(workspace.resolve("refresh_progress_project"));
        Path dependencyRoot = Files.createDirectories(workspace.resolve("clipper_dependency"));
        Path dependencyLibrary = Files.createDirectories(dependencyRoot.resolve("lib"));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        writeFlutterPackageConfig(projectRoot, dependencyRoot, flutterSdk);
        Path importedFile = dependencyLibrary.resolve("refresh.dart");
        String importedSource = """
                import 'package:flutter/widgets.dart';
                const Animation<Color?> importedColor = AlwaysStoppedAnimation<Color?>(null);
                """;
        Files.writeString(importedFile, importedSource, StandardCharsets.UTF_8);
        Path file = lib.resolve("main.dart");
        String disk = "void main() {}\n";
        Files.writeString(file, disk, StandardCharsets.UTF_8);
        Path pubspec = projectRoot.resolve("pubspec.yaml");
        String originalPubspec = Files.readString(pubspec, StandardCharsets.UTF_8);
        List<String> stderr = new ArrayList<>();
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(executable,
                line -> { synchronized (stderr) { stderr.add(line); } });
        String prelude = """
                import 'package:flutter/material.dart';
                import 'package:clipper_dependency/refresh.dart' as project_progress;
                String unchangedCoreScope(String value, Object object) => value;
                const Animation<Color?> localColor = AlwaysStoppedAnimation<Color?>(null);
                Animation<Color?> colorFactory() => localColor;
                dynamic dynamicColor = localColor;
                Animation<Color?>? nullableColor = localColor;
                Object objectColor = localColor;
                const Animation<Object?> wideColor = AlwaysStoppedAnimation<Object?>(null);
                void analyzerStaticTypeProofScope() {
                  // analyzer static-type proof insertion
                }
                """;
        StringBuilder valid = new StringBuilder(prelude).append("""
                Widget complete() => const RefreshProgressIndicator(
                  value: 0.45, backgroundColor: Color(0xFF123456), color: Color(0xFF234567),
                  valueColor: AlwaysStoppedAnimation<Color>(Color(0x80ABCDEF)), strokeWidth: 6, strokeAlign: -2,
                  semanticsLabel: 'Refresh', semanticsValue: '45%', strokeCap: StrokeCap.round, elevation: 3,
                  indicatorMargin: EdgeInsets.fromLTRB(1, 2, 3, 4),
                  indicatorPadding: EdgeInsetsDirectional.fromSTEB(1, 2, 3, 4));
                Widget themeColor(BuildContext context) => RefreshProgressIndicator(
                  backgroundColor: Theme.of(context).colorScheme.surfaceContainerHighest,
                  valueColor: AlwaysStoppedAnimation<Color>(Theme.of(context).colorScheme.secondary));
                List<Widget> inheritedWidths() => const <Widget>[
                  ProgressIndicatorTheme(data: ProgressIndicatorThemeData(strokeWidth: 7),
                    child: RefreshProgressIndicator()),
                  ProgressIndicatorTheme(data: ProgressIndicatorThemeData(strokeWidth: 7),
                    child: RefreshProgressIndicator(strokeWidth: null)),
                  RefreshProgressIndicator(strokeWidth: RefreshProgressIndicator.defaultStrokeWidth),
                ];
                List<Widget> materialThemes() => <Widget>[
                  Theme(data: ThemeData(useMaterial3: false), child: const RefreshProgressIndicator()),
                  Theme(data: ThemeData(useMaterial3: true), child: const RefreshProgressIndicator()),
                ];
                """);
        List<String[]> references = List.of(
                new String[]{"localColor", "localColor"},
                new String[]{"colorFactory()", "colorFactory"},
                new String[]{"project_progress.importedColor", "importedColor"});
        for (String[] spec : references) {
            valid.append("Widget reference").append(spec[1]).append("() => RefreshProgressIndicator(valueColor: ")
                    .append(spec[0]).append(");\n");
        }
        valid.append("List<Widget> boundaries() => const <Widget>[\n");
        for (String args : List.of("", "value: 0", "value: 1", "value: -2", "value: 3",
                "valueColor: AlwaysStoppedAnimation<Color?>(null)",
                "strokeWidth: null", "strokeWidth: -3, strokeAlign: 3", "strokeWidth: 0, strokeAlign: 0",
                "strokeWidth: 1e308, strokeAlign: -1e308", "elevation: 0",
                "strokeCap: StrokeCap.butt, indicatorMargin: EdgeInsets.zero, indicatorPadding: EdgeInsets.zero",
                "strokeCap: StrokeCap.square, indicatorMargin: EdgeInsetsDirectional.only(start: 2), "
                        + "indicatorPadding: EdgeInsets.only(left: 2)",
                "value: null, backgroundColor: null, color: null, valueColor: null, strokeWidth: null, "
                        + "strokeAlign: null, semanticsLabel: null, semanticsValue: null, strokeCap: null")) {
            valid.append("RefreshProgressIndicator(").append(args).append("),\n");
        }
        valid.append("];\n");
        String candidate = valid.toString();
        ArrayList<DartSymbolProbe> probes = new ArrayList<>();
        for (String symbol : List.of("RefreshProgressIndicator", "AlwaysStoppedAnimation", "Animation", "Color",
                "StrokeCap", "EdgeInsets", "EdgeInsetsDirectional", "Theme", "ThemeData", "ProgressIndicatorTheme",
                "ProgressIndicatorThemeData")) {
            var occurrence = Pattern.compile("\\b" + symbol + "\\b").matcher(candidate);
            assertTrue(occurrence.find(), symbol);
            probes.add(probe("refresh-" + symbol, occurrence.start(), symbol, "package:flutter/material.dart", flutterSdk));
        }
        probes.add(probe("refresh-defaultStrokeWidth", candidate.indexOf("RefreshProgressIndicator.defaultStrokeWidth")
                + "RefreshProgressIndicator.".length(), "defaultStrokeWidth", "package:flutter/material.dart", flutterSdk));
        for (String[] spec : references) {
            String expression = spec[0];
            int methodOffset = candidate.indexOf("Widget reference" + spec[1] + "()");
            int offset = candidate.indexOf("valueColor: " + expression, methodOffset) + "valueColor: ".length();
            boolean imported = expression.startsWith("project_progress.");
            probes.add(typedProbe("refresh-reference-" + spec[1],
                    offset + (imported ? "project_progress.".length() : 0), spec[1],
                    imported ? "package:clipper_dependency/refresh.dart" : "project:current",
                    imported ? dependencyLibrary : lib, offset, expression.length(), candidate, "Animation<Color?>"));
        }
        DartCandidateAnalysisResult passed = await(analyzer.analyze(request(projectRoot, file, candidate, 2600, probes)));
        assertEquals(DartCandidateAnalysisStatus.PASSED, passed.status(), () -> passed + " stderr=" + stderr);
        assertEquals(15, passed.symbolEvidence().size());
        assertTrue(passed.symbolEvidence().stream().allMatch(DartSymbolEvidence::accepted));
        assertEquals(3, passed.symbolEvidence().stream().filter(e -> e.staticTypeEvidence().isPresent()).count());
        long version = 2601;
        for (String symbol : List.of("dynamicColor", "nullableColor", "objectColor", "wideColor")) {
            String invalid = "// ignore_for_file: argument_type_not_assignable, invalid_assignment\n" + prelude
                    + "Widget invalid() => RefreshProgressIndicator(valueColor: " + symbol + ");\n";
            int offset = invalid.indexOf(symbol, invalid.indexOf("Widget invalid()"));
            DartSymbolProbe typeProbe = typedProbe("refresh-invalid-" + symbol, offset, symbol,
                    "project:current", lib, offset, symbol.length(), invalid, "Animation<Color?>");
            DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(
                    projectRoot, file, invalid, version++, List.of(typeProbe))));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> symbol + ": " + rejected);
            assertFalse(rejected.symbolEvidence().getFirst().staticTypeEvidence().orElseThrow().accepted());
        }
        for (String invalid : List.of(
                "RefreshProgressIndicator(value: true)", "RefreshProgressIndicator(backgroundColor: 'red')",
                "RefreshProgressIndicator(color: true)", "RefreshProgressIndicator(valueColor: Color(0xFF000000))",
                "RefreshProgressIndicator(strokeWidth: '4')", "RefreshProgressIndicator(strokeAlign: false)",
                "RefreshProgressIndicator(semanticsLabel: 1)", "RefreshProgressIndicator(semanticsValue: false)",
                "RefreshProgressIndicator(strokeCap: Clip.none)", "RefreshProgressIndicator(elevation: null)",
                "RefreshProgressIndicator(indicatorMargin: null)", "RefreshProgressIndicator(indicatorPadding: null)",
                "RefreshProgressIndicator(indicatorMargin: 4)", "RefreshProgressIndicator(indicatorPadding: Radius.circular(2))",
                "RefreshProgressIndicator(controller: null)", "RefreshProgressIndicator(year2023: null)",
                "RefreshProgressIndicator(padding: null)", "RefreshProgressIndicator(constraints: null)",
                "RefreshProgressIndicator(trackGap: null)", "RefreshProgressIndicator(variant: 'adaptive')",
                "RefreshProgressIndicator.adaptive()", "RefreshProgressIndicator(child: Text('No child'))")) {
            String content = "import 'package:flutter/material.dart';\nWidget invalid() => const " + invalid + ";\n";
            DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(projectRoot, file, content, version++, List.of())));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> invalid + ": " + rejected);
        }
        // Source analysis alone cannot prove downstream Material/paint/layout
        // assertions. These are accepted Dart, but need model or Canvas guards.
        String runtimeOnly = prelude + """
                Widget invalidSemantics() => const RefreshProgressIndicator(value: 0.5, semanticsValue: 'Half done');
                Widget negativeElevation() => const RefreshProgressIndicator(elevation: -1);
                Widget negativeMargin() => const RefreshProgressIndicator(indicatorMargin: EdgeInsets.all(-1));
                Widget negativePadding() => const RefreshProgressIndicator(indicatorPadding: EdgeInsets.all(-1));
                Widget paintOverflow() => const RefreshProgressIndicator(strokeWidth: 1e308, strokeAlign: 1e308);
                Widget nonFinitePaint() => const RefreshProgressIndicator(strokeWidth: double.infinity);
                Widget rectangularArrow() => const RefreshProgressIndicator(value: 0.5,
                  indicatorPadding: EdgeInsets.only(left: 2));
                """;
        DartCandidateAnalysisResult runtimePassed = await(analyzer.analyze(request(projectRoot, file, runtimeOnly, version, List.of())));
        assertEquals(DartCandidateAnalysisStatus.PASSED, runtimePassed.status(), () -> runtimePassed.toString());
        assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        assertEquals(originalPubspec, Files.readString(pubspec, StandardCharsets.UTF_8));
        assertEquals(importedSource, Files.readString(importedFile, StandardCharsets.UTF_8));
        assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
    }

    @Test
    void validatesRefreshIndicatorAllConstructorsAndTypedFunctionContracts() throws Exception {
        Path executable = configuredDartExecutable();
        Path flutterSdk = configuredFlutter3448Sdk();
        Path projectRoot = Files.createDirectories(workspace.resolve("refresh_indicator_project"));
        Path dependencyRoot = Files.createDirectories(workspace.resolve("clipper_dependency"));
        Path dependencyLibrary = Files.createDirectories(dependencyRoot.resolve("lib"));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        writeFlutterPackageConfig(projectRoot, dependencyRoot, flutterSdk);
        Path importedFile = dependencyLibrary.resolve("refresh_wrapper.dart");
        String importedSource = """
                import 'package:flutter/material.dart';
                Future<void> importedRefresh() async {}
                bool importedPredicate(ScrollNotification notification) => notification.depth == 1;
                void importedStatus(RefreshIndicatorStatus? status) {}
                """;
        Files.writeString(importedFile, importedSource, StandardCharsets.UTF_8);
        Path file = lib.resolve("main.dart");
        String disk = "void main() {}\n";
        Files.writeString(file, disk, StandardCharsets.UTF_8);
        Path pubspec = projectRoot.resolve("pubspec.yaml");
        String originalPubspec = Files.readString(pubspec, StandardCharsets.UTF_8);
        List<String> stderr = new ArrayList<>();
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(executable,
                line -> { synchronized (stderr) { stderr.add(line); } });
        String prelude = """
                import 'package:flutter/material.dart';
                import 'package:clipper_dependency/refresh_wrapper.dart' as project_refresh;
                String unchangedCoreScope(String value, Object object) => value;
                Future<void> localRefresh() async {}
                RefreshCallback refreshFactory() => localRefresh;
                bool localPredicate(ScrollNotification notification) => notification.depth == 1;
                ScrollNotificationPredicate predicateFactory() => localPredicate;
                void localStatus(RefreshIndicatorStatus? status) {}
                ValueChanged<RefreshIndicatorStatus?> statusFactory() => localStatus;
                dynamic dynamicRefresh = localRefresh;
                RefreshCallback? nullableRefresh;
                void synchronousRefresh() {}
                dynamic dynamicPredicate = localPredicate;
                ScrollNotificationPredicate? nullablePredicate;
                bool? nullablePredicateResult(ScrollNotification notification) => null;
                bool wrongPredicateArgument(String notification) => true;
                dynamic dynamicStatus = localStatus;
                ValueChanged<RefreshIndicatorStatus?>? nullableStatus;
                void narrowStatus(RefreshIndicatorStatus status) {}
                void wrongStatusArgument(String? status) {}
                const Animation<Color?> localAnimation = AlwaysStoppedAnimation<Color?>(null);
                const CustomClipper<RRect> localClipper = RefreshProofClipper();
                class RefreshProofClipper extends CustomClipper<RRect> {
                  const RefreshProofClipper();
                  @override
                  RRect getClip(Size size) => RRect.fromRectAndRadius(Offset.zero & size, Radius.zero);
                  @override
                  bool shouldReclip(covariant RefreshProofClipper oldClipper) => false;
                }
                void analyzerStaticTypeProofScope() {
                  // analyzer static-type proof insertion
                }
                """;
        List<String[]> references = List.of(
                new String[]{"localRefresh", "localRefresh", "onRefresh", "RefreshCallback"},
                new String[]{"refreshFactory()", "refreshFactory", "onRefresh", "RefreshCallback"},
                new String[]{"project_refresh.importedRefresh", "importedRefresh", "onRefresh", "RefreshCallback"},
                new String[]{"localPredicate", "localPredicate", "notificationPredicate", "ScrollNotificationPredicate"},
                new String[]{"predicateFactory()", "predicateFactory", "notificationPredicate", "ScrollNotificationPredicate"},
                new String[]{"project_refresh.importedPredicate", "importedPredicate", "notificationPredicate", "ScrollNotificationPredicate"},
                new String[]{"localStatus", "localStatus", "onStatusChange", "ValueChanged<RefreshIndicatorStatus?>"},
                new String[]{"statusFactory()", "statusFactory", "onStatusChange", "ValueChanged<RefreshIndicatorStatus?>"},
                new String[]{"project_refresh.importedStatus", "importedStatus", "onStatusChange", "ValueChanged<RefreshIndicatorStatus?>"});
        StringBuilder valid = new StringBuilder(prelude);
        for (String variant : List.of("material", "adaptive", "noSpinner")) {
            String constructor = variant.equals("material") ? "RefreshIndicator" : "RefreshIndicator." + variant;
            valid.append("Widget ").append(variant).append("Complete() => const ").append(constructor)
                    .append("(onRefresh: localRefresh, notificationPredicate: defaultScrollNotificationPredicate, ")
                    .append("semanticsLabel: 'Refresh list', semanticsValue: '45%', triggerMode: RefreshIndicatorTriggerMode.anywhere, ")
                    .append("elevation: 3, child: SizedBox(width: 240, height: 320), ")
                    .append(variant.equals("noSpinner") ? "onStatusChange: localStatus" :
                            "displacement: 48, edgeOffset: -4, color: Color(0xFF123456), backgroundColor: Color(0xFFABCDEF), strokeWidth: -2")
                    .append(");\n");
            valid.append("Widget ").append(variant).append("Prototype() => ").append(constructor)
                    .append("(onRefresh: () async {}, child: ListView(physics: const AlwaysScrollableScrollPhysics()));\n");
            for (String[] spec : references) {
                if (spec[2].equals("onStatusChange") && !variant.equals("noSpinner")) continue;
                valid.append("Widget ").append(variant).append(spec[1]).append("() => ").append(constructor).append('(')
                        .append(spec[2].equals("onRefresh") ? "" : "onRefresh: localRefresh, ")
                        .append(spec[2]).append(": ").append(spec[0]).append(", child: const SizedBox());\n");
            }
            for (String predicate : List.of("(notification) => notification.depth == 0", "(_) => true")) {
                valid.append("Widget ").append(variant).append(predicate.startsWith("(_)") ? "All" : "DepthZero")
                        .append("() => ").append(constructor).append("(onRefresh: localRefresh, notificationPredicate: ")
                        .append(predicate).append(", child: const SizedBox());\n");
            }
            valid.append("Widget ").append(variant).append("Theme(BuildContext context) => Theme(data: ThemeData(platform: TargetPlatform.iOS), child: ")
                    .append(constructor).append("(onRefresh: localRefresh, child: const SizedBox(), ")
                    .append(variant.equals("noSpinner") ? "onStatusChange: null" : "color: Theme.of(context).colorScheme.primary")
                    .append("));\n");
        }
        valid.append("""
                List<Widget> boundaries() => const <Widget>[
                  RefreshIndicator(onRefresh: localRefresh, child: SizedBox(), displacement: 0, edgeOffset: -1e308, strokeWidth: 0, elevation: 0),
                  RefreshIndicator.adaptive(onRefresh: localRefresh, child: SizedBox(), displacement: 1e308, edgeOffset: 1e308, strokeWidth: -1e308),
                  RefreshIndicator.noSpinner(onRefresh: localRefresh, child: SizedBox(), onStatusChange: null,
                    semanticsLabel: null, semanticsValue: null, triggerMode: RefreshIndicatorTriggerMode.onEdge),
                ];
                Widget mixedReferences() => RefreshIndicator(onRefresh: localRefresh,
                  child: ClipRRect(clipper: localClipper,
                    child: CircularProgressIndicator(valueColor: localAnimation)));
                """);
        String candidate = valid.toString();
        ArrayList<DartSymbolProbe> probes = new ArrayList<>();
        for (String symbol : List.of("RefreshIndicator", "RefreshCallback", "ScrollNotification", "ScrollNotificationPredicate",
                "RefreshIndicatorStatus", "ValueChanged", "RefreshIndicatorTriggerMode", "defaultScrollNotificationPredicate",
                "Color", "SizedBox", "ListView", "AlwaysScrollableScrollPhysics", "Theme", "ThemeData", "TargetPlatform")) {
            var occurrence = Pattern.compile("\\b" + symbol + "\\b").matcher(candidate);
            assertTrue(occurrence.find(), symbol);
            probes.add(probe("refresh-wrapper-" + symbol, occurrence.start(), symbol, "package:flutter/material.dart", flutterSdk));
        }
        for (String member : List.of("adaptive", "noSpinner")) {
            probes.add(probe("refresh-wrapper-" + member, candidate.indexOf("RefreshIndicator." + member)
                    + "RefreshIndicator.".length(), member, "package:flutter/material.dart", flutterSdk));
        }
        for (String variant : List.of("material", "adaptive", "noSpinner")) {
            for (String[] spec : references) {
                if (spec[2].equals("onStatusChange") && !variant.equals("noSpinner")) continue;
                int methodOffset = candidate.indexOf("Widget " + variant + spec[1] + "()");
                int offset = candidate.indexOf(spec[2] + ": " + spec[0], methodOffset) + spec[2].length() + 2;
                boolean imported = spec[0].startsWith("project_refresh.");
                probes.add(typedProbe("refresh-wrapper-" + variant + '-' + spec[1],
                        offset + (imported ? "project_refresh.".length() : 0), spec[1],
                        imported ? "package:clipper_dependency/refresh_wrapper.dart" : "project:current",
                        imported ? dependencyLibrary : lib, offset, spec[0].length(), candidate, spec[3], "package:flutter/material.dart"));
            }
        }
        for (String[] spec : List.of(new String[]{"localAnimation", "Animation<Color?>"},
                new String[]{"localClipper", "CustomClipper<RRect>"})) {
            int offset = candidate.indexOf(spec[0], candidate.indexOf("Widget mixedReferences()"));
            probes.add(0, typedProbe("refresh-wrapper-mixed-" + spec[0], offset, spec[0], "project:current", lib,
                    offset, spec[0].length(), candidate, spec[1], "package:flutter/material.dart"));
        }
        DartCandidateAnalysisResult passed = await(analyzer.analyze(request(projectRoot, file, candidate, 2700, probes)));
        assertEquals(DartCandidateAnalysisStatus.PASSED, passed.status(), () -> passed + " stderr=" + stderr);
        assertEquals(40, passed.symbolEvidence().size());
        assertTrue(passed.symbolEvidence().stream().allMatch(DartSymbolEvidence::accepted));
        assertEquals(23, passed.symbolEvidence().stream().filter(e -> e.staticTypeEvidence().isPresent()).count());
        long version = 2701;
        for (String[] spec : List.of(
                new String[]{"dynamicRefresh", "onRefresh", "RefreshCallback"},
                new String[]{"nullableRefresh", "onRefresh", "RefreshCallback"},
                new String[]{"synchronousRefresh", "onRefresh", "RefreshCallback"},
                new String[]{"dynamicPredicate", "notificationPredicate", "ScrollNotificationPredicate"},
                new String[]{"nullablePredicate", "notificationPredicate", "ScrollNotificationPredicate"},
                new String[]{"nullablePredicateResult", "notificationPredicate", "ScrollNotificationPredicate"},
                new String[]{"wrongPredicateArgument", "notificationPredicate", "ScrollNotificationPredicate"},
                new String[]{"dynamicStatus", "onStatusChange", "ValueChanged<RefreshIndicatorStatus?>"},
                new String[]{"nullableStatus", "onStatusChange", "ValueChanged<RefreshIndicatorStatus?>"},
                new String[]{"narrowStatus", "onStatusChange", "ValueChanged<RefreshIndicatorStatus?>"},
                new String[]{"wrongStatusArgument", "onStatusChange", "ValueChanged<RefreshIndicatorStatus?>"})) {
            String invalid = "// ignore_for_file: argument_type_not_assignable, invalid_assignment\n" + prelude
                    + "Widget invalid() => RefreshIndicator.noSpinner("
                    + (spec[1].equals("onRefresh") ? "" : "onRefresh: localRefresh, ")
                    + spec[1] + ": " + spec[0] + ", child: const SizedBox());\n";
            int offset = invalid.indexOf(spec[0], invalid.indexOf("Widget invalid()"));
            DartSymbolProbe typeProbe = typedProbe("refresh-wrapper-invalid-" + spec[0], offset, spec[0],
                    "project:current", lib, offset, spec[0].length(), invalid, spec[2], "package:flutter/material.dart");
            DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(projectRoot, file, invalid, version++, List.of(typeProbe))));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> spec[0] + ": " + rejected);
            assertFalse(rejected.symbolEvidence().getFirst().staticTypeEvidence().orElseThrow().accepted());
        }
        for (String args : List.of("displacement: null", "edgeOffset: null", "onRefresh: null", "color: true",
                "backgroundColor: 'red'", "notificationPredicate: null", "semanticsLabel: 1", "semanticsValue: false",
                "strokeWidth: null", "triggerMode: null", "triggerMode: Axis.vertical", "elevation: -1", "elevation: null",
                "onStatusChange: localStatus", "variant: 'adaptive'")) {
            String invalid = prelude + "Widget invalid() => const RefreshIndicator("
                    + (args.startsWith("onRefresh:") ? "" : "onRefresh: localRefresh, ") + args + ", child: SizedBox());\n";
            DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(projectRoot, file, invalid, version++, List.of())));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> args + ": " + rejected);
        }
        for (String args : List.of("displacement: 0", "edgeOffset: 0", "color: null", "backgroundColor: null", "strokeWidth: 0")) {
            String invalid = prelude + "Widget invalid() => const RefreshIndicator.noSpinner(onRefresh: localRefresh, "
                    + args + ", child: SizedBox());\n";
            DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(projectRoot, file, invalid, version++, List.of())));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> args + ": " + rejected);
        }
        for (String constructor : List.of("RefreshIndicator", "RefreshIndicator.adaptive", "RefreshIndicator.noSpinner")) {
            for (String args : List.of("child: SizedBox()", "onRefresh: localRefresh", "onRefresh: localRefresh, child: null")) {
                String invalid = prelude + "Widget invalid() => const " + constructor + '(' + args + ");\n";
                DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(projectRoot, file, invalid, version++, List.of())));
                assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> constructor + ' ' + args + ": " + rejected);
            }
        }
        String runtimeOnly = prelude + """
                Widget negativeDisplacement() => const RefreshIndicator(onRefresh: localRefresh, child: SizedBox(), displacement: -1);
                Widget nonConstElevation() => RefreshIndicator(onRefresh: localRefresh, child: const SizedBox(), elevation: -1);
                Widget infiniteGeometry() => const RefreshIndicator(onRefresh: localRefresh, child: SizedBox(), displacement: double.infinity, edgeOffset: double.infinity, strokeWidth: double.infinity);
                Widget activeSemantics() => const RefreshIndicator(onRefresh: localRefresh, child: SizedBox(), semanticsValue: 'Refreshing items');
                """;
        DartCandidateAnalysisResult runtimePassed = await(analyzer.analyze(request(projectRoot, file, runtimeOnly, version, List.of())));
        assertEquals(DartCandidateAnalysisStatus.PASSED, runtimePassed.status(), () -> runtimePassed.toString());
        assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        assertEquals(originalPubspec, Files.readString(pubspec, StandardCharsets.UTF_8));
        assertEquals(importedSource, Files.readString(importedFile, StandardCharsets.UTF_8));
        assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
    }

    @Test
    void validatesTextButtonConstructorsCompleteStyleAndTypedReferences() throws Exception {
        Path executable = configuredDartExecutable();
        Path flutterSdk = configuredFlutter3448Sdk();
        Path projectRoot = Files.createDirectories(workspace.resolve("text_button_project"));
        Path dependencyRoot = Files.createDirectories(workspace.resolve("clipper_dependency"));
        Path dependencyLibrary = Files.createDirectories(dependencyRoot.resolve("lib"));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        writeFlutterPackageConfig(projectRoot, dependencyRoot, flutterSdk);
        Path importedFile = dependencyLibrary.resolve("button.dart");
        String importedSource = """
                import 'package:flutter/material.dart';
                void importedPress() {}
                void importedBool(bool value) {}
                const ButtonStyle importedStyle = ButtonStyle();
                final FocusNode importedFocus = FocusNode();
                final WidgetStatesController importedStates = WidgetStatesController();
                Widget importedLayer(BuildContext context, Set<WidgetState> states, Widget? child) => child ?? const SizedBox();
                """;
        Files.writeString(importedFile, importedSource, StandardCharsets.UTF_8);
        Path file = lib.resolve("main.dart");
        String disk = "void main() {}\n";
        Files.writeString(file, disk, StandardCharsets.UTF_8);
        Path pubspec = projectRoot.resolve("pubspec.yaml");
        String originalPubspec = Files.readString(pubspec, StandardCharsets.UTF_8);
        List<String> stderr = new ArrayList<>();
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(executable,
                line -> { synchronized (stderr) { stderr.add(line); } });
        String prelude = """
                import 'package:flutter/material.dart';
                import 'package:clipper_dependency/button.dart' as project_button;
                String unchangedCoreScope(String value, Object object) => value;
                void localPress() {}
                VoidCallback pressFactory() => localPress;
                void localBool(bool value) {}
                ValueChanged<bool> boolFactory() => localBool;
                const ButtonStyle localStyle = ButtonStyle();
                ButtonStyle styleFactory() => localStyle;
                final FocusNode localFocus = FocusNode();
                FocusNode focusFactory() => localFocus;
                final WidgetStatesController localStates = WidgetStatesController();
                WidgetStatesController statesFactory() => localStates;
                Widget localLayer(BuildContext context, Set<WidgetState> states, Widget? child) => child ?? const SizedBox();
                ButtonLayerBuilder layerFactory() => localLayer;
                dynamic dynamicPress = localPress;
                VoidCallback? nullablePress;
                void wrongPress(String value) {}
                dynamic dynamicBool = localBool;
                ValueChanged<bool>? nullableBool;
                void wrongBool(String value) {}
                dynamic dynamicStyle = localStyle;
                ButtonStyle? nullableStyle;
                dynamic dynamicFocus = localFocus;
                FocusNode? nullableFocus;
                dynamic dynamicStates = localStates;
                WidgetStatesController? nullableStates;
                dynamic dynamicLayer = localLayer;
                ButtonLayerBuilder? nullableLayer;
                Widget? nullableLayerResult(BuildContext context, Set<WidgetState> states, Widget? child) => child;
                Widget narrowLayer(BuildContext context, Set<WidgetState> states, Widget child) => child;
                const Animation<Color?> localAnimation = AlwaysStoppedAnimation<Color?>(null);
                const CustomClipper<RRect> localClipper = ButtonProofClipper();
                class ButtonProofClipper extends CustomClipper<RRect> {
                  const ButtonProofClipper();
                  @override
                  RRect getClip(Size size) => RRect.fromRectAndRadius(Offset.zero & size, Radius.zero);
                  @override
                  bool shouldReclip(covariant ButtonProofClipper oldClipper) => false;
                }
                void analyzerStaticTypeProofScope() {
                  // analyzer static-type proof insertion
                }
                """;
        List<String[]> references = List.of(
                new String[]{"localPress", "localPress", "onPressed", "VoidCallback"},
                new String[]{"pressFactory()", "pressFactory", "onPressed", "VoidCallback"},
                new String[]{"project_button.importedPress", "importedPress", "onPressed", "VoidCallback"},
                new String[]{"localBool", "localBool", "onHover", "ValueChanged<bool>"},
                new String[]{"boolFactory()", "boolFactory", "onHover", "ValueChanged<bool>"},
                new String[]{"project_button.importedBool", "importedBool", "onHover", "ValueChanged<bool>"},
                new String[]{"localStyle", "localStyle", "style", "ButtonStyle"},
                new String[]{"styleFactory()", "styleFactory", "style", "ButtonStyle"},
                new String[]{"project_button.importedStyle", "importedStyle", "style", "ButtonStyle"},
                new String[]{"localFocus", "localFocus", "focusNode", "FocusNode"},
                new String[]{"focusFactory()", "focusFactory", "focusNode", "FocusNode"},
                new String[]{"project_button.importedFocus", "importedFocus", "focusNode", "FocusNode"},
                new String[]{"localStates", "localStates", "statesController", "WidgetStatesController"},
                new String[]{"statesFactory()", "statesFactory", "statesController", "WidgetStatesController"},
                new String[]{"project_button.importedStates", "importedStates", "statesController", "WidgetStatesController"},
                new String[]{"localLayer", "localLayer", "backgroundBuilder", "ButtonLayerBuilder"},
                new String[]{"layerFactory()", "layerFactory", "backgroundBuilder", "ButtonLayerBuilder"},
                new String[]{"project_button.importedLayer", "importedLayer", "backgroundBuilder", "ButtonLayerBuilder"});
        StringBuilder valid = new StringBuilder(prelude);
        for (String variant : List.of("standard", "icon")) {
            String constructor = variant.equals("standard") ? "TextButton" : "TextButton.icon";
            String child = variant.equals("standard") ? "child: const Text('Label')" : "icon: const Icon(Icons.add), label: const Text('Label')";
            valid.append("Widget ").append(variant).append("Complete() => ").append(constructor)
                    .append("(onPressed: localPress, onLongPress: localPress, onHover: localBool, onFocusChange: localBool, ")
                    .append("style: localStyle, focusNode: localFocus, autofocus: true, clipBehavior: null, statesController: localStates, ")
                    .append(child).append(variant.equals("standard") ? ", isSemanticButton: null" : ", iconAlignment: IconAlignment.end")
                    .append(");\n");
            valid.append("Widget ").append(variant).append("Prototype() => ").append(constructor)
                    .append("(onPressed: () {}, ").append(child).append(");\n");
            valid.append("Widget ").append(variant).append("LongPressOnly() => ").append(constructor)
                    .append("(onPressed: null, onLongPress: localPress, ").append(child).append(");\n");
            for (String[] spec : references) {
                boolean layer = spec[2].equals("backgroundBuilder");
                valid.append("Widget ").append(variant).append(spec[1]).append("() => ").append(constructor).append('(')
                        .append(spec[2].equals("onPressed") ? "" : "onPressed: null, ")
                        .append(layer ? "style: ButtonStyle(backgroundBuilder: " : spec[2] + ": ")
                        .append(spec[0]).append(layer ? "), " : ", ").append(child).append(");\n");
            }
        }
        valid.append("""
                const ButtonStyle completeStyle = ButtonStyle(
                  textStyle: WidgetStatePropertyAll<TextStyle?>(TextStyle(fontSize: 17, fontWeight: FontWeight.w500)),
                  backgroundColor: WidgetStatePropertyAll<Color?>(Color(0xFF112233)),
                  foregroundColor: WidgetStatePropertyAll<Color?>(Color(0xFF445566)),
                  overlayColor: WidgetStatePropertyAll<Color?>(Color(0x55123456)),
                  shadowColor: WidgetStatePropertyAll<Color?>(Color(0xFF123456)),
                  surfaceTintColor: WidgetStatePropertyAll<Color?>(Color(0xFF234567)),
                  elevation: WidgetStatePropertyAll<double?>(2),
                  padding: WidgetStatePropertyAll<EdgeInsetsGeometry?>(EdgeInsetsDirectional.fromSTEB(10, 6, 14, 8)),
                  minimumSize: WidgetStatePropertyAll<Size?>(Size(20, 20)),
                  fixedSize: WidgetStatePropertyAll<Size?>(Size(90, 40)),
                  maximumSize: WidgetStatePropertyAll<Size?>(Size.infinite),
                  iconColor: WidgetStatePropertyAll<Color?>(Color(0xFF345678)),
                  iconSize: WidgetStatePropertyAll<double?>(21),
                  iconAlignment: IconAlignment.end,
                  side: WidgetStatePropertyAll<BorderSide?>(BorderSide(width: 2, color: Color(0xFF456789))),
                  shape: WidgetStatePropertyAll<OutlinedBorder?>(StadiumBorder()),
                  mouseCursor: WidgetStatePropertyAll<MouseCursor?>(SystemMouseCursors.click),
                  visualDensity: VisualDensity(horizontal: 1, vertical: -1),
                  tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                  animationDuration: Duration(milliseconds: 150),
                  enableFeedback: true, alignment: AlignmentDirectional(1, -0.5),
                  splashFactory: InkRipple.splashFactory,
                  backgroundBuilder: localLayer, foregroundBuilder: localLayer,
                );
                final ButtonStyle arbitraryStateStyle = ButtonStyle(
                  foregroundColor: WidgetStateProperty<Color?>.fromMap(<WidgetStatesConstraint, Color?>{
                    WidgetState.disabled: const Color(0xFF111111),
                    WidgetState.error & WidgetState.hovered: const Color(0xFF222222),
                    WidgetState.dragged | WidgetState.selected: const Color(0xFF333333),
                    WidgetState.scrolledUnder: const Color(0xFF444444),
                    WidgetState.pressed: const Color(0xFF555555),
                    WidgetState.focused: const Color(0xFF666666),
                    WidgetState.any: null,
                  }),
                  backgroundBuilder: localLayer, foregroundBuilder: localLayer,
                );
                Widget constStandard() => const TextButton(onPressed: localPress, style: completeStyle,
                  onLongPress: localPress, onHover: localBool, onFocusChange: localBool,
                  autofocus: false, clipBehavior: Clip.antiAlias, isSemanticButton: false, child: Text('Const'));
                Widget iconNull() => TextButton.icon(onPressed: null, icon: null, label: const Text('No icon'),
                  clipBehavior: null, style: completeStyle, iconAlignment: IconAlignment.start);
                Widget iconOmitted() => TextButton.icon(onPressed: null, label: const Text('Omitted icon'));
                Widget themed(BuildContext context) => TextButtonTheme(
                  data: const TextButtonThemeData(style: ButtonStyle(iconAlignment: IconAlignment.end)),
                  child: TextButton.icon(onPressed: localPress, icon: const Icon(Icons.add), label: const Text('Theme'),
                    style: arbitraryStateStyle));
                Widget mixedReferences() => TextButton(onPressed: localPress, style: localStyle,
                  child: ClipRRect(clipper: localClipper, child: CircularProgressIndicator(valueColor: localAnimation)));
                """);
        String candidate = valid.toString();
        ArrayList<DartSymbolProbe> probes = new ArrayList<>();
        List<String> sdkSymbols = List.of("TextButton", "ButtonStyle", "ButtonLayerBuilder", "VoidCallback", "ValueChanged",
                "FocusNode", "WidgetStatesController", "WidgetState", "WidgetStatesConstraint", "WidgetStateProperty",
                "WidgetStatePropertyAll", "Text", "Icon", "Icons", "IconAlignment", "Clip", "TextButtonTheme", "TextButtonThemeData",
                "MaterialTapTargetSize", "VisualDensity", "InkRipple", "AlignmentDirectional", "EdgeInsetsDirectional");
        for (String symbol : sdkSymbols) {
            var occurrence = Pattern.compile("\\b" + symbol + "\\b").matcher(candidate);
            assertTrue(occurrence.find(), symbol);
            probes.add(probe("text-button-" + symbol, occurrence.start(), symbol, "package:flutter/material.dart", flutterSdk));
        }
        probes.add(probe("text-button-icon-constructor", candidate.indexOf("TextButton.icon") + "TextButton.".length(),
                "icon", "package:flutter/material.dart", flutterSdk));
        for (String variant : List.of("standard", "icon")) {
            for (String[] spec : references) {
                int methodOffset = candidate.indexOf("Widget " + variant + spec[1] + "()");
                int offset = candidate.indexOf(spec[2] + ": " + spec[0], methodOffset) + spec[2].length() + 2;
                boolean imported = spec[0].startsWith("project_button.");
                probes.add(typedProbe("text-button-" + variant + '-' + spec[1],
                        offset + (imported ? "project_button.".length() : 0), spec[1],
                        imported ? "package:clipper_dependency/button.dart" : "project:current",
                        imported ? dependencyLibrary : lib, offset, spec[0].length(), candidate, spec[3], "package:flutter/material.dart"));
            }
        }
        for (String[] spec : List.of(new String[]{"localAnimation", "Animation<Color?>"},
                new String[]{"localClipper", "CustomClipper<RRect>"})) {
            int offset = candidate.indexOf(spec[0], candidate.indexOf("Widget mixedReferences()"));
            probes.add(0, typedProbe("text-button-mixed-" + spec[0], offset, spec[0], "project:current", lib,
                    offset, spec[0].length(), candidate, spec[1], "package:flutter/material.dart"));
        }
        DartCandidateAnalysisResult passed = await(analyzer.analyze(request(projectRoot, file, candidate, 2900, probes)));
        assertEquals(DartCandidateAnalysisStatus.PASSED, passed.status(), () -> passed + " stderr=" + stderr);
        assertEquals(sdkSymbols.size() + 39, passed.symbolEvidence().size());
        assertTrue(passed.symbolEvidence().stream().allMatch(DartSymbolEvidence::accepted));
        assertEquals(38, passed.symbolEvidence().stream().filter(e -> e.staticTypeEvidence().isPresent()).count());
        long version = 2901;
        for (String[] spec : List.of(
                new String[]{"dynamicPress", "onPressed", "VoidCallback"},
                new String[]{"nullablePress", "onPressed", "VoidCallback"},
                new String[]{"wrongPress", "onPressed", "VoidCallback"},
                new String[]{"dynamicBool", "onHover", "ValueChanged<bool>"},
                new String[]{"nullableBool", "onHover", "ValueChanged<bool>"},
                new String[]{"wrongBool", "onHover", "ValueChanged<bool>"},
                new String[]{"dynamicStyle", "style", "ButtonStyle"},
                new String[]{"nullableStyle", "style", "ButtonStyle"},
                new String[]{"dynamicFocus", "focusNode", "FocusNode"},
                new String[]{"nullableFocus", "focusNode", "FocusNode"},
                new String[]{"dynamicStates", "statesController", "WidgetStatesController"},
                new String[]{"nullableStates", "statesController", "WidgetStatesController"},
                new String[]{"dynamicLayer", "backgroundBuilder", "ButtonLayerBuilder"},
                new String[]{"nullableLayer", "backgroundBuilder", "ButtonLayerBuilder"},
                new String[]{"nullableLayerResult", "backgroundBuilder", "ButtonLayerBuilder"},
                new String[]{"narrowLayer", "backgroundBuilder", "ButtonLayerBuilder"})) {
            boolean layer = spec[1].equals("backgroundBuilder");
            String invalid = "// ignore_for_file: argument_type_not_assignable, invalid_assignment\n" + prelude
                    + "Widget invalid() => TextButton(" + (spec[1].equals("onPressed") ? "" : "onPressed: null, ")
                    + (layer ? "style: ButtonStyle(backgroundBuilder: " : spec[1] + ": ") + spec[0]
                    + (layer ? "), " : ", ") + "child: const Text('Label'));\n";
            int offset = invalid.indexOf(spec[0], invalid.indexOf("Widget invalid()"));
            DartSymbolProbe typeProbe = typedProbe("text-button-invalid-" + spec[0], offset, spec[0],
                    "project:current", lib, offset, spec[0].length(), invalid, spec[2], "package:flutter/material.dart");
            DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(projectRoot, file, invalid, version++, List.of(typeProbe))));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> spec[0] + ": " + rejected);
            assertFalse(rejected.symbolEvidence().getFirst().staticTypeEvidence().orElseThrow().accepted());
        }
        for (String expression : List.of(
                "TextButton(child: Text('Missing press'))",
                "TextButton(onPressed: null)", "TextButton(onPressed: null, child: null)",
                "TextButton(onPressed: null, child: Text('x'), icon: Icon(Icons.add))",
                "TextButton(onPressed: null, child: Text('x'), iconAlignment: IconAlignment.start)",
                "TextButton(onPressed: null, child: Text('x'), isSemanticButton: 1)",
                "TextButton(onPressed: null, child: Text('x'), autofocus: null)",
                "TextButton(onPressed: null, child: Text('x'), clipBehavior: Axis.horizontal)",
                "TextButton(onPressed: null, child: Text('x'), style: true)",
                "TextButton(onPressed: null, child: Text('x'), focusNode: true)",
                "TextButton(onPressed: null, child: Text('x'), statesController: true)",
                "TextButton(onPressed: null, child: Text('x'), enabled: true)",
                "TextButton(onPressed: null, child: Text('x'), variant: 'icon')",
                "TextButton.icon(label: Text('Missing press'))",
                "TextButton.icon(onPressed: null)", "TextButton.icon(onPressed: null, label: null)",
                "TextButton.icon(onPressed: null, label: Text('x'), child: Text('y'))",
                "TextButton.icon(onPressed: null, label: Text('x'), isSemanticButton: true)",
                "TextButton.icon(onPressed: null, label: Text('x'), icon: true)",
                "TextButton.icon(onPressed: null, label: Text('x'), iconAlignment: Axis.vertical)",
                "const TextButton.icon(onPressed: null, label: Text('Not const'))",
                "TextButton(onPressed: null, child: Text('x'), style: ButtonStyle(iconAlignment: Axis.horizontal))",
                "TextButton(onPressed: null, child: Text('x'), style: ButtonStyle(backgroundBuilder: localBool))",
                "TextButton(onPressed: null, child: Text('x'), style: ButtonStyle(foregroundBuilder: localPress))")) {
            String invalid = prelude + "Widget invalid() => " + expression + ";\n";
            DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(projectRoot, file, invalid, version++, List.of())));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> expression + ": " + rejected);
        }
        assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        assertEquals(originalPubspec, Files.readString(pubspec, StandardCharsets.UTF_8));
        assertEquals(importedSource, Files.readString(importedFile, StandardCharsets.UTF_8));
        assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
    }

    @Test
    void validatesBooleanCallbackProofWithoutChangingExplicitOrImplicitCoreScope() throws Exception {
        Path executable = configuredDartExecutable();
        Path flutterSdk = configuredFlutter3448Sdk();
        Path projectRoot = Files.createDirectories(workspace.resolve("bool_callback_scope_project"));
        Path dependencyRoot = Files.createDirectories(workspace.resolve("clipper_dependency"));
        Files.createDirectories(dependencyRoot.resolve("lib"));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        writeFlutterPackageConfig(projectRoot, dependencyRoot, flutterSdk);
        Path file = lib.resolve("main.dart");
        Files.writeString(file, "void main() {}\n", StandardCharsets.UTF_8);
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(executable, ignored -> { });
        long version = 3100;
        for (var scope : List.of(
                Map.entry("", ""),
                Map.entry("import 'dart:core' as core;\n", "core."),
                Map.entry("import r'dart:core' as core;\n", "core."),
                Map.entry("import '''\ndart:core''' as core;\n", "core."),
                Map.entry("import r''' \t\r\ndart:core''' as core;\n", "core."),
                Map.entry("import '''\\\ndart:core''' as core;\n", "core."),
                Map.entry("import 'dart:' /* split */ r'core' as core;\n", "core."),
                Map.entry("import 'dart:' '''\ncore''' as core;\n", "core."),
                Map.entry("import 'dart:\\u0063ore' as core;\n", "core."),
                Map.entry("import 'dart:core' hide bool;\nimport 'dart:core' as core;\n", "core."),
                Map.entry("import 'dart:core' if (dart.library.io) 'dart:core' as core;\n", "core."),
                Map.entry("import 'dart:math' if (dart.library.io) 'dart:core' as conditional;\n", ""))) {
            String header = scope.getKey();
            String prefix = scope.getValue();
            String candidate = header + "import 'package:flutter/material.dart';\n"
                    + "const marker = \"import 'dart:core';\";\n"
                    + prefix + "String unchangedScope(" + prefix + "String value, " + prefix + "Object object) => value;\n"
                    + "void callback(" + prefix + "bool value) {}\n"
                    + "void analyzerStaticTypeProofScope() {\n  // analyzer static-type proof insertion\n}\n"
                    + "Widget sample() => TextButton(onPressed: null, onHover: callback, child: const Text('Label'));\n";
            int offset = candidate.indexOf("callback", candidate.indexOf("Widget sample()"));
            var probe = typedProbe("scoped-bool", offset, "callback", "project:current", lib,
                    offset, "callback".length(), candidate, "ValueChanged<bool>", "package:flutter/widgets.dart");
            var result = await(analyzer.analyze(request(projectRoot, file, candidate, version++, List.of(probe))));
            assertEquals(DartCandidateAnalysisStatus.PASSED, result.status(), () -> header + ": " + result);
            assertTrue(result.symbolEvidence().getFirst().staticTypeEvidence().orElseThrow().accepted());
        }
        String shadowed = """
                // ignore_for_file: argument_type_not_assignable
                import 'dart:core' as core;
                import 'package:flutter/material.dart';
                class bool {}
                void callback(bool value) {}
                void analyzerStaticTypeProofScope() {
                  // analyzer static-type proof insertion
                }
                Widget sample() => TextButton(onPressed: null, onHover: callback, child: const Text('Label'));
                """;
        int offset = shadowed.indexOf("callback", shadowed.indexOf("Widget sample()"));
        var probe = typedProbe("shadowed-bool", offset, "callback", "project:current", lib,
                offset, "callback".length(), shadowed, "ValueChanged<bool>", "package:flutter/widgets.dart");
        var rejected = await(analyzer.analyze(request(projectRoot, file, shadowed, version, List.of(probe))));
        assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> rejected.toString());
        assertFalse(rejected.symbolEvidence().getFirst().staticTypeEvidence().orElseThrow().accepted());
        assertEquals("void main() {}\n", Files.readString(file, StandardCharsets.UTF_8));
        assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
    }

    @Test
    void validatesRadioNullableTypeIdentityWithoutDynamicOrCovariantRegistryEscapes() throws Exception {
        Path executable = configuredDartExecutable();
        Path flutterSdk = configuredFlutter3448Sdk();
        Path projectRoot = Files.createDirectories(workspace.resolve("radio_type_proof_project"));
        Path dependencyRoot = Files.createDirectories(workspace.resolve("clipper_dependency"));
        Files.createDirectories(dependencyRoot.resolve("lib"));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        writeFlutterPackageConfig(projectRoot, dependencyRoot, flutterSdk);
        Path file = lib.resolve("main.dart");
        String disk = "void main() {}\n";
        Files.writeString(file, disk, StandardCharsets.UTF_8);
        String prelude = """
                // ignore_for_file: invalid_assignment, argument_type_not_assignable
                import 'dart:core' as core;
                import 'package:flutter/material.dart';
                enum Choice { first }
                typedef Alias = Choice;
                typedef NullableAlias = Choice?;
                typedef DynamicAlias = core.dynamic;
                Choice? nullableChoice;
                Choice? nullableChoiceFactory() => null;
                core.Object? nullableObject;
                core.dynamic dynamicValue;
                core.dynamic dynamicFactory() => null;
                T genericFactory<T>() => throw 0;
                class Spoof {
                  core.int get _nbfdStaticTypeProof0NonDynamic => 1;
                }
                core.dynamic spoofedDynamic = Spoof();
                class Registry<T> implements RadioGroupRegistry<T> {
                  T? get groupValue => null;
                  ValueChanged<T?> get onChanged => (_) {};
                  void registerClient(RadioClient<T> value) {}
                  void unregisterClient(RadioClient<T> value) {}
                }
                final choiceRegistry = Registry<Choice>();
                final stringRegistry = Registry<core.String>();
                void analyzerStaticTypeProofScope() {
                  // analyzer static-type proof insertion
                }
                """;
        record Case(String expression, String expectedType, String selectedType, boolean accepted) {}
        List<Case> cases = List.of(
                new Case("nullableChoice", "Object?", "Choice", true),
                new Case("nullableChoiceFactory()", "Object?", "Choice", true),
                new Case("nullableObject", "Object?", "core.Object?", true),
                new Case("dynamicValue", "Object?", "core.Object?", false),
                new Case("dynamicFactory()", "Object?", "core.Object?", false),
                new Case("spoofedDynamic", "Object?", "core.Object?", false),
                new Case("genericFactory()", "Object?", "core.Object?", false),
                new Case("genericFactory()", "Object", "Choice", true),
                new Case("choiceRegistry", "RadioGroupRegistry<Object>", "Choice", true),
                new Case("stringRegistry", "RadioGroupRegistry<Object>", "core.Object?", false),
                new Case("Alias", "Type", "Alias", true),
                new Case("NullableAlias", "Type", "NullableAlias?", true),
                new Case("NullableAlias", "Type", "NullableAlias", false),
                new Case("DynamicAlias", "Type", "DynamicAlias", false));
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(executable, ignored -> {});
        long version = 3400;
        for (Case sample : cases) {
            String candidate = prelude + "core.Object? sample() => " + sample.expression() + ";\n";
            int offset = candidate.indexOf(sample.expression(), candidate.indexOf("core.Object? sample()"));
            String symbol = sample.expression().replace("()", "");
            var probe = new DartSymbolProbe("radio-proof", offset, symbol.length(), symbol, "project:current", lib,
                    Optional.empty(), Optional.of(new DartStaticTypeProbe(offset, sample.expression().length(), 0,
                            candidate.indexOf("  // analyzer static-type proof insertion"), sample.expectedType(),
                            "package:flutter/material.dart", Optional.of(sample.selectedType()))));
            var result = await(analyzer.analyze(request(projectRoot, file, candidate, version++, List.of(probe))));
            assertEquals(sample.accepted() ? DartCandidateAnalysisStatus.PASSED : DartCandidateAnalysisStatus.REJECTED,
                    result.status(), () -> sample + ": " + result);
            assertEquals(sample.accepted(), result.symbolEvidence().getFirst().staticTypeEvidence().orElseThrow().accepted(), sample.toString());
            assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        }
        assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
    }

    @Test
    void validatesNumericStatePropertyProofWithRestrictedCoreAndRejectsShadowedTypes() throws Exception {
        Path executable = configuredDartExecutable();
        Path flutterSdk = configuredFlutter3448Sdk();
        Path projectRoot = Files.createDirectories(workspace.resolve("numeric_state_core_scope_project"));
        Path dependencyRoot = Files.createDirectories(workspace.resolve("clipper_dependency"));
        Files.createDirectories(dependencyRoot.resolve("lib"));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        writeFlutterPackageConfig(projectRoot, dependencyRoot, flutterSdk);
        Path file = lib.resolve("main.dart");
        String disk = "void main() {}\n";
        Files.writeString(file, disk, StandardCharsets.UTF_8);
        Path pubspec = projectRoot.resolve("pubspec.yaml");
        Path packageConfig = projectRoot.resolve(".dart_tool/package_config.json");
        String originalPubspec = Files.readString(pubspec, StandardCharsets.UTF_8);
        String originalPackageConfig = Files.readString(packageConfig, StandardCharsets.UTF_8);
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(executable, ignored -> { });
        long version = 3300;
        for (var scope : List.of(
                Map.entry("", ""),
                Map.entry("import 'dart:core' as core;\n", "core."),
                Map.entry("import 'dart:core' show String;\nimport 'dart:core' as core;\n", "core."),
                Map.entry("import 'dart:core' hide double;\nimport 'dart:core' as core;\n", "core."),
                Map.entry("import 'dart:\\u0063ore' as core;\n", "core."))) {
            String prefix = scope.getValue();
            String candidate = scope.getKey() + "import 'package:flutter/material.dart';\n"
                    + "const marker = \"import 'dart:core';\";\n"
                    + prefix + "String unchangedScope(" + prefix + "String value, " + prefix + "Object object) => value;\n"
                    + "const plainWidth = WidgetStatePropertyAll<" + prefix + "double>(2);\n"
                    + "const nullableWidth = WidgetStatePropertyAll<" + prefix + "double?>(null);\n"
                    + "void analyzerStaticTypeProofScope() {\n  // analyzer static-type proof insertion\n}\n"
                    + "Widget sample() => Column(children: [\n"
                    + "  Switch(value: false, onChanged: null, trackOutlineWidth: plainWidth),\n"
                    + "  Switch.adaptive(value: false, onChanged: null, trackOutlineWidth: nullableWidth),\n]);\n";
            ArrayList<DartSymbolProbe> probes = new ArrayList<>();
            for (var reference : List.of(Map.entry("plainWidth", "WidgetStateProperty<double>"),
                    Map.entry("nullableWidth", "WidgetStateProperty<double?>"))) {
                int offset = candidate.indexOf(reference.getKey(), candidate.indexOf("Widget sample()"));
                probes.add(typedProbe("scoped-" + reference.getKey(), offset, reference.getKey(), "project:current", lib,
                        offset, reference.getKey().length(), candidate, reference.getValue(), "package:flutter/widgets.dart"));
            }
            var result = await(analyzer.analyze(request(projectRoot, file, candidate, version++, probes)));
            assertEquals(DartCandidateAnalysisStatus.PASSED, result.status(), () -> scope.getKey() + ": " + result);
            assertEquals(2, result.symbolEvidence().size());
            assertTrue(result.symbolEvidence().stream().allMatch(evidence ->
                    evidence.staticTypeEvidence().orElseThrow().accepted()));
        }
        for (String declaration : List.of(
                "core.dynamic get reference => const WidgetStatePropertyAll<core.double?>(2);",
                "WidgetStateProperty<core.double?>? get reference => const WidgetStatePropertyAll<core.double?>(2);",
                "WidgetStateProperty<core.String> get reference => const WidgetStatePropertyAll<core.String>('wrong');",
                "class double { const double(); }\n"
                        + "WidgetStateProperty<double> get reference => const WidgetStatePropertyAll<double>(double());")) {
            String candidate = "// ignore_for_file: argument_type_not_assignable, invalid_assignment\n"
                    + "import 'dart:core' show String;\nimport 'dart:core' as core;\n"
                    + "import 'package:flutter/material.dart';\n" + declaration + "\n"
                    + "void analyzerStaticTypeProofScope() {\n  // analyzer static-type proof insertion\n}\n"
                    + "Widget sample() => Switch(value: false, onChanged: null, trackOutlineWidth: reference);\n";
            int offset = candidate.indexOf("reference", candidate.indexOf("Widget sample()"));
            var probe = typedProbe("invalid-numeric-state", offset, "reference", "project:current", lib,
                    offset, "reference".length(), candidate, "WidgetStateProperty<double?>", "package:flutter/widgets.dart");
            var rejected = await(analyzer.analyze(request(projectRoot, file, candidate, version++, List.of(probe))));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> declaration + ": " + rejected);
            assertFalse(rejected.symbolEvidence().getFirst().staticTypeEvidence().orElseThrow().accepted());
        }
        assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        assertEquals(originalPubspec, Files.readString(pubspec, StandardCharsets.UTF_8));
        assertEquals(originalPackageConfig, Files.readString(packageConfig, StandardCharsets.UTF_8));
        assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
    }

    @Test
    void validatesNonBooleanTypedBatchesWithRestrictedCoreAndRejectsDynamicReferences() throws Exception {
        Path executable = configuredDartExecutable();
        Path flutterSdk = configuredFlutter3448Sdk();
        Path projectRoot = Files.createDirectories(workspace.resolve("non_bool_core_scope_project"));
        Path dependencyRoot = Files.createDirectories(workspace.resolve("clipper_dependency"));
        Files.createDirectories(dependencyRoot.resolve("lib"));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        writeFlutterPackageConfig(projectRoot, dependencyRoot, flutterSdk);
        Path file = lib.resolve("main.dart");
        String disk = "void main() {}\n";
        Files.writeString(file, disk, StandardCharsets.UTF_8);
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(executable, ignored -> { });
        List<String[]> references = List.of(
                new String[]{"localStyle", "style", "ButtonStyle"},
                new String[]{"localPress", "onPressed", "VoidCallback"},
                new String[]{"localFocus", "focusNode", "FocusNode"});
        long version = 3200;
        for (var scope : List.of(
                Map.entry("", ""),
                Map.entry("import 'dart:core' as core;\n", "core."),
                Map.entry("import 'dart:core' show String;\nimport 'dart:core' as core;\n", "core."))) {
            String candidate = scope.getKey() + "import 'package:flutter/material.dart';\n"
                    + scope.getValue() + "String unchangedScope(" + scope.getValue() + "String value, "
                    + scope.getValue() + "Object object) => value;\n"
                    + "const localStyle = ButtonStyle();\n"
                    + "void localPress() {}\n"
                    + "final localFocus = FocusNode();\n"
                    + "void analyzerStaticTypeProofScope() {\n  // analyzer static-type proof insertion\n}\n"
                    + "Widget sample() => TextButton(style: localStyle, onPressed: localPress, focusNode: localFocus, child: const Text('Label'));\n";
            ArrayList<DartSymbolProbe> probes = new ArrayList<>();
            for (String[] reference : references) {
                int offset = candidate.indexOf(reference[0], candidate.indexOf("Widget sample()"));
                probes.add(typedProbe("scoped-" + reference[0], offset, reference[0], "project:current", lib,
                        offset, reference[0].length(), candidate, reference[2], "package:flutter/material.dart"));
            }
            var result = await(analyzer.analyze(request(projectRoot, file, candidate, version++, probes)));
            assertEquals(DartCandidateAnalysisStatus.PASSED, result.status(), () -> scope.getKey() + ": " + result);
            assertEquals(3, result.symbolEvidence().size());
            assertTrue(result.symbolEvidence().stream().allMatch(evidence ->
                    evidence.staticTypeEvidence().orElseThrow().accepted()));
        }
        for (String[] reference : references) {
            String candidate = """
                    // ignore_for_file: argument_type_not_assignable, invalid_assignment
                    import 'dart:core' show String;
                    import 'dart:core' as core;
                    import 'package:flutter/material.dart';
                    core.dynamic dynamicReference;
                    void analyzerStaticTypeProofScope() {
                      // analyzer static-type proof insertion
                    }
                    """ + "Widget sample() => TextButton("
                    + (reference[1].equals("onPressed") ? "" : "onPressed: null, ")
                    + reference[1] + ": dynamicReference, child: const Text('Label'));\n";
            int offset = candidate.indexOf("dynamicReference", candidate.indexOf("Widget sample()"));
            var probe = typedProbe("dynamic-" + reference[2], offset, "dynamicReference", "project:current", lib,
                    offset, "dynamicReference".length(), candidate, reference[2], "package:flutter/material.dart");
            var rejected = await(analyzer.analyze(request(projectRoot, file, candidate, version++, List.of(probe))));
            assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(), () -> reference[2] + ": " + rejected);
            assertFalse(rejected.symbolEvidence().getFirst().staticTypeEvidence().orElseThrow().accepted());
        }
        assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
        assertFalse(Files.exists(lib.resolve("analysis_options.yaml")));
    }

    private static String superellipseCandidate(String methods) {
        return clipperCandidate("""
                class CurrentSuperellipseClipper extends CustomClipper<RSuperellipse> {
                  const CurrentSuperellipseClipper();
                  @override
                  RSuperellipse getClip(Size size) => throw UnimplementedError();
                  @override
                  bool shouldReclip(covariant CurrentSuperellipseClipper oldClipper) => false;
                }
                const currentSuperellipseClipper = CurrentSuperellipseClipper();
                dynamic dynamicSuperellipseClipper = currentSuperellipseClipper;
                CustomClipper<RSuperellipse>? nullableSuperellipseClipper = currentSuperellipseClipper;
                """ + methods);
    }

    private static String clipPathCandidate(String buildMethods) {
        return clipperCandidate("""
                class CurrentPathClipper extends CustomClipper<Path> {
                  const CurrentPathClipper();
                  @override
                  Path getClip(Size size) => Path()..addRect(Offset.zero & size);
                  @override
                  bool shouldReclip(covariant CurrentPathClipper oldClipper) => false;
                }
                const currentPathClipper = CurrentPathClipper();
                dynamic dynamicPathClipper = currentPathClipper;
                CustomClipper<Path>? nullablePathClipper = currentPathClipper;
                ShapeBorder get currentShape => const CircleBorder();
                dynamic dynamicShape = currentShape;
                ShapeBorder? nullableShape = currentShape;
                """ + buildMethods);
    }

    private static void assertSuppressedStaticProofIsRejected(
            DartCandidateAnalyzer analyzer,
            Path projectRoot,
            Path file,
            Path projectLibrary,
            String candidate,
            String expression,
            long version,
            List<String> stderr) throws Exception {
        int expressionOffset = candidate.indexOf(
                expression, candidate.indexOf("clipper:"));
        String symbol = rootSymbol(expression);
        int symbolOffset = candidate.indexOf(symbol, expressionOffset);
        DartSymbolProbe probe = typedProbe(
                "suppressed-invalid",
                symbolOffset,
                symbol,
                "project:current",
                projectLibrary,
                expressionOffset,
                expression.length(),
                candidate,
                "CustomClipper<RRect>");

        DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(
                projectRoot, file, candidate, version, List.of(probe))));

        assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(),
                () -> rejected + " stderr=" + stderr);
        DartStaticTypeEvidence staticEvidence = rejected.symbolEvidence()
                .getFirst().staticTypeEvidence().orElseThrow();
        assertFalse(staticEvidence.accepted());
        assertFalse(staticEvidence.rejectionReason().orElseThrow().isBlank(),
                () -> staticEvidence.toString());
    }

    private static String rootSymbol(String expression) {
        String value = expression.startsWith("const ")
                ? expression.substring("const ".length()) : expression;
        int end = value.indexOf('(');
        return end < 0 ? value : value.substring(0, end);
    }

    private static String clipperCandidate(String buildMethods) {
        return """
                import 'package:flutter/widgets.dart';
                import 'package:clipper_dependency/clippers.dart' as project_clippers;

                int Object() => 0;

                class CurrentRRectClipper extends CustomClipper<RRect> {
                  const CurrentRRectClipper();

                  @override
                  RRect getClip(Size size) => RRect.zero;

                  @override
                  bool shouldReclip(covariant CurrentRRectClipper oldClipper) => false;
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

                class CurrentRectClipper extends CustomClipper<Rect> {
                  const CurrentRectClipper();

                  @override
                  Rect getClip(Size size) => Offset.zero & size;

                  @override
                  bool shouldReclip(covariant CurrentRectClipper oldClipper) => false;
                }

                class GenericCtorClipper<T> extends CustomClipper<T> {
                  const GenericCtorClipper();

                  @override
                  T getClip(Size size) => throw UnimplementedError();

                  @override
                  bool shouldReclip(covariant GenericCtorClipper<T> oldClipper) => false;
                }

                abstract class GenericFactoryClipper<T> extends CustomClipper<T> {
                  const GenericFactoryClipper._();
                  const factory GenericFactoryClipper() =
                      GenericFactoryClipperImpl<T>;
                }

                class GenericFactoryClipperImpl<T>
                    extends GenericFactoryClipper<T> {
                  const GenericFactoryClipperImpl() : super._();

                  @override
                  T getClip(Size size) => throw UnimplementedError();

                  @override
                  bool shouldReclip(
                    covariant GenericFactoryClipperImpl<T> oldClipper,
                  ) => false;
                }

                sealed class ClosedRRectFamily {
                  const ClosedRRectFamily();
                }

                final class ClosedRRectMember extends CurrentRRectClipper
                    implements ClosedRRectFamily {
                  const ClosedRRectMember();
                }

                class NotAClipper {
                  const NotAClipper();
                }

                const currentClipper = CurrentRRectClipper();
                const currentRectClipper = CurrentRectClipper();
                dynamic dynamicClipper = currentClipper;
                CustomClipper<RRect>? nullableClipper = currentClipper;
                dynamic dynamicRectClipper = currentRectClipper;
                ClosedRRectFamily sealedFamilyClipper = const ClosedRRectMember();

                void analyzerStaticTypeProofScope() {
                  // analyzer static-type proof insertion
                }

                """ + buildMethods;
    }

    private static DartSymbolProbe probe(
            String id,
            int offset,
            String symbol,
            String libraryUri,
            Path expectedRoot) {
        return new DartSymbolProbe(
                id,
                offset,
                symbol.length(),
                symbol,
                libraryUri,
                expectedRoot.toAbsolutePath().normalize(),
                Optional.empty());
    }

    private static DartSymbolProbe typedProbe(
            String id,
            int offset,
            String symbol,
            String libraryUri,
            Path expectedRoot,
            int expressionOffset,
            int expressionLength,
            String candidate,
            String expectedDartType) {
        return typedProbe(id, offset, symbol, libraryUri, expectedRoot, expressionOffset,
                expressionLength, candidate, expectedDartType, "package:flutter/widgets.dart");
    }

    private static DartSymbolProbe typedProbe(
            String id, int offset, String symbol, String libraryUri, Path expectedRoot,
            int expressionOffset, int expressionLength, String candidate,
            String expectedDartType, String expectedTypeLibraryUri) {
        int statementInsertion = candidate.indexOf(
                "  // analyzer static-type proof insertion");
        if (statementInsertion < 0) {
            throw new IllegalArgumentException("candidate has no proof scope");
        }
        return new DartSymbolProbe(
                id,
                offset,
                symbol.length(),
                symbol,
                libraryUri,
                expectedRoot.toAbsolutePath().normalize(),
                Optional.empty(),
                Optional.of(new DartStaticTypeProbe(
                        expressionOffset,
                        expressionLength,
                        0,
                        statementInsertion,
                        expectedDartType,
                        expectedTypeLibraryUri)));
    }

    private void writeFlutterPackageConfig(
            Path projectRoot,
            Path dependencyRoot,
            Path flutterSdk) throws Exception {
        Path pubCache = configuredPubCache();
        ObjectNode config = JSON.createObjectNode();
        config.put("configVersion", 2);
        ArrayNode packages = config.putArray("packages");
        addPackage(packages, "clipper_probe", projectRoot, "3.10");
        addPackage(packages, "clipper_dependency", dependencyRoot, "3.10");
        addPackage(packages, "flutter", flutterSdk.resolve("packages/flutter"),
                "3.10");
        addPackage(packages, "sky_engine",
                flutterSdk.resolve("bin/cache/pkg/sky_engine"), "3.10");
        for (Map.Entry<String, String> entry
                : FLUTTER_3448_HOSTED_PACKAGES.entrySet()) {
            Path root = pubCache.resolve("hosted/pub.dev")
                    .resolve(entry.getKey() + '-' + entry.getValue());
            assumeTrue(Files.isDirectory(root),
                    "Flutter 3.44.8 dependency is absent from the configured pub cache: "
                    + root);
            addPackage(packages, entry.getKey(), root, "3.4");
        }
        Path dartTool = Files.createDirectories(
                projectRoot.resolve(".dart_tool"));
        Files.writeString(
                dartTool.resolve("package_config.json"),
                JSON.writerWithDefaultPrettyPrinter().writeValueAsString(config),
                StandardCharsets.UTF_8);
        Files.writeString(projectRoot.resolve("pubspec.yaml"), """
                name: clipper_probe
                environment:
                  sdk: ^3.10.0
                dependencies:
                  flutter:
                    sdk: flutter
                  clipper_dependency:
                    path: ../clipper_dependency
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

    private static Path configuredFlutter3448Sdk() throws Exception {
        String configured = System.getProperty("flutter.sdk", "").trim();
        assumeTrue(!configured.isEmpty(), "set -Dflutter.sdk=<path-to-flutter-3.44.8>");
        Path sdk = Path.of(configured).toAbsolutePath().normalize();
        assumeTrue(Files.isDirectory(sdk.resolve("packages/flutter/lib")),
                "Flutter framework library does not exist: " + sdk);
        Path version = sdk.resolve("bin/cache/flutter.version.json");
        assumeTrue(Files.isRegularFile(version),
                "Flutter version metadata does not exist: " + version);
        assertEquals("3.44.8",
                JSON.readTree(Files.readAllBytes(version))
                        .path("flutterVersion").asText(),
                "This contract proof must run against pinned Flutter 3.44.8");
        return sdk;
    }

    private static Path configuredPubCache() {
        String explicit = System.getProperty("pub.cache", "").trim();
        if (!explicit.isEmpty()) {
            Path value = Path.of(explicit).toAbsolutePath().normalize();
            assumeTrue(Files.isDirectory(value), "Configured pub cache is absent: " + value);
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

    private DartCandidateAnalysisRequest request(
            Path file,
            String content,
            long version,
            List<DartSymbolProbe> probes) {
        return request(workspace, file, content, version, probes);
    }

    private static DartCandidateAnalysisRequest request(
            Path projectRoot,
            Path file,
            String content,
            long version,
            List<DartSymbolProbe> probes) {
        return new DartCandidateAnalysisRequest(
                projectRoot.toAbsolutePath().normalize(),
                file.toAbsolutePath().normalize(),
                content,
                version,
                DartCandidateHashes.sha256(DartCandidateHashes.strictUtf8(content)),
                DartCandidateWarningPolicy.ALLOW,
                probes);
    }

    private static DartCandidateAnalysisResult await(
            DartCandidateAnalysisOperation operation) throws Exception {
        return operation.result().toCompletableFuture().get(60, TimeUnit.SECONDS);
    }

    private static Path configuredDartExecutable() {
        String configured = System.getProperty("dart.executable", "").trim();
        assumeTrue(!configured.isEmpty(), "set -Ddart.executable=<path-to-dart>");
        Path executable = Path.of(configured).toAbsolutePath().normalize();
        assumeTrue(Files.isRegularFile(executable),
                "Dart executable does not exist: " + executable);
        return executable;
    }
}
