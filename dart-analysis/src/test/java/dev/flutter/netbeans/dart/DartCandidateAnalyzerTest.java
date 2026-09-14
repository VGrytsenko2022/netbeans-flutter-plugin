package dev.flutter.netbeans.dart;

import dev.flutter.netbeans.api.DartCandidateCapacityBudget;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DartCandidateAnalyzerTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir
    Path temporaryDirectory;

    @Test
    void validatesOverlayDiagnosticsAndExactNavigationWithoutWritingDisk() throws Exception {
        Fixture fixture = fixture(Mode.PASS, limits(Duration.ofSeconds(3), 1024 * 1024));
        String disk = "void main() {}\n";
        String candidate = "void main() { print('candidate'); }\n";
        Files.writeString(fixture.dartFile, disk, StandardCharsets.UTF_8);
        Path sdkRoot = Files.createDirectories(temporaryDirectory.resolve("sdk/lib"));
        int offset = candidate.indexOf("print");
        DartSymbolProbe probe = new DartSymbolProbe(
                "dart.print",
                offset,
                "print".length(),
                "print",
                "dart:core",
                sdkRoot,
                Optional.of("FUNCTION"));

        DartCandidateAnalysisResult result = await(fixture.analyzer.analyze(request(
                fixture,
                candidate,
                99173,
                DartCandidateWarningPolicy.ALLOW,
                List.of(probe))));

        assertEquals(DartCandidateAnalysisStatus.PASSED, result.status());
        assertEquals(Optional.of("1.40.1"), result.analyzerProtocolVersion());
        assertEquals(1, result.requestedSymbolProbes());
        assertTrue(result.symbolEvidence().getFirst().accepted());
        assertEquals("FUNCTION",
                result.symbolEvidence().getFirst().targets().getFirst().kind());
        assertEquals(disk, Files.readString(fixture.dartFile, StandardCharsets.UTF_8));
        assertEquals(List.of(
                "analysis.setAnalysisRoots",
                "analysis.setPriorityFiles",
                "analysis.updateContent",
                "analysis.getErrors",
                "analysis.getNavigation",
                "analysis.updateContent",
                "analysis.setPriorityFiles",
                "server.shutdown"), fixture.process.methods());
        JsonNode overlay = fixture.process.requests().stream()
                .filter(value -> "analysis.updateContent".equals(value.path("method").asText()))
                .findFirst().orElseThrow()
                .path("params").path("files").path(fixture.dartFile.toString());
        assertEquals("add", overlay.path("type").asText());
        assertEquals(candidate, overlay.path("content").asText());
        assertEquals(99173, overlay.path("version").asLong());
        assertExpectedCommand(fixture);
    }

    @Test
    void booleanCallbackProofQualifiesCoreWithoutChangingOriginalImportScope() throws Exception {
        for (String header : List.of("", "const marker = \"import 'dart:core';\";\n",
                "import 'dart:core' as sourceCore show bool;\n",
                "import 'dart:\\u0063ore' hide String;\n")) {
            String candidate = header + "void build() {\n  print('candidate');\n}\n";
            Fixture accepted = fixture(Mode.PASS, limits(Duration.ofSeconds(3), 1024 * 1024));
            Files.writeString(accepted.dartFile, "void build() {}\n", StandardCharsets.UTF_8);
            Path sdkRoot = Files.createDirectories(temporaryDirectory.resolve("sdk/lib"));
            int offset = candidate.indexOf("print");
            var type = new DartStaticTypeProbe(offset, 5, 0, candidate.indexOf("  print"),
                    "ValueChanged<bool>", "package:flutter/material.dart");
            var probe = new DartSymbolProbe("typed.bool", offset, 5, "print", "dart:core",
                    sdkRoot, Optional.of("FUNCTION"), Optional.of(type));
            DartCandidateAnalysisResult result = await(accepted.analyzer.analyze(request(
                    accepted, candidate, 41, DartCandidateWarningPolicy.ALLOW, List.of(probe))));
            assertEquals(DartCandidateAnalysisStatus.PASSED, result.status());
            var additions = accepted.factory.processes().stream().flatMap(process -> process.requests().stream())
                    .filter(value -> "analysis.updateContent".equals(value.path("method").asText()))
                    .map(value -> value.path("params").path("files").path(accepted.dartFile.toString()))
                    .filter(value -> "add".equals(value.path("type").asText())).toList();
            assertEquals(2, additions.size());
            assertEquals(candidate, additions.getFirst().path("content").asText());
            String witness = additions.getLast().path("content").asText();
            assertTrue(witness.contains("import 'dart:core' as _nbfdStaticTypeProof0Core;"), witness);
            assertTrue(witness.contains("    _nbfdStaticTypeProof0Core.dynamic _nbfdStaticTypeProof0Dynamic() => null;"), witness);
            assertTrue(witness.contains("_nbfdStaticTypeProof0.ValueChanged<_nbfdStaticTypeProof0Core.bool>"), witness);
            assertEquals(!header.startsWith("import"), witness.startsWith(
                    "import 'package:flutter/material.dart' as _nbfdStaticTypeProof0;\nimport 'dart:core';"), witness);
        }
    }

    @Test
    void gestureCallbackProofsUseReviewedGestureImportAlongsideOneSharedUmbrellaAndPreserveSource() throws Exception {
        List<String> callbacks = List.of(
                "GestureDragCancelCallback",
                "GestureDragDownCallback",
                "GestureDragEndCallback",
                "GestureDragStartCallback",
                "GestureDragUpdateCallback",
                "GestureForcePressEndCallback",
                "GestureForcePressPeakCallback",
                "GestureForcePressStartCallback",
                "GestureForcePressUpdateCallback",
                "GestureLongPressCallback",
                "GestureLongPressCancelCallback",
                "GestureLongPressDownCallback",
                "GestureLongPressEndCallback",
                "GestureLongPressMoveUpdateCallback",
                "GestureLongPressStartCallback",
                "GestureLongPressUpCallback",
                "GestureScaleEndCallback",
                "GestureScaleStartCallback",
                "GestureScaleUpdateCallback",
                "GestureTapCallback",
                "GestureTapCancelCallback",
                "GestureTapDownCallback",
                "GestureTapMoveCallback",
                "GestureTapUpCallback");
        for (String library : List.of("package:flutter/widgets.dart", "package:flutter/material.dart")) {
            String header = "import 'package:flutter/gestures.dart' as sourceGestures show GestureTapCallback;\n"
                    + "const marker = '_nbfdStaticTypeProof0Gestures';\n"
                    + "class GestureTapMoveCallback {}\n";
            String candidate = header + "void build() {\n  print('candidate');\n}\n";
            String disk = "void build() {}\n";
            Fixture fixture = fixture(Mode.PASS, limits(Duration.ofSeconds(3), 1024 * 1024,
                    new DartCandidateCapacityBudget("gesture-proof-test", 2 * 1024 * 1024, 32, 0)));
            Files.writeString(fixture.dartFile, disk, StandardCharsets.UTF_8);
            Path sdkRoot = Files.createDirectories(temporaryDirectory.resolve("sdk/lib"));
            int offset = candidate.indexOf("print");
            var expected = new ArrayList<>(callbacks);
            expected.addAll(List.of("RangeValues", "TextEditingController", "ValueChanged<bool>", "double",
                    "WidgetStateProperty<GestureTapMoveCallback?>"));
            assertThrows(IllegalArgumentException.class, () -> new DartStaticTypeProbe(offset, 5, 0,
                    candidate.indexOf("  print"), "GestureTapMoveCallback?", library));
            List<DartSymbolProbe> probes = new ArrayList<>();
            for (int index = 0; index < expected.size(); index++) {
                var type = new DartStaticTypeProbe(offset, 5, 0, candidate.indexOf("  print"), expected.get(index), library);
                probes.add(new DartSymbolProbe("typed.gesture." + index, offset, 5, "print", "dart:core",
                        sdkRoot, Optional.of("FUNCTION"), Optional.of(type)));
            }
            var result = await(fixture.analyzer.analyze(request(
                    fixture, candidate, 51, DartCandidateWarningPolicy.ALLOW, probes)));
            assertEquals(DartCandidateAnalysisStatus.PASSED, result.status(), result.toString());
            assertEquals(expected.size(), result.symbolEvidence().size());
            assertTrue(result.symbolEvidence().stream().allMatch(evidence -> evidence.staticTypeEvidence().orElseThrow().accepted()));
            var additions = fixture.factory.processes().stream().flatMap(process -> process.requests().stream())
                    .filter(value -> "analysis.updateContent".equals(value.path("method").asText()))
                    .map(value -> value.path("params").path("files").path(fixture.dartFile.toString()))
                    .filter(value -> "add".equals(value.path("type").asText())).toList();
            assertEquals(2, additions.size());
            assertEquals(candidate, additions.getFirst().path("content").asText());
            String witness = additions.getLast().path("content").asText();
            String prefix = "_nbfdStaticTypeProof1";
            assertTrue(witness.contains("import '" + library + "' as " + prefix + ";"), witness);
            assertTrue(witness.contains("import 'package:flutter/gestures.dart' as " + prefix + "Gestures;"), witness);
            assertTrue(witness.contains("import 'dart:core' as " + prefix + "Core;"), witness);
            assertTrue(witness.contains(header), witness);
            for (int index = 0; index < callbacks.size(); index++) {
                assertTrue(witness.contains("final " + prefix + "Gestures." + callbacks.get(index)
                        + " " + prefix + "Value" + index + " = print;"), witness);
            }
            assertTrue(witness.contains("final " + prefix + ".RangeValues "), witness);
            assertTrue(witness.contains("final " + prefix + ".TextEditingController "), witness);
            assertTrue(witness.contains("final " + prefix + ".ValueChanged<" + prefix + "Core.bool> "), witness);
            assertTrue(witness.contains("final " + prefix + "Core.double "), witness);
            assertTrue(witness.contains("final " + prefix + ".WidgetStateProperty<" + prefix + "Gestures.GestureTapMoveCallback?> "), witness);
            assertEquals(disk, Files.readString(fixture.dartFile, StandardCharsets.UTF_8));
        }
    }

    @Test
    void gestureProofImportDoesNotMatchUnreviewedLookalikeTypeNames() throws Exception {
        String candidate = "void build() {\n  print('candidate');\n}\n";
        Fixture fixture = fixture(Mode.PASS, limits(Duration.ofSeconds(3), 1024 * 1024));
        Files.writeString(fixture.dartFile, "void build() {}\n", StandardCharsets.UTF_8);
        Path sdkRoot = Files.createDirectories(temporaryDirectory.resolve("sdk/lib"));
        int offset = candidate.indexOf("print");
        var type = new DartStaticTypeProbe(offset, 5, 0, candidate.indexOf("  print"),
                "GestureTapMoveCallbackExtra", "package:flutter/widgets.dart");
        var probe = new DartSymbolProbe("typed.lookalike", offset, 5, "print", "dart:core", sdkRoot,
                Optional.of("FUNCTION"), Optional.of(type));
        var result = await(fixture.analyzer.analyze(request(
                fixture, candidate, 52, DartCandidateWarningPolicy.ALLOW, List.of(probe))));
        assertEquals(DartCandidateAnalysisStatus.PASSED, result.status(), "Scripted analyzer only verifies witness structure");
        String witness = fixture.factory.processes().get(1).requests().stream()
                .filter(value -> "analysis.updateContent".equals(value.path("method").asText()))
                .map(value -> value.path("params").path("files").path(fixture.dartFile.toString()))
                .filter(value -> "add".equals(value.path("type").asText()))
                .findFirst().orElseThrow().path("content").asText();
        assertFalse(witness.contains("package:flutter/gestures.dart"), witness);
        assertTrue(witness.contains("_nbfdStaticTypeProof0.GestureTapMoveCallbackExtra"), witness);
    }

    @Test
    void gestureProofStillRejectsWrongTypesAndUnavailableStrictCastsControls() throws Exception {
        String candidate = "void build() {\n  print('candidate');\n}\n";
        Path sdkRoot = Files.createDirectories(temporaryDirectory.resolve("sdk/lib"));
        for (Mode mode : List.of(Mode.STATIC_TYPE_ERROR, Mode.SUPPRESSED_PROOF_CONTROL, Mode.DEMOTED_PROOF_CONTROL)) {
            Fixture fixture = fixture(mode, limits(Duration.ofSeconds(3), 1024 * 1024));
            String disk = "void build() {}\n";
            Files.writeString(fixture.dartFile, disk, StandardCharsets.UTF_8);
            int offset = candidate.indexOf("print");
            var type = new DartStaticTypeProbe(offset, 5, 0, candidate.indexOf("  print"),
                    "GestureTapMoveCallback", "package:flutter/widgets.dart");
            var probe = new DartSymbolProbe("typed.gesture", offset, 5, "print", "dart:core", sdkRoot,
                    Optional.of("FUNCTION"), Optional.of(type));
            var result = await(fixture.analyzer.analyze(request(
                    fixture, candidate, 53, DartCandidateWarningPolicy.ALLOW, List.of(probe))));
            assertFalse(result.status() == DartCandidateAnalysisStatus.PASSED, mode.toString());
            assertFalse(result.symbolEvidence().getFirst().staticTypeEvidence().orElseThrow().accepted(), mode.toString());
            assertEquals(disk, Files.readString(fixture.dartFile, StandardCharsets.UTF_8));
        }
    }

    @Test
    void sameNamedTypeFromANonFlutterProofLibraryRetainsItsRequestedIdentity() throws Exception {
        String candidate = "void build() {\n  print('candidate');\n}\n";
        Fixture fixture = fixture(Mode.PASS, limits(Duration.ofSeconds(3), 1024 * 1024));
        Files.writeString(fixture.dartFile, "void build() {}\n", StandardCharsets.UTF_8);
        Path sdkRoot = Files.createDirectories(temporaryDirectory.resolve("sdk/lib"));
        int offset = candidate.indexOf("print");
        var type = new DartStaticTypeProbe(offset, 5, 0, candidate.indexOf("  print"),
                "GestureTapMoveCallback", "package:app/types.dart");
        var probe = new DartSymbolProbe("typed.custom", offset, 5, "print", "dart:core", sdkRoot,
                Optional.of("FUNCTION"), Optional.of(type));
        var result = await(fixture.analyzer.analyze(request(
                fixture, candidate, 54, DartCandidateWarningPolicy.ALLOW, List.of(probe))));
        assertEquals(DartCandidateAnalysisStatus.PASSED, result.status(), "Scripted analyzer only verifies witness structure");
        String witness = fixture.factory.processes().get(1).requests().stream()
                .filter(value -> "analysis.updateContent".equals(value.path("method").asText()))
                .map(value -> value.path("params").path("files").path(fixture.dartFile.toString()))
                .filter(value -> "add".equals(value.path("type").asText()))
                .findFirst().orElseThrow().path("content").asText();
        assertTrue(witness.contains("import 'package:app/types.dart' as _nbfdStaticTypeProof0;"), witness);
        assertFalse(witness.contains("package:flutter/gestures.dart"), witness);
        assertTrue(witness.contains("final _nbfdStaticTypeProof0.GestureTapMoveCallback "), witness);
    }

    @Test
    void listenerAndMouseRegionProofsUseExactRenderingAndServicesOwnersWithoutChangingSourceImports() throws Exception {
        List<String> names = List.of("PointerDownEventListener", "PointerMoveEventListener", "PointerUpEventListener",
                "PointerHoverEventListener", "PointerCancelEventListener", "PointerPanZoomStartEventListener",
                "PointerPanZoomUpdateEventListener", "PointerPanZoomEndEventListener", "PointerSignalEventListener",
                "PointerEnterEventListener", "PointerExitEventListener");
        for (String library : List.of("package:flutter/widgets.dart", "package:flutter/material.dart")) {
            String candidate = "const marker = '_nbfdStaticTypeProof0Rendering';\n"
                    + "class PointerSignalEventListener {}\nvoid build() {\n  print('candidate');\n}\n";
            Fixture fixture = fixture(Mode.PASS, limits(Duration.ofSeconds(3), 1024 * 1024));
            String original = "void build() {}\n";
            Files.writeString(fixture.dartFile, original);
            Path sdkRoot = Files.createDirectories(temporaryDirectory.resolve("sdk/lib"));
            int offset = candidate.indexOf("print");
            List<DartSymbolProbe> probes = new ArrayList<>();
            for (String name : names) {
                var type = new DartStaticTypeProbe(offset, 5, 0, candidate.indexOf("  print"), name, library);
                probes.add(new DartSymbolProbe("listener." + name, offset, 5, "print", "dart:core", sdkRoot,
                        Optional.of("FUNCTION"), Optional.of(type)));
            }
            var result = await(fixture.analyzer.analyze(request(fixture, candidate, 55, DartCandidateWarningPolicy.ALLOW, probes)));
            assertEquals(DartCandidateAnalysisStatus.PASSED, result.status(), result.toString());
            assertEquals(11, result.symbolEvidence().size());
            assertTrue(result.symbolEvidence().stream().allMatch(e -> e.staticTypeEvidence().orElseThrow().accepted()));
            String witness = witnessContent(fixture);
            assertTrue(witness.contains("import 'package:flutter/rendering.dart' as _nbfdStaticTypeProof1Rendering;"), witness);
            assertTrue(witness.contains("import 'package:flutter/services.dart' as _nbfdStaticTypeProof1Services;"), witness);
            for (String name : names) {
                String owner = List.of("PointerHoverEventListener", "PointerEnterEventListener", "PointerExitEventListener")
                        .contains(name) ? "Services" : "Rendering";
                assertTrue(witness.contains("final _nbfdStaticTypeProof1" + owner + "." + name + " "), witness);
            }
            assertFalse(witness.contains("package:flutter/gestures.dart"));
            assertTrue(witness.contains("class PointerSignalEventListener {}"));
            assertEquals(original, Files.readString(fixture.dartFile));
        }
    }

    @Test
    void listenerProofRoutingDoesNotRewriteForeignTypesOrNameLookalikes() throws Exception {
        String candidate = "void build() {\n  print('candidate');\n}\n";
        for (String library : List.of("package:app/types.dart", "package:flutter/widgets.dart")) {
            String name = library.startsWith("package:app/") ? "PointerSignalEventListener" : "PointerSignalEventListenerExtra";
            Fixture fixture = fixture(Mode.PASS, limits(Duration.ofSeconds(3), 1024 * 1024));
            Files.writeString(fixture.dartFile, "void build() {}\n");
            Path sdkRoot = Files.createDirectories(temporaryDirectory.resolve("sdk/lib"));
            int offset = candidate.indexOf("print");
            var type = new DartStaticTypeProbe(offset, 5, 0, candidate.indexOf("  print"), name, library);
            var probe = new DartSymbolProbe("listener.lookalike", offset, 5, "print", "dart:core", sdkRoot,
                    Optional.of("FUNCTION"), Optional.of(type));
            var result = await(fixture.analyzer.analyze(request(fixture, candidate, 56, DartCandidateWarningPolicy.ALLOW, List.of(probe))));
            assertEquals(DartCandidateAnalysisStatus.PASSED, result.status(), "Scripted analyzer checks witness structure only");
            String witness = witnessContent(fixture);
            assertFalse(witness.contains("package:flutter/rendering.dart"), witness);
            assertFalse(witness.contains("package:flutter/services.dart"), witness);
            assertTrue(witness.contains("final _nbfdStaticTypeProof0." + name + " "), witness);
        }
    }

    @Test
    void listenerProofStillRejectsWrongTypesAndMissingStrictCastsControls() throws Exception {
        String candidate = "void build() {\n  print('candidate');\n}\n";
        for (Mode mode : List.of(Mode.STATIC_TYPE_ERROR, Mode.SUPPRESSED_PROOF_CONTROL, Mode.DEMOTED_PROOF_CONTROL)) {
            Fixture fixture = fixture(mode, limits(Duration.ofSeconds(3), 1024 * 1024));
            Files.writeString(fixture.dartFile, "void build() {}\n");
            Path sdkRoot = Files.createDirectories(temporaryDirectory.resolve("sdk/lib"));
            int offset = candidate.indexOf("print");
            var type = new DartStaticTypeProbe(offset, 5, 0, candidate.indexOf("  print"),
                    "PointerHoverEventListener", "package:flutter/widgets.dart");
            var probe = new DartSymbolProbe("listener.bad", offset, 5, "print", "dart:core", sdkRoot,
                    Optional.of("FUNCTION"), Optional.of(type));
            var result = await(fixture.analyzer.analyze(request(fixture, candidate, 57, DartCandidateWarningPolicy.ALLOW, List.of(probe))));
            assertFalse(result.status() == DartCandidateAnalysisStatus.PASSED, mode.toString());
            assertFalse(result.symbolEvidence().getFirst().staticTypeEvidence().orElseThrow().accepted(), mode.toString());
        }
    }

    private static String witnessContent(Fixture fixture) {
        return fixture.factory.processes().get(1).requests().stream()
                .filter(value -> "analysis.updateContent".equals(value.path("method").asText()))
                .map(value -> value.path("params").path("files").path(fixture.dartFile.toString()))
                .filter(value -> "add".equals(value.path("type").asText()))
                .findFirst().orElseThrow().path("content").asText();
    }

    @Test
    void numericStatePropertyProofQualifiesCoreWithoutChangingOriginalImportScope() throws Exception {
        for (String argument : List.of("double", "double?")) {
            for (String header : List.of("", "const marker = \"import 'dart:core';\";\n",
                    "import 'dart:core' as sourceCore show double;\n",
                    "import 'dart:core' show String;\n",
                    "import 'dart:\\u0063ore' hide double;\n",
                    "import 'dart:core' as sourceCore;\nclass double {}\n")) {
                String candidate = header + "void build() {\n  print('candidate');\n}\n";
                Fixture accepted = fixture(Mode.PASS, limits(Duration.ofSeconds(3), 1024 * 1024));
                String original = "void build() {}\n";
                Files.writeString(accepted.dartFile, original, StandardCharsets.UTF_8);
                Path sdkRoot = Files.createDirectories(temporaryDirectory.resolve("sdk/lib"));
                int offset = candidate.indexOf("print");
                var type = new DartStaticTypeProbe(offset, 5, 0, candidate.indexOf("  print"),
                        "WidgetStateProperty<" + argument + ">", "package:flutter/widgets.dart");
                var probe = new DartSymbolProbe("typed.double", offset, 5, "print", "dart:core",
                        sdkRoot, Optional.of("FUNCTION"), Optional.of(type));
                var result = await(accepted.analyzer.analyze(request(
                        accepted, candidate, 41, DartCandidateWarningPolicy.ALLOW, List.of(probe))));
                assertEquals(DartCandidateAnalysisStatus.PASSED, result.status());
                var additions = accepted.factory.processes().stream().flatMap(process -> process.requests().stream())
                        .filter(value -> "analysis.updateContent".equals(value.path("method").asText()))
                        .map(value -> value.path("params").path("files").path(accepted.dartFile.toString()))
                        .filter(value -> "add".equals(value.path("type").asText())).toList();
                assertEquals(2, additions.size());
                assertEquals(candidate, additions.getFirst().path("content").asText());
                String witness = additions.getLast().path("content").asText();
                assertTrue(witness.contains("import 'dart:core' as _nbfdStaticTypeProof0Core;"), witness);
                assertTrue(witness.contains("final _nbfdStaticTypeProof0.WidgetStateProperty<"
                        + "_nbfdStaticTypeProof0Core." + argument + "> _nbfdStaticTypeProof0Value0 = print;"), witness);
                assertFalse(witness.contains("_nbfdStaticTypeProof0.double"), witness);
                assertEquals(!header.startsWith("import"), witness.startsWith(
                        "import 'package:flutter/widgets.dart' as _nbfdStaticTypeProof0;\nimport 'dart:core';"), witness);
                assertTrue(witness.contains(header + "void build()"), witness);
                assertTrue(accepted.factory.processes().get(1).requests().getFirst().toString()
                        .contains("strict-casts: true"));
                assertEquals(original, Files.readString(accepted.dartFile, StandardCharsets.UTF_8));
            }
        }
    }

    @Test
    void nonBooleanTypedBatchesQualifyTheDynamicControlWithoutWideningCoreScope() throws Exception {
        for (String expectedType : List.of("ButtonStyle", "VoidCallback", "FocusNode", "FocusNode?",
                "FocusOnKeyEventCallback", "FocusOnKeyCallback", "Duration")) {
            for (String header : List.of("", "import 'dart:core' as sourceCore;\n",
                    "import 'dart:core' show String;\n", "import 'dart:core' show String;\nclass Duration {}\n")) {
                String candidate = header + "void build() {\n  print('candidate');\n}\n";
                Fixture accepted = fixture(Mode.PASS, limits(Duration.ofSeconds(3), 1024 * 1024));
                Files.writeString(accepted.dartFile, "void build() {}\n", StandardCharsets.UTF_8);
                Path sdkRoot = Files.createDirectories(temporaryDirectory.resolve("sdk/lib"));
                int offset = candidate.indexOf("print");
                var type = new DartStaticTypeProbe(offset, 5, 0, candidate.indexOf("  print"),
                        expectedType, "package:flutter/material.dart");
                var probe = new DartSymbolProbe("typed.nonBool", offset, 5, "print", "dart:core",
                        sdkRoot, Optional.of("FUNCTION"), Optional.of(type));
                var result = await(accepted.analyzer.analyze(request(
                        accepted, candidate, 41, DartCandidateWarningPolicy.ALLOW, List.of(probe))));
                assertEquals(DartCandidateAnalysisStatus.PASSED, result.status());
                String witness = accepted.factory.processes().stream().flatMap(process -> process.requests().stream())
                        .filter(value -> "analysis.updateContent".equals(value.path("method").asText()))
                        .map(value -> value.path("params").path("files").path(accepted.dartFile.toString()))
                        .filter(value -> "add".equals(value.path("type").asText()))
                        .map(value -> value.path("content").asText()).reduce((first, last) -> last).orElseThrow();
                assertTrue(witness.contains("import 'dart:core' as _nbfdStaticTypeProof0Core;"), witness);
                assertTrue(witness.contains("    _nbfdStaticTypeProof0Core.dynamic _nbfdStaticTypeProof0Dynamic() => null;"), witness);
                assertTrue(witness.contains("final _nbfdStaticTypeProof0Core.int"
                        + " _nbfdStaticTypeProof0Control = _nbfdStaticTypeProof0Dynamic();"), witness);
                String expectedOwner = expectedType.equals("Duration") ? "_nbfdStaticTypeProof0Core" : "_nbfdStaticTypeProof0";
                assertTrue(witness.contains("final " + expectedOwner + '.' + expectedType
                        + " _nbfdStaticTypeProof0Value0 = print;"), witness);
                if (expectedType.equals("Duration")) assertFalse(witness.contains("final _nbfdStaticTypeProof0.Duration "), witness);
                assertEquals(header.isEmpty(), witness.startsWith(
                        "import 'package:flutter/material.dart' as _nbfdStaticTypeProof0;\nimport 'dart:core';"), witness);
                assertTrue(witness.contains(header + "void build()"), witness);
            }
        }
    }

    @Test
    void scrimBuilderProofQualifiesTheAnonymousFunctionAndChecksItsNullableWidgetResult() throws Exception {
        for (String library : List.of("package:flutter/widgets.dart", "package:flutter/material.dart")) {
            for (String header : List.of("", "import 'dart:core' as sourceCore;\n")) {
                String candidate = header + "const collision = '_nbfdStaticTypeProof0';\n"
                        + "class Widget {}\nclass BuildContext {}\nclass Animation<T> {}\n"
                        + "void build() {\n  print('candidate');\n}\n";
                Fixture accepted = fixture(Mode.PASS, limits(Duration.ofSeconds(3), 1024 * 1024));
                Files.writeString(accepted.dartFile, "void build() {}\n", StandardCharsets.UTF_8);
                Path sdkRoot = Files.createDirectories(temporaryDirectory.resolve("sdk/lib"));
                int offset = candidate.indexOf("print");
                var type = new DartStaticTypeProbe(offset, 5, 0, candidate.indexOf("  print"),
                        "Widget? Function(BuildContext, Animation<double>)", library);
                var probe = new DartSymbolProbe("typed.scrim", offset, 5, "print", "dart:core",
                        sdkRoot, Optional.of("FUNCTION"), Optional.of(type));
                var result = await(accepted.analyzer.analyze(request(accepted, candidate, 41, DartCandidateWarningPolicy.ALLOW, List.of(probe))));
                assertEquals(DartCandidateAnalysisStatus.PASSED, result.status(), "Scripted analyzer verifies witness structure only");
                String witness = witnessContent(accepted);
                String prefix = "_nbfdStaticTypeProof1";
                assertTrue(witness.contains("final " + prefix + ".Widget? Function(" + prefix + ".BuildContext, "
                        + prefix + ".Animation<" + prefix + "Core.double>) " + prefix + "Value0 = print;"), witness);
                assertTrue(witness.contains("final " + prefix + ".Widget? " + prefix + "ScrimResult0 = (print)("
                        + prefix + "ScrimContext0(), " + prefix + "ScrimAnimation0());"), witness);
                assertTrue(witness.contains(prefix + ".BuildContext " + prefix + "ScrimContext0() => throw 0;"), witness);
                assertTrue(witness.contains("final " + prefix + "Core.int " + prefix + "Control = " + prefix + "Dynamic();"), witness);
                assertEquals(header.isEmpty(), witness.contains("import 'dart:core';"), witness);
                assertEquals("void build() {}\n", Files.readString(accepted.dartFile));
            }
        }
        for (Mode mode : List.of(Mode.STATIC_TYPE_ERROR, Mode.SUPPRESSED_PROOF_CONTROL, Mode.DEMOTED_PROOF_CONTROL)) {
            Fixture rejected = fixture(mode, limits(Duration.ofSeconds(3), 1024 * 1024));
            Files.writeString(rejected.dartFile, "void build() {}\n", StandardCharsets.UTF_8);
            String candidate = "void build() {\n  print('candidate');\n}\n";
            int offset = candidate.indexOf("print");
            var type = new DartStaticTypeProbe(offset, 5, 0, candidate.indexOf("  print"),
                    "Widget? Function(BuildContext, Animation<double>)", "package:flutter/widgets.dart");
            var probe = new DartSymbolProbe("typed.scrim", offset, 5, "print", "dart:core",
                    Files.createDirectories(temporaryDirectory.resolve("sdk/lib")), Optional.of("FUNCTION"), Optional.of(type));
            var result = await(rejected.analyzer.analyze(request(rejected, candidate, 41, DartCandidateWarningPolicy.ALLOW, List.of(probe))));
            assertFalse(result.symbolEvidence().getFirst().staticTypeEvidence().orElseThrow().accepted(), mode.toString());
        }
    }

    @Test
    void scrollPredicateProofQualifiesTheSdkArgumentAndIndependentlyChecksBoolResult() throws Exception {
        for (String library : List.of("package:flutter/widgets.dart", "package:flutter/material.dart")) {
            for (String header : List.of("", "import 'dart:core' as sourceCore;\n")) {
                String candidate = header + "const collision = '_nbfdStaticTypeProof0';\n"
                        + "class ScrollNotification {}\n"
                        + "void build() {\n  print('candidate');\n}\n";
                Fixture accepted = fixture(Mode.PASS, limits(Duration.ofSeconds(3), 1024 * 1024));
                Files.writeString(accepted.dartFile, "void build() {}\n", StandardCharsets.UTF_8);
                Path sdkRoot = Files.createDirectories(temporaryDirectory.resolve("sdk/lib"));
                int offset = candidate.indexOf("print");
                var type = new DartStaticTypeProbe(offset, 5, 0, candidate.indexOf("  print"),
                        "ScrollNotificationPredicate", library);
                var probe = new DartSymbolProbe("typed.predicate", offset, 5, "print", "dart:core",
                        sdkRoot, Optional.of("FUNCTION"), Optional.of(type));
                var result = await(accepted.analyzer.analyze(request(accepted, candidate, 41, DartCandidateWarningPolicy.ALLOW, List.of(probe))));
                assertEquals(DartCandidateAnalysisStatus.PASSED, result.status(), "Scripted analyzer verifies witness structure only");
                String witness = witnessContent(accepted);
                String prefix = "_nbfdStaticTypeProof1";
                assertTrue(witness.contains("final " + prefix + ".ScrollNotificationPredicate "
                        + prefix + "Value0 = print;"), witness);
                assertTrue(witness.contains("final " + prefix + "Core.bool " + prefix + "PredicateResult0 = (print)("
                        + prefix + "ScrollNotification0());"), witness);
                assertTrue(witness.contains(prefix + ".ScrollNotification " + prefix
                        + "ScrollNotification0() => throw 0;"), witness);
                assertTrue(witness.contains("final " + prefix + "Core.int " + prefix + "Control = " + prefix + "Dynamic();"), witness);
                assertEquals(header.isEmpty(), witness.contains("import 'dart:core';"), witness);
                assertEquals("void build() {}\n", Files.readString(accepted.dartFile));
            }
        }
        for (Mode mode : List.of(Mode.STATIC_TYPE_ERROR, Mode.SUPPRESSED_PROOF_CONTROL, Mode.DEMOTED_PROOF_CONTROL)) {
            Fixture rejected = fixture(mode, limits(Duration.ofSeconds(3), 1024 * 1024));
            Files.writeString(rejected.dartFile, "void build() {}\n", StandardCharsets.UTF_8);
            String candidate = "void build() {\n  print('candidate');\n}\n";
            int offset = candidate.indexOf("print");
            var type = new DartStaticTypeProbe(offset, 5, 0, candidate.indexOf("  print"),
                    "ScrollNotificationPredicate", "package:flutter/widgets.dart");
            var probe = new DartSymbolProbe("typed.predicate", offset, 5, "print", "dart:core",
                    Files.createDirectories(temporaryDirectory.resolve("sdk/lib")), Optional.of("FUNCTION"), Optional.of(type));
            var result = await(rejected.analyzer.analyze(request(rejected, candidate, 41, DartCandidateWarningPolicy.ALLOW, List.of(probe))));
            assertFalse(result.symbolEvidence().getFirst().staticTypeEvidence().orElseThrow().accepted(), mode.toString());
        }
    }

    @Test
    void buttonLayerProofQualifiesContextStateSetAndNullableChildAndRequiresNonnullWidgetResult() throws Exception {
        for (String library : List.of("package:flutter/material.dart")) {
            for (String header : List.of("", "import 'dart:core' as sourceCore;\n")) {
                String candidate = header + "const collision = '_nbfdStaticTypeProof0';\n"
                        + "class Widget {}\nclass BuildContext {}\nclass WidgetState {}\n"
                        + "void build() {\n  print('candidate');\n}\n";
                Fixture accepted = fixture(Mode.PASS, limits(Duration.ofSeconds(3), 1024 * 1024));
                Files.writeString(accepted.dartFile, "void build() {}\n", StandardCharsets.UTF_8);
                Path sdkRoot = Files.createDirectories(temporaryDirectory.resolve("sdk/lib"));
                int offset = candidate.indexOf("print");
                var type = new DartStaticTypeProbe(offset, 5, 0, candidate.indexOf("  print"),
                        "ButtonLayerBuilder", library);
                var probe = new DartSymbolProbe("typed.layer", offset, 5, "print", "dart:core",
                        sdkRoot, Optional.of("FUNCTION"), Optional.of(type));
                var result = await(accepted.analyzer.analyze(request(accepted, candidate, 41, DartCandidateWarningPolicy.ALLOW, List.of(probe))));
                assertEquals(DartCandidateAnalysisStatus.PASSED, result.status(), "Scripted analyzer verifies witness structure only");
                String witness = witnessContent(accepted);
                String prefix = "_nbfdStaticTypeProof1";
                assertTrue(witness.contains("final " + prefix + ".ButtonLayerBuilder " + prefix + "Value0 = print;"), witness);
                assertTrue(witness.contains(prefix + ".BuildContext " + prefix + "LayerContext0() => throw 0;"), witness);
                assertTrue(witness.contains(prefix + "Core.Set<" + prefix + ".WidgetState> " + prefix + "LayerStates0() => throw 0;"), witness);
                assertTrue(witness.contains(prefix + ".Widget? " + prefix + "LayerChild0() => throw 0;"), witness);
                assertTrue(witness.contains("final " + prefix + ".Widget " + prefix + "LayerResult0 = (print)("
                        + prefix + "LayerContext0(), " + prefix + "LayerStates0(), " + prefix + "LayerChild0());"), witness);
                assertTrue(witness.contains("final " + prefix + "Core.int " + prefix + "Control = " + prefix + "Dynamic();"), witness);
                assertEquals(header.isEmpty(), witness.contains("import 'dart:core';"), witness);
                assertEquals("void build() {}\n", Files.readString(accepted.dartFile));
            }
        }
        for (Mode mode : List.of(Mode.STATIC_TYPE_ERROR, Mode.SUPPRESSED_PROOF_CONTROL, Mode.DEMOTED_PROOF_CONTROL)) {
            Fixture rejected = fixture(mode, limits(Duration.ofSeconds(3), 1024 * 1024));
            Files.writeString(rejected.dartFile, "void build() {}\n", StandardCharsets.UTF_8);
            String candidate = "void build() {\n  print('candidate');\n}\n";
            int offset = candidate.indexOf("print");
            var type = new DartStaticTypeProbe(offset, 5, 0, candidate.indexOf("  print"),
                    "ButtonLayerBuilder", "package:flutter/material.dart");
            var probe = new DartSymbolProbe("typed.layer", offset, 5, "print", "dart:core",
                    Files.createDirectories(temporaryDirectory.resolve("sdk/lib")), Optional.of("FUNCTION"), Optional.of(type));
            var result = await(rejected.analyzer.analyze(request(rejected, candidate, 41, DartCandidateWarningPolicy.ALLOW, List.of(probe))));
            assertFalse(result.symbolEvidence().getFirst().staticTypeEvidence().orElseThrow().accepted(), mode.toString());
        }
    }

    @Test
    void textFieldBuilderProofsKeepRequiredNamedArgumentsAndNullableCallbacksSeparate() throws Exception {
        for (String base : List.of("InputCounterWidgetBuilder", "EditableTextContextMenuBuilder")) {
            for (String suffix : List.of("", "?")) for (String header : List.of("", "import 'dart:core' as sourceCore;\n")) {
                String candidate = header + "const collision = '_nbfdStaticTypeProof0';\n"
                        + "class Widget {}\nclass BuildContext {}\nclass EditableTextState {}\n"
                        + "void build() {\n  print('candidate');\n}\n";
                Fixture accepted = fixture(Mode.PASS, limits(Duration.ofSeconds(3), 1024 * 1024));
                Files.writeString(accepted.dartFile, "void build() {}\n", StandardCharsets.UTF_8);
                int offset = candidate.indexOf("print");
                var type = new DartStaticTypeProbe(offset, 5, 0, candidate.indexOf("  print"),
                        base + suffix, "package:flutter/material.dart");
                var probe = new DartSymbolProbe("typed.textFieldBuilder", offset, 5, "print", "dart:core",
                        Files.createDirectories(temporaryDirectory.resolve("sdk/lib")), Optional.of("FUNCTION"), Optional.of(type));
                var result = await(accepted.analyzer.analyze(request(accepted, candidate, 41, DartCandidateWarningPolicy.ALLOW, List.of(probe))));
                assertEquals(DartCandidateAnalysisStatus.PASSED, result.status(), "Scripted analyzer verifies witness structure only");
                String witness = witnessContent(accepted), prefix = "_nbfdStaticTypeProof1";
                assertTrue(witness.contains("final " + prefix + "." + base + suffix + " " + prefix + "Value0 = print;"), witness);
                String call = suffix.isEmpty() ? "(print)(" : "(print)?.call(";
                if (base.equals("InputCounterWidgetBuilder")) {
                    assertTrue(witness.contains(prefix + ".BuildContext " + prefix + "CounterContext0() => throw 0;"), witness);
                    assertTrue(witness.contains(prefix + "Core.int " + prefix + "CounterLength0() => throw 0;"), witness);
                    assertTrue(witness.contains(prefix + "Core.int? " + prefix + "CounterMaxLength0() => throw 0;"), witness);
                    assertTrue(witness.contains(prefix + "Core.bool " + prefix + "CounterFocused0() => throw 0;"), witness);
                    assertTrue(witness.contains("final " + prefix + ".Widget? " + prefix + "CounterResult0 = " + call
                            + prefix + "CounterContext0(), currentLength: " + prefix + "CounterLength0(), maxLength: "
                            + prefix + "CounterMaxLength0(), isFocused: " + prefix + "CounterFocused0());"), witness);
                } else {
                    assertTrue(witness.contains(prefix + ".EditableTextState " + prefix + "MenuState0() => throw 0;"), witness);
                    assertTrue(witness.contains("final " + prefix + ".Widget" + suffix + " " + prefix + "MenuResult0 = "
                            + call + prefix + "MenuContext0(), " + prefix + "MenuState0());"), witness);
                }
                assertEquals(header.isEmpty(), witness.contains("import 'dart:core';"), witness);
                assertTrue(witness.contains("final " + prefix + "Core.int " + prefix + "Control = " + prefix + "Dynamic();"), witness);
                assertEquals("void build() {}\n", Files.readString(accepted.dartFile));
            }
        }
    }

    @Test
    void textFieldBuilderProofsRejectFailedSuppressedAndDemotedControls() throws Exception {
        for (String expected : List.of("InputCounterWidgetBuilder?", "EditableTextContextMenuBuilder?")) {
            for (Mode mode : List.of(Mode.STATIC_TYPE_ERROR, Mode.SUPPRESSED_PROOF_CONTROL, Mode.DEMOTED_PROOF_CONTROL)) {
                Fixture rejected = fixture(mode, limits(Duration.ofSeconds(3), 1024 * 1024));
                Files.writeString(rejected.dartFile, "void build() {}\n", StandardCharsets.UTF_8);
                String candidate = "void build() {\n  print('candidate');\n}\n";
                int offset = candidate.indexOf("print");
                var type = new DartStaticTypeProbe(offset, 5, 0, candidate.indexOf("  print"), expected, "package:flutter/material.dart");
                var probe = new DartSymbolProbe("typed.textFieldBuilder", offset, 5, "print", "dart:core",
                        Files.createDirectories(temporaryDirectory.resolve("sdk/lib")), Optional.of("FUNCTION"), Optional.of(type));
                var result = await(rejected.analyzer.analyze(request(rejected, candidate, 41, DartCandidateWarningPolicy.ALLOW, List.of(probe))));
                assertFalse(result.symbolEvidence().getFirst().staticTypeEvidence().orElseThrow().accepted(), expected + mode);
            }
        }
    }

    @Test
    void appBarFoundationAndServicesProofTypesUseFixedAliasesWithoutAmbientShadowing() throws Exception {
        for (String type : List.of("AsyncCallback", "SystemUiOverlayStyle?")) for (String library : List.of("package:flutter/widgets.dart", "package:flutter/material.dart")) {
            String candidate="const collision = '_nbfdStaticTypeProof0';\nclass AsyncCallback {}\nclass SystemUiOverlayStyle {}\nvoid build() {\n  print('candidate');\n}\n";
            Fixture accepted=fixture(Mode.PASS,limits(Duration.ofSeconds(3),1024*1024));
            Files.writeString(accepted.dartFile,"void build() {}\n",StandardCharsets.UTF_8);
            int offset=candidate.indexOf("print");
            var proof=new DartStaticTypeProbe(offset,5,0,candidate.indexOf("  print"),type,library);
            var probe=new DartSymbolProbe("typed.appbar",offset,5,"print","dart:core",
                    Files.createDirectories(temporaryDirectory.resolve("sdk/lib")),Optional.of("FUNCTION"),Optional.of(proof));
            var result=await(accepted.analyzer.analyze(request(accepted,candidate,41,DartCandidateWarningPolicy.ALLOW,List.of(probe))));
            assertEquals(DartCandidateAnalysisStatus.PASSED,result.status(),"Scripted analyzer checks witness structure; real SDK test checks assignability");
            String witness=witnessContent(accepted),prefix="_nbfdStaticTypeProof1";
            String namespace=type.equals("AsyncCallback")?"Foundation":"Services",file=type.equals("AsyncCallback")?"foundation":"services";
            assertTrue(witness.contains("import 'package:flutter/"+file+".dart' as "+prefix+namespace+";"),witness);
            assertTrue(witness.contains("final "+prefix+namespace+"."+type+" "+prefix+"Value0 = print;"),witness);
            assertTrue(witness.contains(prefix+"Core.int "+prefix+"Control = "+prefix+"Dynamic();"),witness);
            assertEquals("void build() {}\n",Files.readString(accepted.dartFile));
        }
    }

    @Test
    void itemExtentBuilderProofUsesExactRenderingAndCoreTypesWithoutAmbientShadowing() throws Exception {
        for (String library : List.of("package:flutter/widgets.dart", "package:flutter/material.dart")) {
            for (String suffix : List.of("", "?")) for (String header : List.of("", "import 'dart:core' as sourceCore;\n")) {
                String candidate = header + "const collision = '_nbfdStaticTypeProof0';\n"
                        + "class ItemExtentBuilder {}\nclass SliverLayoutDimensions {}\n"
                        + "void build() {\n  print('candidate');\n}\n";
                Fixture accepted = fixture(Mode.PASS, limits(Duration.ofSeconds(3), 1024 * 1024));
                Files.writeString(accepted.dartFile, "void build() {}\n", StandardCharsets.UTF_8);
                int offset = candidate.indexOf("print");
                var type = new DartStaticTypeProbe(offset, 5, 0, candidate.indexOf("  print"), "ItemExtentBuilder" + suffix, library);
                var probe = new DartSymbolProbe("typed.extent", offset, 5, "print", "dart:core",
                        Files.createDirectories(temporaryDirectory.resolve("sdk/lib")), Optional.of("FUNCTION"), Optional.of(type));
                var result = await(accepted.analyzer.analyze(request(accepted, candidate, 41, DartCandidateWarningPolicy.ALLOW, List.of(probe))));
                assertEquals(DartCandidateAnalysisStatus.PASSED, result.status(), "Scripted analyzer verifies witness structure only");
                String witness = witnessContent(accepted), prefix = "_nbfdStaticTypeProof1";
                assertTrue(witness.contains("import 'package:flutter/rendering.dart' as " + prefix + "Rendering;"), witness);
                assertFalse(witness.contains("import 'package:flutter/services.dart' as " + prefix + "Services;"), witness);
                assertTrue(witness.contains("final " + prefix + "Rendering.ItemExtentBuilder" + suffix + " " + prefix + "Value0 = print;"), witness);
                assertTrue(witness.contains(prefix + "Core.int " + prefix + "ExtentIndex0() => throw 0;"), witness);
                assertTrue(witness.contains(prefix + "Rendering.SliverLayoutDimensions " + prefix + "ExtentDimensions0() => throw 0;"), witness);
                String call = suffix.isEmpty() ? "(print)(" : "(print)?.call(";
                assertTrue(witness.contains("final " + prefix + "Core.double? " + prefix + "ExtentResult0 = " + call
                        + prefix + "ExtentIndex0(), " + prefix + "ExtentDimensions0());"), witness);
                assertEquals(header.isEmpty(), witness.contains("import 'dart:core';"), witness);
                assertTrue(witness.contains("final " + prefix + "Core.int " + prefix + "Control = " + prefix + "Dynamic();"), witness);
                assertEquals("void build() {}\n", Files.readString(accepted.dartFile));
            }
        }
    }

    @Test
    void itemExtentBuilderProofRejectsFailedSuppressedAndDemotedControls() throws Exception {
        for (String expected : List.of("ItemExtentBuilder", "ItemExtentBuilder?")) {
            for (Mode mode : List.of(Mode.STATIC_TYPE_ERROR, Mode.SUPPRESSED_PROOF_CONTROL, Mode.DEMOTED_PROOF_CONTROL)) {
                Fixture rejected = fixture(mode, limits(Duration.ofSeconds(3), 1024 * 1024));
                Files.writeString(rejected.dartFile, "void build() {}\n", StandardCharsets.UTF_8);
                String candidate = "void build() {\n  print('candidate');\n}\n";
                int offset = candidate.indexOf("print");
                var type = new DartStaticTypeProbe(offset, 5, 0, candidate.indexOf("  print"), expected, "package:flutter/widgets.dart");
                var probe = new DartSymbolProbe("typed.extent", offset, 5, "print", "dart:core",
                        Files.createDirectories(temporaryDirectory.resolve("sdk/lib")), Optional.of("FUNCTION"), Optional.of(type));
                var result = await(rejected.analyzer.analyze(request(rejected, candidate, 41, DartCandidateWarningPolicy.ALLOW, List.of(probe))));
                assertFalse(result.symbolEvidence().getFirst().staticTypeEvidence().orElseThrow().accepted(), expected + mode);
            }
        }
    }

    @Test
    void sliverCallbackProofsConsumeOriginalCallResultsWithExactCoreAndFlutterTypes() throws Exception {
        for (String expected : List.of("NullableIndexedWidgetBuilder", "IndexedWidgetBuilder", "ChildIndexGetter?", "SliverLayoutWidgetBuilder", "LayoutWidgetBuilder", "OrientationWidgetBuilder", "TransitionBuilder", "AnimatedCrossFadeBuilder", "AnimatedSwitcherTransitionBuilder", "AnimatedSwitcherLayoutBuilder")) {
            String candidate = "const collision = '_nbfdStaticTypeProof0';\nvoid build() {\n  print('candidate');\n}\n";
            Fixture accepted = fixture(Mode.PASS, limits(Duration.ofSeconds(3), 1024 * 1024));
            Files.writeString(accepted.dartFile, "void build() {}\n", StandardCharsets.UTF_8);
            int offset = candidate.indexOf("print");
            var type = new DartStaticTypeProbe(offset, 5, 0, candidate.indexOf("  print"), expected, "package:flutter/widgets.dart");
            var probe = new DartSymbolProbe("typed.sliver", offset, 5, "print", "dart:core",
                    Files.createDirectories(temporaryDirectory.resolve("sdk/lib")), Optional.of("FUNCTION"), Optional.of(type));
            var result = await(accepted.analyzer.analyze(request(accepted, candidate, 41, DartCandidateWarningPolicy.ALLOW, List.of(probe))));
            assertEquals(DartCandidateAnalysisStatus.PASSED, result.status());
            String witness = witnessContent(accepted), prefix = "_nbfdStaticTypeProof1";
            if (java.util.Set.of("SliverLayoutWidgetBuilder", "LayoutWidgetBuilder", "OrientationWidgetBuilder").contains(expected)) {
                assertEquals(!expected.equals("OrientationWidgetBuilder"), witness.contains("import 'package:flutter/rendering.dart' as " + prefix + "Rendering;"), witness);
                assertTrue(witness.contains(prefix + (expected.equals("OrientationWidgetBuilder") ? ".Orientation " : expected.equals("LayoutWidgetBuilder") ? "Rendering.BoxConstraints " : "Rendering.SliverConstraints ") + prefix + "LayoutConstraints0() => throw 0;"), witness);
                assertTrue(witness.contains("final " + prefix + ".Widget " + prefix + "LayoutResult0 = (print)("), witness);
            } else if (expected.startsWith("AnimatedSwitcher")) {
                boolean layout=expected.equals("AnimatedSwitcherLayoutBuilder");
                assertTrue(witness.contains(prefix+(layout?".Widget? ":".Widget ")+prefix+"SwitcherChild0() => throw 0;"),witness);
                assertTrue(witness.contains((layout?prefix+"Core.List<"+prefix+".Widget> ":prefix+".Animation<"+prefix+"Core.double> ")+prefix+"SwitcherArgument0() => throw 0;"),witness);
                assertTrue(witness.contains("final "+prefix+".Widget "+prefix+"SwitcherResult0 = (print)("+prefix+"SwitcherChild0(), "+prefix+"SwitcherArgument0());"),witness);
            } else if (expected.equals("AnimatedCrossFadeBuilder")) {
                assertTrue(witness.contains(prefix + ".Widget " + prefix + "CrossFadeChild0() => throw 0;"), witness);
                assertTrue(witness.contains(prefix + ".Key " + prefix + "CrossFadeKey0() => throw 0;"), witness);
                assertTrue(witness.contains("final " + prefix + ".Widget " + prefix + "CrossFadeResult0 = (print)("
                        + prefix + "CrossFadeChild0(), " + prefix + "CrossFadeKey0(), " + prefix + "CrossFadeChild0(), " + prefix + "CrossFadeKey0());"), witness);
            } else if (expected.equals("TransitionBuilder")) {
                assertTrue(witness.contains(prefix + ".BuildContext " + prefix + "TransitionContext0() => throw 0;"), witness);
                assertTrue(witness.contains(prefix + ".Widget? " + prefix + "TransitionChild0() => throw 0;"), witness);
                assertTrue(witness.contains("final " + prefix + ".Widget " + prefix + "TransitionResult0 = (print)("), witness);
                assertFalse(witness.contains("import 'package:flutter/rendering.dart'"), witness);
            } else if (expected.equals("ChildIndexGetter?")) {
                assertTrue(witness.contains(prefix + ".Key " + prefix + "SliverKey0() => throw 0;"), witness);
                assertTrue(witness.contains("final " + prefix + "Core.int? " + prefix + "SliverKeyResult0 = (print)?.call("), witness);
            } else {
                assertTrue(witness.contains(prefix + "Core.int " + prefix + "SliverIndex0() => throw 0;"), witness);
                assertTrue(witness.contains("final " + prefix + ".Widget" + (expected.startsWith("Nullable") ? "?" : "")
                        + " " + prefix + "SliverResult0 = (print)("), witness);
            }
            assertEquals("void build() {}\n", Files.readString(accepted.dartFile));
        }
    }

    @Test
    void valueListenableProofChecksOriginalValueAndCallbackResultWithSelectedT() throws Exception {
        for(String expected:List.of("ValueListenable<Object>","ValueWidgetBuilder<Object>","Tween<Object>")) {
            String candidate="void build() {\n  print('candidate');\n}\n";
            Fixture accepted=fixture(Mode.PASS,limits(Duration.ofSeconds(3),1024*1024));
            Files.writeString(accepted.dartFile,"void build() {}\n",StandardCharsets.UTF_8);
            int offset=candidate.indexOf("print");
            var type=new DartStaticTypeProbe(offset,5,0,candidate.indexOf("  print"),expected,
                    "package:flutter/widgets.dart",Optional.of("String"));
            var probe=new DartSymbolProbe("typed.value",offset,5,"print","dart:core",
                    Files.createDirectories(temporaryDirectory.resolve("sdk/lib")),Optional.of("FUNCTION"),Optional.of(type));
            var result=await(accepted.analyzer.analyze(request(accepted,candidate,41,DartCandidateWarningPolicy.ALLOW,List.of(probe))));
            assertEquals(DartCandidateAnalysisStatus.PASSED,result.status());
            String witness=witnessContent(accepted),prefix="_nbfdStaticTypeProof0";
            if(expected.startsWith("Tween")) {
                assertTrue(witness.contains(prefix+".Tween<String>"),witness);
                assertTrue(witness.contains("TweenValue0 = (print).lerp(0.5);"),witness);
                assertTrue(witness.contains("TweenValueCheck0 = (print).lerp(0.5)."),witness);
            } else if(expected.startsWith("ValueListenable")) {
                assertTrue(witness.contains("import 'package:flutter/foundation.dart' as "+prefix+"Foundation;"),witness);
                assertTrue(witness.contains(prefix+"Foundation.ValueListenable<String>"),witness);
                assertTrue(witness.contains("ListenableValue0 = (print).value;"),witness);
                assertTrue(witness.contains("ListenableValueCheck0 = (print).value."),witness);
            } else {
                assertTrue(witness.contains(prefix+".ValueWidgetBuilder<String>"),witness);
                assertTrue(witness.contains("String "+prefix+"ValueArgument0() => throw 0;"),witness);
                assertTrue(witness.contains(prefix+".Widget? "+prefix+"ValueChild0() => throw 0;"),witness);
                assertTrue(witness.contains(prefix+".Widget "+prefix+"ValueResult0 = (print)("),witness);
            }
            assertEquals("void build() {}\n",Files.readString(accepted.dartFile));
        }
    }

    @Test
    void objectReferenceProofUsesCoreAliasWithoutBroadeningUserImports() throws Exception {
        for (String header : List.of("", "import 'dart:core' as sourceCore;\n",
                "import 'dart:core' show String;\n", "import 'dart:core' hide Object;\n")) {
            String candidate = header + "void build() {\n  print('candidate');\n}\n";
            Fixture accepted = fixture(Mode.PASS, limits(Duration.ofSeconds(3), 1024 * 1024));
            String original = "void build() {}\n";
            Files.writeString(accepted.dartFile, original, StandardCharsets.UTF_8);
            Path sdkRoot = Files.createDirectories(temporaryDirectory.resolve("sdk/lib"));
            int offset = candidate.indexOf("print");
            var type = new DartStaticTypeProbe(offset, 5, 0, candidate.indexOf("  print"),
                    "Object", "package:flutter/material.dart");
            var probe = new DartSymbolProbe("typed.object", offset, 5, "print", "dart:core",
                    sdkRoot, Optional.of("FUNCTION"), Optional.of(type));
            var result = await(accepted.analyzer.analyze(request(
                    accepted, candidate, 41, DartCandidateWarningPolicy.ALLOW, List.of(probe))));
            assertEquals(DartCandidateAnalysisStatus.PASSED, result.status());
            String witness = accepted.factory.processes().stream().flatMap(process -> process.requests().stream())
                    .filter(value -> "analysis.updateContent".equals(value.path("method").asText()))
                    .map(value -> value.path("params").path("files").path(accepted.dartFile.toString()))
                    .filter(value -> "add".equals(value.path("type").asText()))
                    .map(value -> value.path("content").asText()).reduce((first, last) -> last).orElseThrow();
            assertTrue(witness.contains("final _nbfdStaticTypeProof0Core.int _nbfdStaticTypeProof0Control"), witness);
            assertFalse(witness.contains("_nbfdStaticTypeProof0.Object"), witness);
            assertTrue(witness.contains("import 'dart:core' as _nbfdStaticTypeProof0Core;"), witness);
            assertEquals(header.isEmpty(), witness.startsWith(
                    "import 'package:flutter/material.dart' as _nbfdStaticTypeProof0;\nimport 'dart:core';"), witness);
            assertTrue(witness.contains(header + "void build()"), witness);
            assertEquals(original, Files.readString(accepted.dartFile, StandardCharsets.UTF_8));
        }
    }

    @Test
    void radioProofUsesClosedSourceIdentityCollisionFreeExtensionAndRegistryConsumption() throws Exception {
        for (String expected : List.of("Type", "Object", "Object?", "ValueChanged<Object?>", "RadioGroupRegistry<Object>")) {
            String candidate = "import 'dart:core' as sourceCore;\n"
                    + "const spoof = '_nbfdStaticTypeProof0NonDynamic';\n"
                    + "void build() {\n  print('candidate');\n}\n";
            Fixture accepted = fixture(Mode.PASS, limits(Duration.ofSeconds(3), 1024 * 1024));
            String disk = "void build() {}\n";
            Files.writeString(accepted.dartFile, disk, StandardCharsets.UTF_8);
            Path sdkRoot = Files.createDirectories(temporaryDirectory.resolve("sdk/lib"));
            int offset = candidate.indexOf("print");
            var type = new DartStaticTypeProbe(offset, 5, 0, candidate.indexOf("  print"),
                    expected, "package:flutter/widgets.dart", Optional.of("project.Choice?"));
            var probe = new DartSymbolProbe("typed.radio", offset, 5, "print", "dart:core",
                    sdkRoot, Optional.of("FUNCTION"), Optional.of(type));
            var result = await(accepted.analyzer.analyze(request(accepted, candidate, 41, DartCandidateWarningPolicy.ALLOW, List.of(probe))));
            assertEquals(DartCandidateAnalysisStatus.PASSED, result.status());
            String witness = accepted.factory.processes().stream().flatMap(process -> process.requests().stream())
                    .filter(value -> "analysis.updateContent".equals(value.path("method").asText()))
                    .map(value -> value.path("params").path("files").path(accepted.dartFile.toString()))
                    .filter(value -> "add".equals(value.path("type").asText()))
                    .map(value -> value.path("content").asText()).reduce((first, last) -> last).orElseThrow();
            assertTrue(witness.contains("final _nbfdStaticTypeProof1Core.int _nbfdStaticTypeProof1Control"), witness);
            assertTrue(witness.contains("project.Choice? _nbfdStaticTypeProof1Selected0() => throw 0;"), witness);
            assertTrue(witness.endsWith("extension _nbfdStaticTypeProof1Extension on _nbfdStaticTypeProof1Core.Object? { _nbfdStaticTypeProof1Core.int get _nbfdStaticTypeProof1NonDynamic => 0; }\n"), witness);
            assertFalse(witness.contains("Choice??"), witness);
            assertFalse(witness.contains("import 'dart:core';"), witness);
            if (expected.equals("RadioGroupRegistry<Object>")) {
                assertTrue(witness.contains("void Function(_nbfdStaticTypeProof1.RadioClient<project.Choice?>) _nbfdStaticTypeProof1Consumer0 = (print).registerClient;"), witness);
            }
            if (expected.equals("Object") || expected.equals("Object?")) {
                assertTrue(witness.contains("ExpressionCheck0 = (print)._nbfdStaticTypeProof1NonDynamic;"), witness);
            }
            assertEquals(disk, Files.readString(accepted.dartFile, StandardCharsets.UTF_8));
        }
    }

    @Test
    void notificationProofChecksSdkBoundEvenWithoutCallbackAndUsesSelectedCallbackType() throws Exception {
        for (boolean callback : List.of(false, true)) {
            String candidate = "// ignore_for_file: invalid_assignment, type_argument_not_matching_bounds\n"
                    + "const spoof = '_nbfdStaticTypeProof0Notifications';\n"
                    + "class Notification {}\nvoid build() {\n  print('candidate');\n}\n";
            Fixture accepted = fixture(Mode.PASS, limits(Duration.ofSeconds(3), 1024 * 1024));
            Files.writeString(accepted.dartFile, "void build() {}\n", StandardCharsets.UTF_8);
            Path sdkRoot = Files.createDirectories(temporaryDirectory.resolve("sdk/lib"));
            int offset = candidate.indexOf("print");
            var type = new DartStaticTypeProbe(offset, 5, 0, candidate.indexOf("  print"),
                    callback ? "NotificationListenerCallback<Notification>" : "Type",
                    "package:flutter/widgets.dart", Optional.of("project.CustomNotification"),
                    callback ? Optional.empty() : Optional.of("Notification"));
            var probe = new DartSymbolProbe("typed.notification", offset, 5, "print", "dart:core",
                    sdkRoot, Optional.of("FUNCTION"), Optional.of(type));
            var result = await(accepted.analyzer.analyze(request(accepted, candidate, 41,
                    DartCandidateWarningPolicy.ALLOW, List.of(probe))));
            assertEquals(DartCandidateAnalysisStatus.PASSED, result.status(), "Scripted analyzer tests witness structure only");
            String witness = witnessContent(accepted);
            assertTrue(witness.contains("project.CustomNotification _nbfdStaticTypeProof1Selected0() => throw 0;"), witness);
            assertTrue(witness.contains("_nbfdStaticTypeProof1Selected0()._nbfdStaticTypeProof1NonDynamic"), witness);
            assertTrue(witness.contains("project.CustomNotification _nbfdStaticTypeProof1NonNullableControl0 = null;"), witness);
            if (callback) {
                assertTrue(witness.contains("final _nbfdStaticTypeProof1.NotificationListenerCallback<project.CustomNotification> _nbfdStaticTypeProof1Value0 = print;"), witness);
            } else {
                assertTrue(witness.contains("import 'package:flutter/widgets.dart' as _nbfdStaticTypeProof1Notifications;"), witness);
                assertTrue(witness.contains("final _nbfdStaticTypeProof1Notifications.Notification _nbfdStaticTypeProof1NotificationBound0 = _nbfdStaticTypeProof1Selected0();"), witness);
                assertTrue(witness.contains("final _nbfdStaticTypeProof1Core.Type _nbfdStaticTypeProof1Value0 = print;"), witness);
            }
            assertFalse(witness.contains("ignore_for_file"), witness);
            assertEquals("void build() {}\n", Files.readString(accepted.dartFile));
        }
    }

    @Test
    void requiresAnalyzerOnlyNonNullStaticTypeProofForTypedSymbols()
            throws Exception {
        String candidate = """
                const marker = '// ignore_for_file: invalid_assignment';
                // ignore_for_file: invalid_assignment
                void build() {
                  print('candidate');
                }
                """;
        int symbolOffset = candidate.indexOf("print");
        int statementInsertion = candidate.indexOf("  print");

        Fixture accepted = fixture(
                Mode.PASS, limits(Duration.ofSeconds(3), 1024 * 1024));
        Files.writeString(accepted.dartFile, "void build() {}\n",
                StandardCharsets.UTF_8);
        Path sdkRoot = Files.createDirectories(
                temporaryDirectory.resolve("sdk/lib"));
        DartStaticTypeProbe staticType = new DartStaticTypeProbe(
                symbolOffset,
                "print".length(),
                0,
                statementInsertion,
                "CustomClipper<RRect>",
                "package:flutter/widgets.dart");
        DartSymbolProbe typedProbe = new DartSymbolProbe(
                "typed.print",
                symbolOffset,
                "print".length(),
                "print",
                "dart:core",
                sdkRoot,
                Optional.of("FUNCTION"),
                Optional.of(staticType));

        DartCandidateAnalysisResult passed = await(accepted.analyzer.analyze(request(
                accepted,
                candidate,
                41,
                DartCandidateWarningPolicy.ALLOW,
                List.of(typedProbe))));

        assertEquals(DartCandidateAnalysisStatus.PASSED, passed.status());
        assertTrue(passed.symbolEvidence().getFirst()
                .staticTypeEvidence().orElseThrow().accepted());
        List<JsonNode> additions = accepted.factory.processes().stream()
                .flatMap(process -> process.requests().stream())
                .filter(value -> "analysis.updateContent".equals(
                        value.path("method").asText()))
                .map(value -> value.path("params").path("files")
                        .path(accepted.dartFile.toString()))
                .filter(value -> "add".equals(value.path("type").asText()))
                .toList();
        assertEquals(2, additions.size());
        String witness = additions.get(1).path("content").asText();
        assertTrue(witness.contains(
                "import 'package:flutter/widgets.dart' as _nbfdStaticTypeProof0;"),
                witness);
        assertTrue(witness.contains(
                "'// ignore_for_file: invalid_assignment'"), witness);
        assertEquals(witness.indexOf("ignore_for_file"),
                witness.lastIndexOf("ignore_for_file"), witness);
        assertTrue(witness.contains(
                "    _nbfdStaticTypeProof0Core.dynamic _nbfdStaticTypeProof0Dynamic() => null;"), witness);
        assertTrue(witness.contains(
                "final _nbfdStaticTypeProof0.CustomClipper<"
                + "_nbfdStaticTypeProof0.RRect> "
                + "_nbfdStaticTypeProof0Value0 = print;"), witness);
        assertEquals(42, additions.get(1).path("version").asLong());
        assertEquals(2, accepted.factory.processes().size());
        ScriptedProcess proofProcess = accepted.factory.processes().get(1);
        assertEquals(List.of(
                "analysis.updateContent",
                "analysis.setAnalysisRoots",
                "analysis.setPriorityFiles",
                "analysis.updateContent",
                "analysis.getErrors",
                "analysis.updateContent",
                "analysis.setPriorityFiles",
                "server.shutdown"), proofProcess.methods());
        JsonNode optionsOverlay = proofProcess.requests().getFirst()
                .path("params").path("files")
                .path(accepted.dartFile.getParent()
                        .resolve("analysis_options.yaml").toString());
        assertEquals("add", optionsOverlay.path("type").asText());
        assertTrue(optionsOverlay.path("content").asText()
                .contains("strict-casts: true"));
        assertEquals(List.of("add", "add", "remove", "remove"),
                proofProcess.overlayTypes());

        Fixture rejected = fixture(
                Mode.STATIC_TYPE_ERROR,
                limits(Duration.ofSeconds(3), 1024 * 1024));
        Files.writeString(rejected.dartFile, "void build() {}\n",
                StandardCharsets.UTF_8);
        DartSymbolProbe rejectedProbe = new DartSymbolProbe(
                "typed.print",
                symbolOffset,
                "print".length(),
                "print",
                "dart:core",
                sdkRoot,
                Optional.of("FUNCTION"),
                Optional.of(staticType));

        DartCandidateAnalysisResult failed = await(rejected.analyzer.analyze(request(
                rejected,
                candidate,
                42,
                DartCandidateWarningPolicy.ALLOW,
                List.of(rejectedProbe))));

        assertEquals(DartCandidateAnalysisStatus.REJECTED, failed.status());
        assertFalse(failed.symbolEvidence().getFirst().accepted());
        assertFalse(failed.symbolEvidence().getFirst()
                .staticTypeEvidence().orElseThrow().accepted());
        assertTrue(failed.symbolEvidence().getFirst().rejectionReason()
                .orElseThrow().contains("strict requested type CustomClipper<RRect>"));
    }

    @Test
    void qualifiesNullableGenericArgumentsWithoutMakingOuterTypesNullable() throws Exception {
        String candidate = "void build() {\n  print('candidate');\n}\n";
        int expressionOffset = candidate.indexOf("print");
        Path sdkRoot = Files.createDirectories(temporaryDirectory.resolve("sdk/lib"));
        for (String type : List.of("Animation<Color?>", "AnimationController", "WidgetStateProperty<Icon?>")) {
            Fixture fixture = fixture(Mode.PASS, limits(Duration.ofSeconds(3), 1024 * 1024));
            Files.writeString(fixture.dartFile, "void build() {}\n", StandardCharsets.UTF_8);
            DartSymbolProbe probe = new DartSymbolProbe("typed.print", expressionOffset,
                    "print".length(), "print", "dart:core", sdkRoot, Optional.of("FUNCTION"),
                    Optional.of(new DartStaticTypeProbe(expressionOffset, "print".length(),
                            0, candidate.indexOf("  print"), type, "package:flutter/widgets.dart")));
            DartCandidateAnalysisResult result = await(fixture.analyzer.analyze(request(
                    fixture, candidate, 43, DartCandidateWarningPolicy.ALLOW, List.of(probe))));
            assertEquals(DartCandidateAnalysisStatus.PASSED, result.status());
            assertTrue(result.symbolEvidence().getFirst().staticTypeEvidence().orElseThrow().accepted());
            String witness = fixture.factory.processes().get(1).requests().stream()
                    .filter(value -> "analysis.updateContent".equals(value.path("method").asText()))
                    .map(value -> value.path("params").path("files").path(fixture.dartFile.toString()))
                    .filter(value -> "add".equals(value.path("type").asText()))
                    .findFirst().orElseThrow().path("content").asText();
            String expected = switch (type) {
                case "Animation<Color?>" -> "_nbfdStaticTypeProof0.Animation<_nbfdStaticTypeProof0.Color?>";
                case "WidgetStateProperty<Icon?>" -> "_nbfdStaticTypeProof0.WidgetStateProperty<_nbfdStaticTypeProof0.Icon?>";
                default -> "_nbfdStaticTypeProof0.AnimationController";
            };
            assertTrue(witness.contains("final " + expected + " _nbfdStaticTypeProof0Value0 = print;"), witness);
            assertTrue(fixture.factory.processes().get(1)
                    .requests().getFirst().toString().contains("strict-casts: true"));
            assertEquals("void build() {}\n", Files.readString(fixture.dartFile, StandardCharsets.UTF_8));
        }
    }

    @Test
    void rejectsTypedEvidenceWhenEitherProofDiagnosticIsSuppressedOrDemoted()
            throws Exception {
        String candidate = "void build() {\n  print('candidate');\n}\n";
        int symbolOffset = candidate.indexOf("print");
        int statementInsertion = candidate.indexOf("  print");
        Path sdkRoot = Files.createDirectories(
                temporaryDirectory.resolve("sdk/lib"));

        for (Mode mode : List.of(
                Mode.SUPPRESSED_PROOF_CONTROL,
                Mode.DEMOTED_PROOF_CONTROL)) {
            Fixture fixture = fixture(
                    mode, limits(Duration.ofSeconds(3), 1024 * 1024));
            Files.writeString(fixture.dartFile, "void build() {}\n",
                    StandardCharsets.UTF_8);
            DartStaticTypeProbe staticType = new DartStaticTypeProbe(
                    symbolOffset,
                    "print".length(),
                    0,
                    statementInsertion,
                    "CustomClipper<RRect>",
                    "package:flutter/widgets.dart");
            DartSymbolProbe probe = new DartSymbolProbe(
                    "typed.print",
                    symbolOffset,
                    "print".length(),
                    "print",
                    "dart:core",
                    sdkRoot,
                    Optional.of("FUNCTION"),
                    Optional.of(staticType));

            DartCandidateAnalysisResult result = await(fixture.analyzer.analyze(
                    request(fixture, candidate, 51 + mode.ordinal(),
                            DartCandidateWarningPolicy.ALLOW, List.of(probe))));

            assertEquals(DartCandidateAnalysisStatus.REJECTED, result.status(),
                    () -> mode + ": " + result);
            DartStaticTypeEvidence evidence = result.symbolEvidence().getFirst()
                    .staticTypeEvidence().orElseThrow();
            assertFalse(evidence.accepted());
            assertTrue(evidence.rejectionReason().orElseThrow()
                    .contains("suppressed or demoted"));
        }
    }

    @Test
    void analyzerErrorRejectsAndStillRemovesOverlayAndPriority() throws Exception {
        Fixture fixture = fixture(Mode.ERROR, limits(Duration.ofSeconds(3), 1024 * 1024));
        String candidate = "void main() { final value = ; }\n";
        Files.writeString(fixture.dartFile, "void main() {}\n", StandardCharsets.UTF_8);

        DartCandidateAnalysisResult result = await(fixture.analyzer.analyze(request(
                fixture, candidate, 7, DartCandidateWarningPolicy.ALLOW, List.of())));

        assertEquals(DartCandidateAnalysisStatus.REJECTED, result.status());
        assertEquals(1, result.diagnostics().size());
        assertEquals(DartCandidateDiagnosticSeverity.ERROR,
                result.diagnostics().getFirst().severity());
        assertTrue(result.diagnostics().getFirst().blocking());
        assertEquals(List.of("add", "remove"), fixture.process.overlayTypes());
        assertEquals(List.of(1, 0), fixture.process.priorityCounts());
    }

    @Test
    void warningPolicyIsExplicitlyRepresented() throws Exception {
        Fixture allowed = fixture(Mode.WARNING, limits(Duration.ofSeconds(3), 1024 * 1024));
        Files.writeString(allowed.dartFile, "void main() {}\n", StandardCharsets.UTF_8);
        String candidate = "void main() { final value = 1; }\n";

        DartCandidateAnalysisResult pass = await(allowed.analyzer.analyze(request(
                allowed, candidate, 1, DartCandidateWarningPolicy.ALLOW, List.of())));

        assertEquals(DartCandidateAnalysisStatus.PASSED, pass.status());
        assertFalse(pass.diagnostics().getFirst().blocking());

        Fixture rejected = fixture(Mode.WARNING, limits(Duration.ofSeconds(3), 1024 * 1024));
        Files.writeString(rejected.dartFile, "void main() {}\n", StandardCharsets.UTF_8);
        DartCandidateAnalysisResult fail = await(rejected.analyzer.analyze(request(
                rejected, candidate, 2, DartCandidateWarningPolicy.REJECT, List.of())));

        assertEquals(DartCandidateAnalysisStatus.REJECTED, fail.status());
        assertTrue(fail.diagnostics().getFirst().blocking());
    }

    @Test
    void contentModifiedIsStaleAndNeverPasses() throws Exception {
        Fixture fixture = fixture(
                Mode.CONTENT_MODIFIED,
                limits(Duration.ofSeconds(3), 1024 * 1024));
        String candidate = "void main() {}\n";
        Files.writeString(fixture.dartFile, candidate, StandardCharsets.UTF_8);

        DartCandidateAnalysisResult result = await(fixture.analyzer.analyze(request(
                fixture, candidate, 3, DartCandidateWarningPolicy.ALLOW, List.of())));

        assertEquals(DartCandidateAnalysisStatus.STALE, result.status());
        assertEquals(DartCandidateAnalysisIssueCode.CONTENT_MODIFIED,
                result.issue().orElseThrow().code());
        assertFalse(result.passed());
    }

    @Test
    void unsupportedProtocolAndMalformedOrOversizedResponseFailClosed() throws Exception {
        String candidate = "void main() {}\n";

        Fixture old = fixture(Mode.OLD_PROTOCOL, limits(Duration.ofSeconds(3), 1024 * 1024));
        Files.writeString(old.dartFile, candidate, StandardCharsets.UTF_8);
        DartCandidateAnalysisResult oldResult = await(old.analyzer.analyze(request(
                old, candidate, 1, DartCandidateWarningPolicy.ALLOW, List.of())));
        assertEquals(DartCandidateAnalysisIssueCode.PROTOCOL_UNSUPPORTED,
                oldResult.issue().orElseThrow().code());

        Fixture malformed = fixture(Mode.MALFORMED, limits(Duration.ofSeconds(3), 1024 * 1024));
        Files.writeString(malformed.dartFile, candidate, StandardCharsets.UTF_8);
        DartCandidateAnalysisResult malformedResult = await(malformed.analyzer.analyze(request(
                malformed, candidate, 1, DartCandidateWarningPolicy.ALLOW, List.of())));
        assertEquals(DartCandidateAnalysisStatus.UNAVAILABLE, malformedResult.status());
        assertEquals(DartCandidateAnalysisIssueCode.MALFORMED_RESPONSE,
                malformedResult.issue().orElseThrow().code());

        Fixture oversized = fixture(Mode.OVERSIZED, limits(Duration.ofSeconds(3), 512));
        Files.writeString(oversized.dartFile, candidate, StandardCharsets.UTF_8);
        DartCandidateAnalysisResult oversizedResult = await(oversized.analyzer.analyze(request(
                oversized, candidate, 1, DartCandidateWarningPolicy.ALLOW, List.of())));
        assertEquals(DartCandidateAnalysisStatus.UNAVAILABLE, oversizedResult.status());
        assertEquals(DartCandidateAnalysisIssueCode.RESPONSE_LIMIT,
                oversizedResult.issue().orElseThrow().code());
    }

    @Test
    void timeoutAndCancellationTerminateHungProcessWithFailClosedEvidence() throws Exception {
        String candidate = "void main() {}\n";
        Fixture timedOut = fixture(Mode.HANG, limits(Duration.ofMillis(120), 1024 * 1024));
        Files.writeString(timedOut.dartFile, candidate, StandardCharsets.UTF_8);

        DartCandidateAnalysisResult timeoutResult = await(timedOut.analyzer.analyze(request(
                timedOut, candidate, 1, DartCandidateWarningPolicy.ALLOW, List.of())));

        assertEquals(DartCandidateAnalysisStatus.TIMEOUT, timeoutResult.status());
        assertEquals(DartCandidateAnalysisIssueCode.TIMEOUT,
                timeoutResult.issue().orElseThrow().code());
        assertTrue(awaitDestroyed(timedOut.process));

        Fixture cancelled = fixture(Mode.HANG, limits(Duration.ofSeconds(3), 1024 * 1024));
        Files.writeString(cancelled.dartFile, candidate, StandardCharsets.UTF_8);
        DartCandidateAnalysisOperation operation = cancelled.analyzer.analyze(request(
                cancelled, candidate, 2, DartCandidateWarningPolicy.ALLOW, List.of()));
        assertTrue(cancelled.process.getErrorsSeen.await(2, TimeUnit.SECONDS));

        assertTrue(operation.cancel());
        DartCandidateAnalysisResult cancelledResult = await(operation);

        assertEquals(DartCandidateAnalysisStatus.STALE, cancelledResult.status());
        assertEquals(DartCandidateAnalysisIssueCode.CANCELLED,
                cancelledResult.issue().orElseThrow().code());
        assertTrue(awaitDestroyed(cancelled.process));
    }

    @Test
    void processExitAndWrongSymbolTargetNeverPass() throws Exception {
        String candidate = "void main() { print('x'); }\n";
        Fixture exited = fixture(Mode.PROCESS_EXIT, limits(Duration.ofSeconds(3), 1024 * 1024));
        Files.writeString(exited.dartFile, candidate, StandardCharsets.UTF_8);

        DartCandidateAnalysisResult exitResult = await(exited.analyzer.analyze(request(
                exited, candidate, 1, DartCandidateWarningPolicy.ALLOW, List.of())));

        assertEquals(DartCandidateAnalysisStatus.UNAVAILABLE, exitResult.status());
        assertEquals(DartCandidateAnalysisIssueCode.PROCESS_FAILED,
                exitResult.issue().orElseThrow().code());

        Fixture wrong = fixture(Mode.WRONG_TARGET, limits(Duration.ofSeconds(3), 1024 * 1024));
        Files.writeString(wrong.dartFile, candidate, StandardCharsets.UTF_8);
        Path expected = Files.createDirectories(temporaryDirectory.resolve("expected-sdk"));
        int offset = candidate.indexOf("print");
        DartSymbolProbe probe = new DartSymbolProbe(
                "print", offset, 5, "print", "dart:core", expected,
                Optional.of("FUNCTION"));
        DartCandidateAnalysisResult wrongResult = await(wrong.analyzer.analyze(request(
                wrong, candidate, 2, DartCandidateWarningPolicy.ALLOW, List.of(probe))));

        assertEquals(DartCandidateAnalysisStatus.REJECTED, wrongResult.status());
        assertFalse(wrongResult.symbolEvidence().getFirst().accepted());
        assertTrue(wrongResult.symbolEvidence().getFirst().rejectionReason()
                .orElseThrow().contains("outside expected library root"));
    }

    @Test
    void requestRejectsMismatchedShaAndOutOfBoundsProbe() throws Exception {
        Path root = Files.createDirectories(temporaryDirectory.resolve("validation"));
        Path file = root.resolve("main.dart");
        String content = "void main() {}\n";
        Files.writeString(file, content, StandardCharsets.UTF_8);

        assertThrows(IllegalArgumentException.class, () -> new DartCandidateAnalysisRequest(
                root, file, content, 1, "0".repeat(64),
                DartCandidateWarningPolicy.ALLOW, List.of()));
        DartSymbolProbe invalid = new DartSymbolProbe(
                "bad", 500, 1, "x", "dart:core", root, Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> new DartCandidateAnalysisRequest(
                root, file, content, 1, sha(content),
                DartCandidateWarningPolicy.ALLOW, List.of(invalid)));
    }

    @Test
    void sharedCapacityAcceptsExactBoundsAndRejectsBytesOrProbesBeforeProcess()
            throws Exception {
        String exact = "void main() {}\n";
        int exactBytes = exact.getBytes(StandardCharsets.UTF_8).length;
        DartCandidateCapacityBudget budget = new DartCandidateCapacityBudget(
                "analysis-boundary-test", exactBytes, 1, 0);
        DartCandidateAnalysisLimits limits = limits(
                Duration.ofSeconds(3), 1024 * 1024, budget);

        Fixture accepted = fixture(Mode.ERROR, limits);
        Files.writeString(accepted.dartFile, exact, StandardCharsets.UTF_8);
        DartSymbolProbe exactProbe = new DartSymbolProbe(
                "p0", 0, 1, "v", "dart:core",
                accepted.root, Optional.empty());
        DartCandidateAnalysisResult exactResult = await(
                accepted.analyzer.analyze(request(
                        accepted,
                        exact,
                        1,
                        DartCandidateWarningPolicy.ALLOW,
                        List.of(exactProbe))));
        assertFalse(exactResult.issue().map(DartCandidateAnalysisIssue::code)
                .filter(code -> code == DartCandidateAnalysisIssueCode.CANDIDATE_TOO_LARGE
                || code == DartCandidateAnalysisIssueCode.PROBE_LIMIT
                || code == DartCandidateAnalysisIssueCode.CAPACITY_POLICY_MISMATCH)
                .isPresent());
        assertTrue(accepted.factory.command != null,
                "the exact capacity boundary must reach the analyzer process");
        assertEquals(1, exactResult.requestedSymbolProbes());

        Fixture tooLarge = fixture(Mode.PASS, limits);
        String oversized = exact + "é";
        Files.writeString(tooLarge.dartFile, exact, StandardCharsets.UTF_8);
        DartCandidateAnalysisResult byteResult = await(
                tooLarge.analyzer.analyze(request(
                        tooLarge,
                        oversized,
                        2,
                        DartCandidateWarningPolicy.ALLOW,
                        List.of())));
        assertEquals(DartCandidateAnalysisIssueCode.CANDIDATE_TOO_LARGE,
                byteResult.issue().orElseThrow().code());
        assertTrue(tooLarge.factory.command == null,
                "an over-capacity candidate must not start a process");

        Fixture tooManyProbes = fixture(Mode.PASS, limits);
        Files.writeString(tooManyProbes.dartFile, exact, StandardCharsets.UTF_8);
        List<DartSymbolProbe> probes = List.of(
                new DartSymbolProbe(
                        "p0", 0, 1, "v", "dart:core",
                        tooManyProbes.root, Optional.empty()),
                new DartSymbolProbe(
                        "p1", 1, 1, "o", "dart:core",
                        tooManyProbes.root, Optional.empty()));
        DartCandidateAnalysisResult probeResult = await(
                tooManyProbes.analyzer.analyze(request(
                        tooManyProbes,
                        exact,
                        3,
                        DartCandidateWarningPolicy.ALLOW,
                        probes)));
        assertEquals(DartCandidateAnalysisIssueCode.PROBE_LIMIT,
                probeResult.issue().orElseThrow().code());
        assertTrue(tooManyProbes.factory.command == null,
                "an over-capacity probe request must not start a process");
    }

    @Test
    void valueEqualButForeignCapacityIdentityFailsBeforeProcess()
            throws Exception {
        String content = "void main() {}\n";
        int bytes = content.getBytes(StandardCharsets.UTF_8).length;
        DartCandidateCapacityBudget analyzerBudget =
                new DartCandidateCapacityBudget("identity-test", bytes, 1, 0);
        DartCandidateCapacityBudget foreignBudget =
                new DartCandidateCapacityBudget("identity-test", bytes, 1, 0);
        Fixture fixture = fixture(
                Mode.PASS,
                limits(Duration.ofSeconds(3), 1024 * 1024, analyzerBudget));
        Files.writeString(fixture.dartFile, content, StandardCharsets.UTF_8);
        DartCandidateAnalysisRequest request = new DartCandidateAnalysisRequest(
                fixture.root,
                fixture.dartFile,
                content,
                7,
                sha(content),
                DartCandidateWarningPolicy.ALLOW,
                List.of(),
                foreignBudget);

        DartCandidateAnalysisResult result = await(
                fixture.analyzer.analyze(request));

        assertEquals(DartCandidateAnalysisIssueCode.CAPACITY_POLICY_MISMATCH,
                result.issue().orElseThrow().code());
        assertTrue(fixture.factory.command == null);
    }

    private Fixture fixture(Mode mode, DartCandidateAnalysisLimits limits) throws IOException {
        Path root = Files.createDirectories(temporaryDirectory.resolve(
                "project-" + mode + '-' + System.nanoTime()));
        Path file = root.resolve("lib/main.dart");
        Files.createDirectories(file.getParent());
        Path targetRoot = Files.createDirectories(temporaryDirectory.resolve("sdk/lib"));
        Path resolvedTarget = targetRoot.resolve("core/print.dart");
        Files.createDirectories(resolvedTarget.getParent());
        Files.writeString(resolvedTarget, "void print(Object? value) {}\n",
                StandardCharsets.UTF_8);
        Path wrongTarget = targetRoot.getParent().resolve("wrong/print.dart");
        Files.createDirectories(wrongTarget.getParent());
        Files.writeString(wrongTarget, "void print(Object? value) {}\n",
                StandardCharsets.UTF_8);
        ScriptedProcess process = new ScriptedProcess(mode, targetRoot);
        RecordingFactory factory = new RecordingFactory(process);
        Path executable = temporaryDirectory.resolve("dart-sdk/bin/dart.exe").toAbsolutePath();
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(
                executable, ignored -> { }, limits, factory);
        return new Fixture(
                analyzer, process, factory, root, file, executable, limits);
    }

    private static DartCandidateAnalysisLimits limits(Duration timeout, int maxJsonLine) {
        return limits(
                timeout,
                maxJsonLine,
                new DartCandidateCapacityBudget(
                        "analysis-test-default", 2 * 1024 * 1024, 16, 0));
    }

    private static DartCandidateAnalysisLimits limits(
            Duration timeout,
            int maxJsonLine,
            DartCandidateCapacityBudget budget) {
        return new DartCandidateAnalysisLimits(
                budget,
                maxJsonLine,
                32,
                4_096,
                timeout,
                Duration.ofMillis(20));
    }

    private static DartCandidateAnalysisRequest request(
            Fixture fixture,
            String content,
            long version,
            DartCandidateWarningPolicy warningPolicy,
            List<DartSymbolProbe> probes) {
        return new DartCandidateAnalysisRequest(
                fixture.root,
                fixture.dartFile,
                content,
                version,
                sha(content),
                warningPolicy,
                probes,
                fixture.limits.candidateCapacityBudget());
    }

    private static String sha(String content) {
        return DartCandidateHashes.sha256(DartCandidateHashes.strictUtf8(content));
    }

    private static DartCandidateAnalysisResult await(DartCandidateAnalysisOperation operation)
            throws Exception {
        return operation.result().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }

    private static boolean awaitDestroyed(ScriptedProcess process) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline && process.isAlive()) {
            Thread.sleep(5);
        }
        return !process.isAlive();
    }

    private static void assertExpectedCommand(Fixture fixture) {
        assertEquals(List.of(
                fixture.executable.toString(),
                "language-server",
                "--protocol=analyzer",
                "--client-id=netbeans-flutter-designer",
                "--client-version=" + DartAnalysisServer.CLIENT_VERSION),
                fixture.factory.command);
        assertEquals(fixture.root, fixture.factory.workingDirectory);
    }

    private record Fixture(
            DartCandidateAnalyzer analyzer,
            ScriptedProcess process,
            RecordingFactory factory,
            Path root,
            Path dartFile,
            Path executable,
            DartCandidateAnalysisLimits limits) {
    }

    private enum Mode {
        PASS,
        ERROR,
        WARNING,
        CONTENT_MODIFIED,
        OLD_PROTOCOL,
        MALFORMED,
        OVERSIZED,
        HANG,
        PROCESS_EXIT,
        WRONG_TARGET,
        STATIC_TYPE_ERROR,
        SUPPRESSED_PROOF_CONTROL,
        DEMOTED_PROOF_CONTROL
    }

    private static final class RecordingFactory
            implements DartAnalyzerProtocolSession.ProcessFactory {
        private final ScriptedProcess process;
        private final List<ScriptedProcess> processes = new ArrayList<>();
        private List<String> command;
        private Path workingDirectory;

        RecordingFactory(ScriptedProcess process) {
            this.process = process;
        }

        @Override
        public synchronized Process start(
                List<String> command,
                Path workingDirectory) throws IOException {
            this.command = List.copyOf(command);
            this.workingDirectory = workingDirectory;
            ScriptedProcess started = processes.isEmpty()
                    ? process
                    : new ScriptedProcess(process.mode, process.targetRoot);
            processes.add(started);
            started.startServer();
            return started;
        }

        synchronized List<ScriptedProcess> processes() {
            return List.copyOf(processes);
        }
    }

    private static final class ScriptedProcess extends Process {
        private final Mode mode;
        private final Path targetRoot;
        private final PipedOutputStream clientInput = new PipedOutputStream();
        private final PipedInputStream serverInput;
        private final PipedInputStream clientOutput = new PipedInputStream(64 * 1024);
        private final PipedOutputStream serverOutput;
        private final List<JsonNode> requests = java.util.Collections.synchronizedList(
                new ArrayList<>());
        private final AtomicReference<Throwable> serverFailure = new AtomicReference<>();
        private final AtomicBoolean alive = new AtomicBoolean(true);
        private final AtomicInteger destroyCalls = new AtomicInteger();
        private final CountDownLatch getErrorsSeen = new CountDownLatch(1);
        private final AtomicInteger getErrorsCalls = new AtomicInteger();
        private Thread serverThread;

        ScriptedProcess(Mode mode, Path targetRoot) throws IOException {
            this.mode = mode;
            this.targetRoot = targetRoot;
            serverInput = new PipedInputStream(clientInput, 64 * 1024);
            serverOutput = new PipedOutputStream(clientOutput);
        }

        void startServer() {
            serverThread = Thread.ofVirtual().start(this::runServer);
        }

        List<JsonNode> requests() {
            synchronized (requests) {
                return requests.stream()
                        .map(value -> (JsonNode) value.deepCopy())
                        .toList();
            }
        }

        List<String> methods() {
            return requests().stream().map(value -> value.path("method").asText()).toList();
        }

        List<String> overlayTypes() {
            List<String> types = new ArrayList<>();
            for (JsonNode request : requests()) {
                if (!"analysis.updateContent".equals(request.path("method").asText())) {
                    continue;
                }
                request.path("params").path("files").properties()
                        .forEach(entry -> types.add(entry.getValue().path("type").asText()));
            }
            return types;
        }

        List<Integer> priorityCounts() {
            return requests().stream()
                    .filter(value -> "analysis.setPriorityFiles".equals(
                            value.path("method").asText()))
                    .map(value -> value.path("params").path("files").size())
                    .toList();
        }

        private void runServer() {
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(
                    serverInput, StandardCharsets.UTF_8))) {
                send(JSON.createObjectNode()
                        .put("event", "server.connected")
                        .set("params", JSON.createObjectNode().put(
                                "version", mode == Mode.OLD_PROTOCOL ? "1.39.9" : "1.40.1")));
                String line;
                while (alive.get() && (line = reader.readLine()) != null) {
                    JsonNode request = JSON.readTree(line);
                    requests.add(request.deepCopy());
                    String method = request.path("method").asText();
                    if ("analysis.getErrors".equals(method)) {
                        getErrorsCalls.incrementAndGet();
                        getErrorsSeen.countDown();
                        if (mode == Mode.HANG) {
                            continue;
                        }
                        if (mode == Mode.MALFORMED) {
                            sendRaw("{not-json\n");
                            continue;
                        }
                        if (mode == Mode.OVERSIZED) {
                            sendRaw("x".repeat(2_048) + "\n");
                            continue;
                        }
                        if (mode == Mode.PROCESS_EXIT) {
                            alive.set(false);
                            serverOutput.close();
                            return;
                        }
                        if (mode == Mode.CONTENT_MODIFIED) {
                            error(request, "CONTENT_MODIFIED", "Overlay changed.");
                            continue;
                        }
                        errors(request);
                        continue;
                    }
                    if ("analysis.getNavigation".equals(method)) {
                        navigation(request);
                        continue;
                    }
                    acknowledge(request);
                    if ("server.shutdown".equals(method)) {
                        alive.set(false);
                        serverOutput.close();
                        return;
                    }
                }
            } catch (Throwable ex) {
                if (alive.get()) {
                    serverFailure.set(ex);
                }
            } finally {
                alive.set(false);
            }
        }

        private void errors(JsonNode request) throws IOException {
            ArrayNode errors = JSON.createArrayNode();
            String file = request.path("params").path("file").asText();
            String content = latestOverlayContent(file);
            boolean proofOverlay = content.contains("Dynamic() => null;") && content.contains("Control =");
            if (proofOverlay) {
                int assignabilityControl = content.indexOf(
                        "Control =");
                if (mode != Mode.SUPPRESSED_PROOF_CONTROL) {
                    addDiagnostic(errors, file, assignabilityControl,
                            mode == Mode.DEMOTED_PROOF_CONTROL
                                    ? "WARNING" : "ERROR",
                            "invalid_assignment",
                            "A value of type 'dynamic' can't be assigned to the variable type.");
                    var nullableControls = java.util.regex.Pattern.compile("NonNullableControl[0-9]+ = null;").matcher(content);
                    while (nullableControls.find()) {
                        addDiagnostic(errors, file, nullableControls.start(), "ERROR", "invalid_assignment",
                                "Null cannot be assigned to the selected non-nullable type.");
                    }
                }
                if (mode == Mode.STATIC_TYPE_ERROR) {
                    addDiagnostic(errors, file, content.indexOf("Value0 ="),
                            "ERROR", "invalid_assignment",
                            "A value has the wrong static type.");
                }
            } else if (mode == Mode.ERROR || mode == Mode.WARNING) {
                boolean error = mode == Mode.ERROR;
                addDiagnostic(errors, file, error ? 30 : 20,
                        error ? "ERROR" : "WARNING",
                        error ? "missing_identifier" : "unused_local_variable",
                        error ? "Expected an identifier." : "Unused local variable.");
            }
            ObjectNode result = JSON.createObjectNode().set("errors", errors);
            respond(request, result);
        }

        private static void addDiagnostic(
                ArrayNode errors,
                String file,
                int offset,
                String severity,
                String code,
                String message) {
            boolean error = "ERROR".equals(severity);
            ObjectNode location = JSON.createObjectNode()
                    .put("file", file)
                    .put("offset", offset)
                    .put("length", 1)
                    .put("startLine", 1)
                    .put("startColumn", offset + 1)
                    .put("endLine", 1)
                    .put("endColumn", offset + 2);
            errors.add(JSON.createObjectNode()
                    .put("severity", severity)
                    .put("type", error ? "COMPILE_TIME_ERROR" : "STATIC_WARNING")
                    .put("message", message)
                    .put("code", code)
                    .set("location", location));
        }

        private void navigation(JsonNode request) throws IOException {
            int requestedOffset = request.path("params").path("offset").asInt();
            int requestedLength = request.path("params").path("length").asInt();
            Path target = mode == Mode.WRONG_TARGET
                    ? targetRoot.getParent().resolve("wrong/print.dart")
                    : targetRoot.resolve("core/print.dart");
            ArrayNode files = JSON.createArrayNode().add(target.toString());
            ArrayNode targets = JSON.createArrayNode().add(JSON.createObjectNode()
                    .put("kind", "FUNCTION")
                    .put("fileIndex", 0)
                    .put("offset", 100)
                    .put("length", 5)
                    .put("startLine", 10)
                    .put("startColumn", 3));
            ArrayNode regions = JSON.createArrayNode().add(JSON.createObjectNode()
                    .put("offset", requestedOffset)
                    .put("length", requestedLength)
                    .set("targets", JSON.createArrayNode().add(0)));
            ObjectNode result = JSON.createObjectNode();
            result.set("files", files);
            result.set("targets", targets);
            result.set("regions", regions);
            respond(request, result);
        }

        private String latestDartFile() {
            return requests().stream()
                    .filter(value -> "analysis.updateContent".equals(
                            value.path("method").asText()))
                    .findFirst().orElseThrow()
                    .path("params").path("files").propertyStream()
                    .findFirst().orElseThrow().getKey();
        }

        private String latestOverlayContent(String file) {
            List<JsonNode> snapshot = requests();
            for (int index = snapshot.size() - 1; index >= 0; index--) {
                JsonNode request = snapshot.get(index);
                if (!"analysis.updateContent".equals(
                        request.path("method").asText())) {
                    continue;
                }
                JsonNode overlay = request.path("params").path("files")
                        .path(file);
                if (overlay.has("content")) {
                    return overlay.path("content").asText();
                }
            }
            throw new IllegalStateException("no content overlay");
        }

        private void acknowledge(JsonNode request) throws IOException {
            send(JSON.createObjectNode().set("id", request.get("id")));
        }

        private void respond(JsonNode request, JsonNode result) throws IOException {
            ObjectNode response = JSON.createObjectNode();
            response.set("id", request.get("id"));
            response.set("result", result);
            send(response);
        }

        private void error(JsonNode request, String code, String message) throws IOException {
            ObjectNode response = JSON.createObjectNode();
            response.set("id", request.get("id"));
            response.set("error", JSON.createObjectNode()
                    .put("code", code)
                    .put("message", message));
            send(response);
        }

        private synchronized void send(JsonNode value) throws IOException {
            sendRaw(JSON.writeValueAsString(value) + "\n");
        }

        private synchronized void sendRaw(String value) throws IOException {
            serverOutput.write(value.getBytes(StandardCharsets.UTF_8));
            serverOutput.flush();
        }

        @Override
        public OutputStream getOutputStream() {
            return clientInput;
        }

        @Override
        public InputStream getInputStream() {
            return clientOutput;
        }

        @Override
        public InputStream getErrorStream() {
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public int waitFor() throws InterruptedException {
            if (serverThread != null) {
                serverThread.join();
            }
            return 0;
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
            if (serverThread == null) {
                return !alive.get();
            }
            serverThread.join(unit.toMillis(timeout));
            return !alive.get();
        }

        @Override
        public int exitValue() {
            if (alive.get()) {
                throw new IllegalThreadStateException("still running");
            }
            return 0;
        }

        @Override
        public void destroy() {
            destroyCalls.incrementAndGet();
            destroyForcibly();
        }

        @Override
        public Process destroyForcibly() {
            alive.set(false);
            try {
                clientInput.close();
                clientOutput.close();
                serverInput.close();
                serverOutput.close();
            } catch (IOException ignored) {
            }
            return this;
        }

        @Override
        public boolean isAlive() {
            return alive.get();
        }
    }
}
