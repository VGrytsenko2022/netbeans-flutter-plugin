package dev.flutter.netbeans.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

class PluginPackageMetadataIT {
    private static final String MODULE_JAR =
            "netbeans/modules/dev-flutter-netbeans-netbeans-plugin.jar";
    private static final String DESIGNER_LIBRARY =
            "netbeans/modules/ext/dev.flutter.netbeans.netbeans-plugin/"
            + "dev-flutter-netbeans/flutter-designer.jar";
    private static final String ANALYSIS_LIBRARY =
            "netbeans/modules/ext/dev.flutter.netbeans.netbeans-plugin/"
            + "dev-flutter-netbeans/dart-analysis.jar";
    private static final String CORE_LIBRARY =
            "netbeans/modules/ext/dev.flutter.netbeans.netbeans-plugin/"
            + "dev-flutter-netbeans/flutter-core-api.jar";
    private static final String CANVAS_RUNNER_LIBRARY =
            "netbeans/modules/ext/dev.flutter.netbeans.netbeans-plugin/"
            + "dev-flutter-netbeans/flutter-canvas-runner.jar";
    private static final String WEBVIEW2_NATIVE_RESOURCE_ROOT =
            "dev/flutter/netbeans/plugin/designer/canvas/webview2/win-x64/";
    private static final Map<String, PackagedFile> WEBVIEW2_NATIVE_FILES = Map.of(
            WEBVIEW2_NATIVE_RESOURCE_ROOT + "native.manifest",
            new PackagedFile(634,
                    "9a61d29087f7186593626da33b371b2243f981e7dd4ecc9f68104aa3d1e46a3b"),
            WEBVIEW2_NATIVE_RESOURCE_ROOT + "WebView2Loader.dll",
            new PackagedFile(163_680,
                    "c66e4a92fdc7a216118e43b7a5024ea2200e8c43f9310bf20d96a0084f82c5bc"),
            WEBVIEW2_NATIVE_RESOURCE_ROOT + "nb_flutter_webview2_host.dll",
            new PackagedFile(368_128,
                    "fc3dcd30e209f5f98d1801063bed44c0ef273a9c4c5b9a4a29135a86b9fa9760"),
            WEBVIEW2_NATIVE_RESOURCE_ROOT + "Microsoft.Web.WebView2-LICENSE.txt",
            new PackagedFile(1_487,
                    "0af8f1b807512aae39c2ac1aa4d0cae65cabecb6fd554b8439a5162a0d6eca55"),
            WEBVIEW2_NATIVE_RESOURCE_ROOT + "Microsoft.Web.WebView2-NOTICE.txt",
            new PackagedFile(3_894,
                    "106423785c5b7eba0a8e61d1837f2132e9c828e20ad530f565d981c1df60dd90"));
    private static final Set<String> CORE_RUNTIME_ENTRIES = Set.of(
            "dev/flutter/netbeans/api/DartCandidateCapacityBudget.class");
    private static final Set<String> CANVAS_RUNNER_RUNTIME_ENTRIES = Set.of(
            "dev/flutter/netbeans/canvas/runner/CanvasRunnerBundle.class",
            "dev/flutter/netbeans/canvas/runner/CanvasRunnerBundle.manifest",
            "dev/flutter/netbeans/canvas/runner/WebCanvasArtifact.manifest",
            "dev/flutter/netbeans/canvas/runner/v1/pubspec.yaml",
            "dev/flutter/netbeans/canvas/runner/v1/assets/fonts/Roboto-Regular.ttf",
            "dev/flutter/netbeans/canvas/runner/v1/assets/licenses/Roboto-LICENSE.txt",
            "dev/flutter/netbeans/canvas/runner/v1/lib/main.dart",
            "dev/flutter/netbeans/canvas/runner/v1/lib/main_web.dart",
            "dev/flutter/netbeans/canvas/runner/v1/web/index.html",
            "dev/flutter/netbeans/canvas/runner/v1/windows/runner/main.cpp",
            "dev/flutter/netbeans/canvas/runner/v1/windows/runner/resources/app_icon.ico");
    private static final Set<String> MODULE_RUNTIME_ENTRIES = Set.of(
            "dev/flutter/netbeans/plugin/designer/DesignerAtomicEditCapture.class",
            "dev/flutter/netbeans/plugin/designer/DesignerCombinedUndoRedo.class",
            "dev/flutter/netbeans/plugin/designer/DesignerCommandSessionOrchestrator.class",
            "dev/flutter/netbeans/plugin/designer/DesignerSemanticUndoableEdit.class",
            "dev/flutter/netbeans/plugin/designer/DesignerUndoableEditWrapper.class",
            "dev/flutter/netbeans/plugin/designer/GeneratedDartSymbolProbePlanner.class",
            "dev/flutter/netbeans/plugin/designer/PairAnalyzedCandidate.class",
            "dev/flutter/netbeans/plugin/designer/PairAnalyzedCandidateResult.class",
            "dev/flutter/netbeans/plugin/designer/PairAnalyzedCandidateResult$Ready.class",
            "dev/flutter/netbeans/plugin/designer/PairAnalyzedCandidateResult$Rejected.class",
            "dev/flutter/netbeans/plugin/designer/PairCandidateAnalysisTicket.class",
            "dev/flutter/netbeans/plugin/designer/FlutterDesignerPairDelete.class",
            "dev/flutter/netbeans/plugin/designer/FlutterDesignerPairDelete$Backend.class",
            "dev/flutter/netbeans/plugin/designer/FlutterDesignerPairDelete$DeleteResult.class",
            "dev/flutter/netbeans/plugin/designer/FlutterDesignerPairDelete$NetBeansBackend.class",
            "dev/flutter/netbeans/plugin/designer/FlutterDesignerPairCopy.class",
            "dev/flutter/netbeans/plugin/designer/FlutterDesignerPairCopy$Backend.class",
            "dev/flutter/netbeans/plugin/designer/FlutterDesignerPairCopy$CopyAtomicAction.class",
            "dev/flutter/netbeans/plugin/designer/FlutterDesignerPairCopy$CopyLocation.class",
            "dev/flutter/netbeans/plugin/designer/FlutterDesignerPairCopy$CopyMember.class",
            "dev/flutter/netbeans/plugin/designer/FlutterDesignerPairCopy$CopyResult.class",
            "dev/flutter/netbeans/plugin/designer/FlutterDesignerPairCopy$CopyState.class",
            "dev/flutter/netbeans/plugin/designer/FlutterDesignerPairCopy$NetBeansBackend.class",
            "dev/flutter/netbeans/plugin/designer/FlutterDesignerPairCopy$SourceMember.class",
            "dev/flutter/netbeans/plugin/designer/FlutterDesignerPairRename.class",
            "dev/flutter/netbeans/plugin/designer/FlutterDesignerPairRename$Backend.class",
            "dev/flutter/netbeans/plugin/designer/FlutterDesignerPairRename$Location.class",
            "dev/flutter/netbeans/plugin/designer/FlutterDesignerPairRename$Member.class",
            "dev/flutter/netbeans/plugin/designer/FlutterDesignerPairRename$NetBeansBackend.class",
            "dev/flutter/netbeans/plugin/designer/FlutterDesignerPairRename$RenameAtomicAction.class",
            "dev/flutter/netbeans/plugin/designer/FlutterDesignerPairRename$RenameResult.class",
            "dev/flutter/netbeans/plugin/designer/FlutterDesignerPairRename$RenameState.class",
            "dev/flutter/netbeans/plugin/designer/FlutterDesignerPairedFileNode.class",
            "dev/flutter/netbeans/plugin/designer/FlutterDesignerPairedFileNode$1.class",
            "dev/flutter/netbeans/plugin/designer/FlutterDesignerPairedFileNode$PairCopyOperations.class",
            "dev/flutter/netbeans/plugin/designer/FlutterDesignerPairedFileNode$PairCopyPasteType.class",
            "dev/flutter/netbeans/plugin/designer/PairSaveCoordinator.class",
            "dev/flutter/netbeans/plugin/designer/PairSaveCoordinator$PairCopyLease.class",
            "dev/flutter/netbeans/plugin/designer/PairSaveCoordinator$PairDeleteLease.class",
            "dev/flutter/netbeans/plugin/designer/PairSaveCoordinator$PairPathOperationLease.class",
            "dev/flutter/netbeans/plugin/designer/PairSaveCoordinator$PairPreparation.class",
            "dev/flutter/netbeans/plugin/designer/PairSaveCoordinator$PairRenameLease.class",
            "dev/flutter/netbeans/plugin/designer/PairSaveCoordinatorSnapshot.class",
            "dev/flutter/netbeans/plugin/designer/PairSaveCoordinatorStatus.class",
            "dev/flutter/netbeans/plugin/designer/PairSaveEvidence.class",
            "dev/flutter/netbeans/plugin/designer/PairSaveEvidenceDiagnostic.class",
            "dev/flutter/netbeans/plugin/designer/PairSaveEvidenceDiagnostic$Code.class",
            "dev/flutter/netbeans/plugin/designer/PairSaveEvidenceGate.class",
            "dev/flutter/netbeans/plugin/designer/PairSaveEvidenceResult.class",
            "dev/flutter/netbeans/plugin/designer/PairSaveEvidenceResult$Ready.class",
            "dev/flutter/netbeans/plugin/designer/PairSaveEvidenceResult$Rejected.class",
            "dev/flutter/netbeans/plugin/designer/guard/GuardedPersistenceRejectionSink.class",
            "dev/flutter/netbeans/plugin/designer/persistence/package-info.class",
            "dev/flutter/netbeans/plugin/designer/persistence/NetBeansPairFileTransactionBackend.class",
            "dev/flutter/netbeans/plugin/designer/persistence/PairFileRole.class",
            "dev/flutter/netbeans/plugin/designer/persistence/PairFileTransaction.class",
            "dev/flutter/netbeans/plugin/designer/persistence/PairFileTransactionBackend.class",
            "dev/flutter/netbeans/plugin/designer/persistence/PairFileTransactionIssue.class",
            "dev/flutter/netbeans/plugin/designer/persistence/PairFileTransactionIssueCode.class",
            "dev/flutter/netbeans/plugin/designer/persistence/PairFileTransactionRequest.class",
            "dev/flutter/netbeans/plugin/designer/persistence/PairFileTransactionResult.class",
            "dev/flutter/netbeans/plugin/designer/persistence/PairFileTransactionStatus.class");
    private static final String DESIGNER_CODEC_CLASS =
            "dev/flutter/netbeans/designer/codec/FdDocumentCodec.class";
    private static final String DESIGNER_SCHEMA_V1 =
            "META-INF/netbeans-flutter-designer/schema/fd-v1.schema.json";
    private static final String DESIGNER_SCHEMA_V2 =
            "META-INF/netbeans-flutter-designer/schema/fd-v2.schema.json";
    private static final String DESIGNER_SCHEMA_V3 =
            "META-INF/netbeans-flutter-designer/schema/fd-v3.schema.json";
    private static final String DESIGNER_SCHEMA_V4 =
            "META-INF/netbeans-flutter-designer/schema/fd-v4.schema.json";
    private static final Set<String> DESIGNER_PUBLIC_PACKAGES = Set.of(
            "dev.flutter.netbeans.designer.catalog.*",
            "dev.flutter.netbeans.designer.model.*",
            "dev.flutter.netbeans.plugin.designer.canvas.spi.*");
    private static final Set<String> DESIGNER_RUNTIME_ENTRIES = Set.of(
            DESIGNER_CODEC_CLASS,
            DESIGNER_SCHEMA_V1,
            DESIGNER_SCHEMA_V2,
            DESIGNER_SCHEMA_V3,
            DESIGNER_SCHEMA_V4,
            "dev/flutter/netbeans/designer/catalog/MaterialIconRegistry.class",
            "dev/flutter/netbeans/designer/catalog/material-icons-3.44.8.tsv",
            "dev/flutter/netbeans/designer/generation/DartRegionGenerator.class",
            "dev/flutter/netbeans/designer/generation/DartGenerationResult.class",
            "dev/flutter/netbeans/designer/generation/DartGenerationLimits.class",
            "dev/flutter/netbeans/designer/generation/DartGenerationDiagnostic.class",
            "dev/flutter/netbeans/designer/generation/DartGenerationDiagnosticCode.class",
            "dev/flutter/netbeans/designer/generation/DartManagedRegionId.class",
            "dev/flutter/netbeans/designer/generation/DartImportPlan.class",
            "dev/flutter/netbeans/designer/generation/DartImportDirective.class",
             "dev/flutter/netbeans/designer/generation/GeneratedDartRegions.class",
             "dev/flutter/netbeans/designer/generation/GeneratedDartRegion.class",
             "dev/flutter/netbeans/designer/generation/GeneratedDartSymbolOccurrence.class",
            "dev/flutter/netbeans/designer/command/DesignerCommandSession.class",
            "dev/flutter/netbeans/designer/command/DesignerCommandRevision.class",
            "dev/flutter/netbeans/designer/command/DesignerCommandEdit.class",
            "dev/flutter/netbeans/designer/command/DesignerRevisionPersistenceKind.class",
            "dev/flutter/netbeans/designer/command/AddWidget.class",
            "dev/flutter/netbeans/designer/command/RemoveWidget.class",
            "dev/flutter/netbeans/designer/command/MoveWidget.class",
            "dev/flutter/netbeans/designer/command/WrapWidget.class",
            "dev/flutter/netbeans/designer/command/SetProperty.class",
            "dev/flutter/netbeans/designer/command/ResetProperty.class",
            "dev/flutter/netbeans/designer/source/DartSourceIntegrityScanner.class",
            "dev/flutter/netbeans/designer/source/DartSourceIntegrityResult.class",
            "dev/flutter/netbeans/designer/source/DartDesignerSuperclassOccurrence.class",
            "dev/flutter/netbeans/designer/source/DartSourceIntegrityStatus.class",
            "dev/flutter/netbeans/designer/source/DartSourceIntegrityLimits.class",
            "dev/flutter/netbeans/designer/source/DartSourceIntegrityDiagnostic.class",
            "dev/flutter/netbeans/designer/source/DartSourceIntegrityDiagnosticCode.class",
            "dev/flutter/netbeans/designer/source/DartManagedRegionHashing.class",
            "dev/flutter/netbeans/designer/source/DartManagedRegionSnapshot.class",
            "dev/flutter/netbeans/designer/source/OriginalDartBytes.class",
            "dev/flutter/netbeans/designer/source/DartThreeWayIntegrityGate.class",
            "dev/flutter/netbeans/designer/source/DartThreeWayIntegrityResult.class",
            "dev/flutter/netbeans/designer/source/DartThreeWayIntegrityStatus.class",
            "dev/flutter/netbeans/designer/source/DartThreeWayIntegrityDiagnostic.class",
            "dev/flutter/netbeans/designer/source/DartThreeWayIntegrityDiagnosticCode.class",
            "dev/flutter/netbeans/designer/source/DartThreeWayRegionComparison.class",
            "dev/flutter/netbeans/designer/transition/DartSourceTransitionPlanner.class",
            "dev/flutter/netbeans/designer/transition/DartSourceTransitionPlan.class",
            "dev/flutter/netbeans/designer/transition/DartSourceTransitionResult.class",
            "dev/flutter/netbeans/designer/transition/DartSourceTransitionStatus.class",
            "dev/flutter/netbeans/designer/transition/DartSourceTransitionDiagnostic.class",
            "dev/flutter/netbeans/designer/transition/DartSourceTransitionDiagnosticCode.class",
            "dev/flutter/netbeans/designer/pair/DesignerPairPreparationPlanner.class",
            "dev/flutter/netbeans/designer/pair/DesignerPairPreparationResult.class",
            "dev/flutter/netbeans/designer/pair/DesignerPairPreparationStatus.class",
            "dev/flutter/netbeans/designer/pair/DesignerPairPreparationDiagnostic.class",
            "dev/flutter/netbeans/designer/pair/DesignerPairPreparationDiagnosticCode.class",
            "dev/flutter/netbeans/designer/pair/PreparedDesignerPair.class",
            "dev/flutter/netbeans/designer/copy/DesignerPairCopyPlan.class",
            "dev/flutter/netbeans/designer/copy/DesignerPairCopyPlanner.class",
            "dev/flutter/netbeans/designer/copy/DesignerPairCopyResult.class",
            "dev/flutter/netbeans/designer/copy/DesignerPairCopyResult$Code.class",
            "dev/flutter/netbeans/designer/copy/DesignerPairCopyResult$Ready.class",
            "dev/flutter/netbeans/designer/copy/DesignerPairCopyResult$Rejected.class",
            "dev/flutter/netbeans/designer/rename/DesignerPairRenamePlan.class",
            "dev/flutter/netbeans/designer/rename/DesignerPairRenamePlanner.class",
            "dev/flutter/netbeans/designer/rename/DesignerPairRenameResult.class",
            "dev/flutter/netbeans/designer/rename/DesignerPairRenameResult$Code.class",
            "dev/flutter/netbeans/designer/rename/DesignerPairRenameResult$Ready.class",
            "dev/flutter/netbeans/designer/rename/DesignerPairRenameResult$Rejected.class",
            "dev/flutter/netbeans/designer/catalog/WidgetCatalogContributor.class",
            "dev/flutter/netbeans/designer/catalog/WidgetDefinition.class",
            "dev/flutter/netbeans/designer/model/WidgetTypeId.class",
            "dev/flutter/netbeans/designer/model/PropertyName.class",
            "dev/flutter/netbeans/designer/model/PropertyValue.class",
            "dev/flutter/netbeans/designer/model/PropertyValueKind.class",
            "dev/flutter/netbeans/designer/model/SlotName.class",
            "dev/flutter/netbeans/designer/model/SlotCardinality.class");
    private static final Set<String> ANALYSIS_RUNTIME_ENTRIES = Set.of(
            "dev/flutter/netbeans/dart/DartCandidateAnalyzer.class",
            "dev/flutter/netbeans/dart/DartAnalyzerProtocolSession.class",
            "dev/flutter/netbeans/dart/DartCandidateAnalysisRequest.class",
            "dev/flutter/netbeans/dart/DartCandidateAnalysisOperation.class",
            "dev/flutter/netbeans/dart/DartCandidateAnalysisResult.class",
            "dev/flutter/netbeans/dart/DartCandidateAnalysisStatus.class",
            "dev/flutter/netbeans/dart/DartCandidateAnalysisLimits.class",
            "dev/flutter/netbeans/dart/DartCandidateAnalysisIssue.class",
            "dev/flutter/netbeans/dart/DartCandidateAnalysisIssueCode.class",
            "dev/flutter/netbeans/dart/DartCandidateHashes.class",
            "dev/flutter/netbeans/dart/DartCandidateDiagnostic.class",
            "dev/flutter/netbeans/dart/DartCandidateDiagnosticSeverity.class",
            "dev/flutter/netbeans/dart/DartCandidateSnapshot.class",
            "dev/flutter/netbeans/dart/DartCandidateWarningPolicy.class",
            "dev/flutter/netbeans/dart/DartNavigationTarget.class",
            "dev/flutter/netbeans/dart/DartSymbolEvidence.class",
            "dev/flutter/netbeans/dart/DartSymbolProbe.class");
    private static final String NAME = "Flutter and Dart Support";
    private static final String CATEGORY = "Flutter";
    private static final String SHORT_DESCRIPTION =
            "Develop Dart and Flutter applications in Apache NetBeans.";
    private static final String LONG_DESCRIPTION =
            "Adds Dart editing, analysis, completion, navigation and formatting together with "
                    + "Flutter project creation, building, execution, device management, testing and debugging "
                    + "to Apache NetBeans 30.";

