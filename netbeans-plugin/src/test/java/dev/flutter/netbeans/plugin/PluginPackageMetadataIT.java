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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
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
                    "4be6e2ac553ac43b5c6810e368e1da85379d0b4bc9d3cbfda8931af3b429f20c"),
            WEBVIEW2_NATIVE_RESOURCE_ROOT + "WebView2Loader.dll",
            new PackagedFile(163_680,
                    "c66e4a92fdc7a216118e43b7a5024ea2200e8c43f9310bf20d96a0084f82c5bc"),
            WEBVIEW2_NATIVE_RESOURCE_ROOT + "nb_flutter_webview2_host.dll",
            new PackagedFile(384_000,
                    "8886b2d4edc45b631f00a05dd435bbcf87c3af42755b85db2f3a5dc32aa5577f"),
            WEBVIEW2_NATIVE_RESOURCE_ROOT + "Microsoft.Web.WebView2-LICENSE.txt",
            new PackagedFile(1_487,
                    "9995174528dba139ca753d02d8667dbec49f65ab17e65c643a914f2b58cfb4a2"),
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
    private static final String LONG_DESCRIPTION_INTRO =
            "Adds Dart editing, analysis, completion, navigation and formatting together with "
                    + "Flutter project creation, building, execution, device management, testing and debugging "
                    + "to Apache NetBeans.";
    private static final String MODULE_BUNDLE =
            "dev/flutter/netbeans/plugin/Bundle.properties";
    private static final String LONG_DESCRIPTION_KEY = "OpenIDE-Module-Long-Description";
    private static final Map<String, String> CONTACT_LINKS = Map.of(
            "mailto:hrytsenkovalentyn@gmail.com", "hrytsenkovalentyn@gmail.com",
            "https://t.me/netbeans_flutter_plugin", "netbeans_flutter_plugin",
            "https://www.paypal.com/donate/?hosted_button_id=GRQBC554NA356", "Donate via PayPal");

    @Test
    void exposesCompletePluginManagerMetadata() throws Exception {
        Path nbm = requiredPath("nbm.file");
        String mavenVersion = requiredProperty("maven.version");
        String expectedNbmFileName = "netbeans-flutter-plugin-" + mavenVersion + ".nbm";
        assertEquals(expectedNbmFileName, nbm.getFileName().toString(),
                "the distributable NBM filename must use the netbeans-flutter-plugin base");
        Document info = readInfo(nbm);
        Manifest moduleManifest = readModuleManifest(nbm);
        Element module = info.getDocumentElement();
        Element manifest = firstElement(module, "manifest");
        Element license = firstElement(module, "license");

        assertEquals(expectedNbmFileName, module.getAttribute("distribution"),
                "Plugin Manager distribution must match the actual NBM filename");
        assertEquals(NAME, manifest.getAttribute("OpenIDE-Module-Name"));
        assertEquals(CATEGORY, manifest.getAttribute("OpenIDE-Module-Display-Category"));
        assertEquals(SHORT_DESCRIPTION,
                manifest.getAttribute("OpenIDE-Module-Short-Description"));
        assertTrue(manifest.getAttribute(LONG_DESCRIPTION_KEY).contains(LONG_DESCRIPTION_INTRO),
                "the plugin description must retain its version-neutral feature introduction");
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
    void packagesVisibleDeveloperContactAndSupportLinksInBothDescriptions() throws Exception {
        Path nbm = requiredPath("nbm.file");
        Element manifest = firstElement(readInfo(nbm).getDocumentElement(), "manifest");
        // DOM decoding must yield usable HTML, not double-escaped literal markup.
        String infoDescription = manifest.getAttribute(LONG_DESCRIPTION_KEY);
        String bundleDescription = readPackagedModuleBundle(nbm).getProperty(LONG_DESCRIPTION_KEY);
        assertNotNull(bundleDescription, "the packaged module bundle must contain the plugin description");
        assertEquals(bundleDescription, infoDescription,
                "Plugin Manager metadata and the actual runtime bundle must expose the same description");
        assertContactDescription(infoDescription, "Info/info.xml");
        assertContactDescription(bundleDescription, MODULE_BUNDLE);
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

    private static void assertContactDescription(String html, String origin) throws IOException {
        StringBuilder visibleText = new StringBuilder();
        Map<String, StringBuilder> anchorText = new LinkedHashMap<>();
        new ParserDelegator().parse(new StringReader(html), new HTMLEditorKit.ParserCallback() {
            private String currentHref;

            @Override
            public void handleStartTag(HTML.Tag tag, MutableAttributeSet attributes, int position) {
                if (tag == HTML.Tag.A) {
                    Object href = attributes.getAttribute(HTML.Attribute.HREF);
                    currentHref = href == null ? null : href.toString();
                    if (currentHref != null) {
                        anchorText.computeIfAbsent(currentHref, ignored -> new StringBuilder());
                    }
                }
            }

            @Override
            public void handleText(char[] text, int position) {
                visibleText.append(text).append(' ');
                if (currentHref != null) {
                    anchorText.get(currentHref).append(text).append(' ');
                }
            }

            @Override
            public void handleEndTag(HTML.Tag tag, int position) {
                if (tag == HTML.Tag.A) {
                    currentHref = null;
                }
            }
        }, true);
        String rendered = visibleText.toString().replaceAll("\\s+", " ").strip();
        assertTrue(rendered.contains(LONG_DESCRIPTION_INTRO), origin + " must render its feature introduction");
        assertTrue(rendered.contains("Developer contact"), origin + " must visibly label developer contact");
        assertTrue(rendered.contains("Support the project"), origin + " must visibly label project support");
        assertFalse(rendered.contains("Apache NetBeans 30"), origin + " must not advertise a fixed old IDE release");
        CONTACT_LINKS.forEach((href, label) -> {
            assertTrue(anchorText.containsKey(href), () -> origin + " is missing a real HTML anchor for " + href);
            assertTrue(anchorText.get(href).toString().contains(label),
                    () -> origin + " must render the " + label + " link with visible English text");
        });
    }

    private static Properties readPackagedModuleBundle(Path nbm) throws IOException {
        try (ZipFile zip = new ZipFile(nbm.toFile())) {
            ZipEntry module = zip.getEntry(MODULE_JAR);
            assertNotNull(module, "NBM is missing its NetBeans module JAR");
            try (ZipInputStream nested = new ZipInputStream(zip.getInputStream(module))) {
                ZipEntry entry;
                while ((entry = nested.getNextEntry()) != null) {
                    if (MODULE_BUNDLE.equals(entry.getName())) {
                        byte[] bytes = nested.readNBytes(65_537);
                        assertTrue(bytes.length <= 65_536, "packaged module bundle exceeds the metadata safety bound");
                        Properties bundle = new Properties();
                        bundle.load(new StringReader(new String(bytes, StandardCharsets.UTF_8)));
                        return bundle;
                    }
                }
            }
        }
        throw new IOException("NBM module JAR is missing " + MODULE_BUNDLE);
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
