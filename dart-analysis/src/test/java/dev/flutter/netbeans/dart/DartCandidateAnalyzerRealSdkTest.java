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
                        "package:flutter/widgets.dart")));
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