    @Test
    void exposesCompletePluginManagerMetadata() throws Exception {
        Path nbm = requiredPath("nbm.file");
        String mavenVersion = requiredProperty("maven.version");
        Document info = readInfo(nbm);
        Manifest moduleManifest = readModuleManifest(nbm);
        Element module = info.getDocumentElement();
        Element manifest = firstElement(module, "manifest");
        Element license = firstElement(module, "license");

        assertEquals("netbeans-plugin-" + mavenVersion + ".nbm", module.getAttribute("distribution"));
        assertEquals(NAME, manifest.getAttribute("OpenIDE-Module-Name"));
        assertEquals(CATEGORY, manifest.getAttribute("OpenIDE-Module-Display-Category"));
        assertEquals(SHORT_DESCRIPTION,
                manifest.getAttribute("OpenIDE-Module-Short-Description"));
        assertEquals(LONG_DESCRIPTION,
                manifest.getAttribute("OpenIDE-Module-Long-Description"));
        assertEquals(
                DESIGNER_PUBLIC_PACKAGES,
                Set.of(moduleManifest.getMainAttributes()
                        .getValue("OpenIDE-Module-Public-Packages")
                        .split("\\s*,\\s*")),
                "the NBM must export only the supported Designer contributor API");

        String specificationVersion = mavenVersion.replaceFirst("-SNAPSHOT$", "");
        assertEquals(specificationVersion,
                manifest.getAttribute("OpenIDE-Module-Specification-Version"));
        String implementationVersion = manifest.getAttribute("OpenIDE-Module-Implementation-Version");
        if (mavenVersion.endsWith("-SNAPSHOT")) {
            assertTrue(Pattern.matches(
                    Pattern.quote(specificationVersion) + "-\\d{8}", implementationVersion),
                    () -> "unexpected snapshot implementation version: " + implementationVersion);
        } else {
            assertEquals(specificationVersion, implementationVersion);
        }

        assertEquals(module.getAttribute("license"), license.getAttribute("name"));
        String packagedLicense = normalizeNewlines(license.getTextContent());
        String sourceLicense = normalizeNewlines(Files.readString(
                requiredPath("source.license"), StandardCharsets.UTF_8)).stripTrailing();
        assertTrue(packagedLicense.contains(sourceLicense), "NBM must contain the complete LICENSE text");
        assertTrue(packagedLicense.contains("Apache License"));
        assertTrue(packagedLicense.contains("Version 2.0"));
        assertFalse(packagedLicense.contains("Unknown"));
        var attributes = manifest.getAttributes();
        for (int index = 0; index < attributes.getLength(); index++) {
            String attributeName = attributes.item(index).getNodeName();
            String attributeValue = attributes.item(index).getNodeValue();
            assertFalse(
                    attributeValue.contains("<undefined>"),
                    () -> "undefined manifest metadata: " + attributeName);
        }
    }

    @Test
    void packagesDesignerCodecAndSchemaInTheNbmRuntime() throws Exception {
        assertNestedJarContains(
                requiredPath("nbm.file"),
                DESIGNER_LIBRARY,
                "Flutter Designer",
                DESIGNER_RUNTIME_ENTRIES);
    }

    @Test
    void packagesCandidateAnalyzerInTheNbmRuntime() throws Exception {
        assertNestedJarContains(
                requiredPath("nbm.file"),
                ANALYSIS_LIBRARY,
                "Dart analysis",
                ANALYSIS_RUNTIME_ENTRIES);
    }

    @Test
    void packagesSharedCandidateCapacityInTheNbmRuntime() throws Exception {
        assertNestedJarContains(
                requiredPath("nbm.file"),
                CORE_LIBRARY,
                "Flutter core API",
                CORE_RUNTIME_ENTRIES);
    }

    @Test
    void packagesNativeCanvasRunnerSourcesInTheNbmRuntime() throws Exception {
        assertNestedJarContains(
                requiredPath("nbm.file"),
                CANVAS_RUNNER_LIBRARY,
                "Flutter native Canvas runner",
                CANVAS_RUNNER_RUNTIME_ENTRIES);
    }

    @Test
    void packagesDesignerPairOperationsInTheModuleJar() throws Exception {
        assertNestedJarContains(
                requiredPath("nbm.file"),
                MODULE_JAR,
                "Flutter NetBeans module",
                MODULE_RUNTIME_ENTRIES);
    }

    @Test
    void packagesPinnedWindowsWebView2NativeBundleInTheModuleJar() throws Exception {
        Path nbm = requiredPath("nbm.file");
        byte[] moduleJar;
        try (ZipFile zip = new ZipFile(nbm.toFile())) {
            ZipEntry entry = zip.getEntry(MODULE_JAR);
            assertNotNull(entry, "NBM is missing its NetBeans module JAR");
            assertTrue(entry.getSize() > 0, "NetBeans module JAR is empty");
            assertTrue(entry.getSize() <= 64L * 1024L * 1024L,
                    "NetBeans module JAR exceeds the packaging safety bound");
            try (InputStream input = zip.getInputStream(entry)) {
                moduleJar = input.readAllBytes();
            }
        }

        Set<String> missing = new HashSet<>(WEBVIEW2_NATIVE_FILES.keySet());
        Set<String> observed = new HashSet<>();
        try (ZipInputStream nested = new ZipInputStream(
                new ByteArrayInputStream(moduleJar))) {
            ZipEntry entry;
            while ((entry = nested.getNextEntry()) != null) {
                String entryName = entry.getName();
                PackagedFile expected = WEBVIEW2_NATIVE_FILES.get(entryName);
                if (expected == null) {
                    continue;
                }
                assertTrue(observed.add(entryName),
                        () -> "duplicate packaged WebView2 native entry: " + entryName);
                byte[] bytes = nested.readAllBytes();
                assertEquals(expected.size(), (long) bytes.length,
                        () -> "packaged WebView2 native size drift: " + entryName);
                assertEquals(expected.sha256(), sha256(bytes),
                        () -> "packaged WebView2 native digest drift: " + entryName);
                missing.remove(entryName);
            }
        }
        assertTrue(missing.isEmpty(),
                () -> "packaged WebView2 native bundle is missing entries: " + missing);
    }

    private static void assertNestedJarContains(
            Path nbm,
            String libraryPath,
            String libraryName,
            Set<String> requiredEntries) throws Exception {
        byte[] libraryJar;
        try (ZipFile zip = new ZipFile(nbm.toFile())) {
            ZipEntry entry = zip.getEntry(libraryPath);
            assertNotNull(entry, "NBM is missing the " + libraryName + " runtime library");
            assertTrue(entry.getSize() > 0, libraryName + " runtime library is empty");
            assertTrue(entry.getSize() <= 16L * 1024L * 1024L,
                    libraryName + " runtime library exceeds the packaging safety bound");
            try (InputStream input = zip.getInputStream(entry)) {
                libraryJar = input.readAllBytes();
            }
        }

        Set<String> missing = new HashSet<>(requiredEntries);
        try (ZipInputStream nested = new ZipInputStream(
                new ByteArrayInputStream(libraryJar))) {
            ZipEntry entry;
            while ((entry = nested.getNextEntry()) != null) {
                missing.remove(entry.getName());
            }
        }

        assertTrue(missing.isEmpty(),
                () -> "packaged " + libraryName
                + " runtime is missing entries: " + missing);
    }

    private static Document readInfo(Path nbm) throws Exception {
        try (ZipFile zip = new ZipFile(nbm.toFile())) {
            ZipEntry entry = zip.getEntry("Info/info.xml");
            assertNotNull(entry, "NBM is missing Info/info.xml");
            try (InputStream input = zip.getInputStream(entry)) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setXIncludeAware(false);
                factory.setExpandEntityReferences(false);
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                var builder = factory.newDocumentBuilder();
                builder.setEntityResolver((publicId, systemId) ->
                        new InputSource(new StringReader("")));
                return builder.parse(input);
            }
        }
    }

    private static Manifest readModuleManifest(Path nbm) throws Exception {
        byte[] moduleJar;
        try (ZipFile zip = new ZipFile(nbm.toFile())) {
            ZipEntry entry = zip.getEntry(MODULE_JAR);
            assertNotNull(entry, "NBM is missing its NetBeans module JAR");
            assertTrue(entry.getSize() > 0, "NetBeans module JAR is empty");
            assertTrue(entry.getSize() <= 64L * 1024L * 1024L,
                    "NetBeans module JAR exceeds the packaging safety bound");
            try (InputStream input = zip.getInputStream(entry)) {
                moduleJar = input.readAllBytes();
            }
        }
        try (JarInputStream nested = new JarInputStream(
                new ByteArrayInputStream(moduleJar))) {
            Manifest manifest = nested.getManifest();
            assertNotNull(manifest, "NetBeans module JAR is missing META-INF/MANIFEST.MF");
            return manifest;
        }
    }

    private static Element firstElement(Element parent, String tagName) {
        Element element = (Element) parent.getElementsByTagName(tagName).item(0);
        assertNotNull(element, "Info/info.xml is missing <" + tagName + ">");
        return element;
    }

    private static Path requiredPath(String name) throws IOException {
        Path path = Path.of(requiredProperty(name));
        assertTrue(Files.isRegularFile(path), () -> name + " does not point to a file: " + path);
        return path;
    }

    private static String requiredProperty(String name) {
        String value = System.getProperty(name);
        assertNotNull(value, "missing system property: " + name);
        assertFalse(value.isBlank(), "blank system property: " + name);
        return value;
    }

    private static String normalizeNewlines(String value) {
        return value.replace("\r\n", "\n").replace('\r', '\n');
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JDK does not provide SHA-256", exception);
        }
    }

    private record PackagedFile(long size, String sha256) {
        private PackagedFile {
            if (size <= 0 || !sha256.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("invalid packaged file evidence");
            }
        }
    }
}
