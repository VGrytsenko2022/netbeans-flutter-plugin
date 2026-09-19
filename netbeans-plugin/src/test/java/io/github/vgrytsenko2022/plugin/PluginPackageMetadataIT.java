package io.github.vgrytsenko2022.plugin;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
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
import javax.swing.BorderFactory;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import javax.swing.text.html.parser.ParserDelegator;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

class PluginPackageMetadataIT {

    private static final String MODULE_JAR
            = "netbeans/modules/io-github-vgrytsenko2022-netbeans-plugin.jar";

    private static final String DESIGNER_LIBRARY
            = "netbeans/modules/ext/io.github.vgrytsenko2022.netbeans-plugin/"
            + "io-github-vgrytsenko2022/flutter-designer.jar";

    private static final String ANALYSIS_LIBRARY
            = "netbeans/modules/ext/io.github.vgrytsenko2022.netbeans-plugin/"
            + "io-github-vgrytsenko2022/dart-analysis.jar";

    private static final String CORE_LIBRARY
            = "netbeans/modules/ext/io.github.vgrytsenko2022.netbeans-plugin/"
            + "io-github-vgrytsenko2022/flutter-core-api.jar";

    private static final String CANVAS_RUNNER_LIBRARY
            = "netbeans/modules/ext/io.github.vgrytsenko2022.netbeans-plugin/"
            + "io-github-vgrytsenko2022/flutter-canvas-runner.jar";
    private static final String WEBVIEW2_NATIVE_RESOURCE_ROOT
            = "io/github/vgrytsenko2022/plugin/designer/canvas/webview2/win-x64/";
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
            "io/github/vgrytsenko2022/api/DartCandidateCapacityBudget.class");
    private static final Set<String> CANVAS_RUNNER_RUNTIME_ENTRIES = Set.of(
            "io/github/vgrytsenko2022/canvas/runner/CanvasRunnerBundle.class",
            "io/github/vgrytsenko2022/canvas/runner/CanvasRunnerBundle.manifest",
            "io/github/vgrytsenko2022/canvas/runner/WebCanvasArtifact.manifest",
            "io/github/vgrytsenko2022/canvas/runner/v1/pubspec.yaml",
            "io/github/vgrytsenko2022/canvas/runner/v1/assets/fonts/Roboto-Regular.ttf",
            "io/github/vgrytsenko2022/canvas/runner/v1/assets/licenses/Roboto-LICENSE.txt",
            "io/github/vgrytsenko2022/canvas/runner/v1/lib/main.dart",
            "io/github/vgrytsenko2022/canvas/runner/v1/lib/main_web.dart",
            "io/github/vgrytsenko2022/canvas/runner/v1/web/index.html",
            "io/github/vgrytsenko2022/canvas/runner/v1/windows/runner/main.cpp",
            "io/github/vgrytsenko2022/canvas/runner/v1/windows/runner/resources/app_icon.ico");
    private static final Set<String> MODULE_RUNTIME_ENTRIES = Set.of(
            "io/github/vgrytsenko2022/plugin/designer/DesignerAtomicEditCapture.class",
            "io/github/vgrytsenko2022/plugin/designer/DesignerCombinedUndoRedo.class",
            "io/github/vgrytsenko2022/plugin/designer/DesignerCommandSessionOrchestrator.class",
            "io/github/vgrytsenko2022/plugin/designer/DesignerSemanticUndoableEdit.class",
            "io/github/vgrytsenko2022/plugin/designer/DesignerUndoableEditWrapper.class",
            "io/github/vgrytsenko2022/plugin/designer/GeneratedDartSymbolProbePlanner.class",
            "io/github/vgrytsenko2022/plugin/designer/PairAnalyzedCandidate.class",
            "io/github/vgrytsenko2022/plugin/designer/PairAnalyzedCandidateResult.class",
            "io/github/vgrytsenko2022/plugin/designer/PairAnalyzedCandidateResult$Ready.class",
            "io/github/vgrytsenko2022/plugin/designer/PairAnalyzedCandidateResult$Rejected.class",
            "io/github/vgrytsenko2022/plugin/designer/PairCandidateAnalysisTicket.class",
            "io/github/vgrytsenko2022/plugin/designer/FlutterDesignerPairDelete.class",
            "io/github/vgrytsenko2022/plugin/designer/FlutterDesignerPairDelete$Backend.class",
            "io/github/vgrytsenko2022/plugin/designer/FlutterDesignerPairDelete$DeleteResult.class",
            "io/github/vgrytsenko2022/plugin/designer/FlutterDesignerPairDelete$NetBeansBackend.class",
            "io/github/vgrytsenko2022/plugin/designer/FlutterDesignerPairCopy.class",
            "io/github/vgrytsenko2022/plugin/designer/FlutterDesignerPairCopy$Backend.class",
            "io/github/vgrytsenko2022/plugin/designer/FlutterDesignerPairCopy$CopyAtomicAction.class",
            "io/github/vgrytsenko2022/plugin/designer/FlutterDesignerPairCopy$CopyLocation.class",
            "io/github/vgrytsenko2022/plugin/designer/FlutterDesignerPairCopy$CopyMember.class",
            "io/github/vgrytsenko2022/plugin/designer/FlutterDesignerPairCopy$CopyResult.class",
            "io/github/vgrytsenko2022/plugin/designer/FlutterDesignerPairCopy$CopyState.class",
            "io/github/vgrytsenko2022/plugin/designer/FlutterDesignerPairCopy$NetBeansBackend.class",
            "io/github/vgrytsenko2022/plugin/designer/FlutterDesignerPairCopy$SourceMember.class",
            "io/github/vgrytsenko2022/plugin/designer/FlutterDesignerPairRename.class",
            "io/github/vgrytsenko2022/plugin/designer/FlutterDesignerPairRename$Backend.class",
            "io/github/vgrytsenko2022/plugin/designer/FlutterDesignerPairRename$Location.class",
            "io/github/vgrytsenko2022/plugin/designer/FlutterDesignerPairRename$Member.class",
            "io/github/vgrytsenko2022/plugin/designer/FlutterDesignerPairRename$NetBeansBackend.class",
            "io/github/vgrytsenko2022/plugin/designer/FlutterDesignerPairRename$RenameAtomicAction.class",
            "io/github/vgrytsenko2022/plugin/designer/FlutterDesignerPairRename$RenameResult.class",
            "io/github/vgrytsenko2022/plugin/designer/FlutterDesignerPairRename$RenameState.class",
            "io/github/vgrytsenko2022/plugin/designer/FlutterDesignerPairedFileNode.class",
            "io/github/vgrytsenko2022/plugin/designer/FlutterDesignerPairedFileNode$1.class",
            "io/github/vgrytsenko2022/plugin/designer/FlutterDesignerPairedFileNode$PairCopyOperations.class",
            "io/github/vgrytsenko2022/plugin/designer/FlutterDesignerPairedFileNode$PairCopyPasteType.class",
            "io/github/vgrytsenko2022/plugin/designer/PairSaveCoordinator.class",
            "io/github/vgrytsenko2022/plugin/designer/FlutterDesignerEventsBridge.class",
            "io/github/vgrytsenko2022/plugin/designer/properties/FlutterWidgetEventsContext.class",
            "io/github/vgrytsenko2022/plugin/designer/properties/FlutterWidgetEventPropertyEditor.class",
            "io/github/vgrytsenko2022/plugin/designer/properties/FlutterWidgetStatePropertyEditor.class",
            "io/github/vgrytsenko2022/plugin/designer/PairSaveCoordinator$PairCopyLease.class",
            "io/github/vgrytsenko2022/plugin/designer/PairSaveCoordinator$PairDeleteLease.class",
            "io/github/vgrytsenko2022/plugin/designer/PairSaveCoordinator$PairPathOperationLease.class",
            "io/github/vgrytsenko2022/plugin/designer/PairSaveCoordinator$PairPreparation.class",
            "io/github/vgrytsenko2022/plugin/designer/PairSaveCoordinator$PairRenameLease.class",
            "io/github/vgrytsenko2022/plugin/designer/PairSaveCoordinatorSnapshot.class",
            "io/github/vgrytsenko2022/plugin/designer/PairSaveCoordinatorStatus.class",
            "io/github/vgrytsenko2022/plugin/designer/PairSaveEvidence.class",
            "io/github/vgrytsenko2022/plugin/designer/PairSaveEvidenceDiagnostic.class",
            "io/github/vgrytsenko2022/plugin/designer/PairSaveEvidenceDiagnostic$Code.class",
            "io/github/vgrytsenko2022/plugin/designer/PairSaveEvidenceGate.class",
            "io/github/vgrytsenko2022/plugin/designer/PairSaveEvidenceResult.class",
            "io/github/vgrytsenko2022/plugin/designer/PairSaveEvidenceResult$Ready.class",
            "io/github/vgrytsenko2022/plugin/designer/PairSaveEvidenceResult$Rejected.class",
            "io/github/vgrytsenko2022/plugin/designer/guard/GuardedPersistenceRejectionSink.class",
            "io/github/vgrytsenko2022/plugin/designer/persistence/package-info.class",
            "io/github/vgrytsenko2022/plugin/designer/persistence/NetBeansPairFileTransactionBackend.class",
            "io/github/vgrytsenko2022/plugin/designer/persistence/PairFileRole.class",
            "io/github/vgrytsenko2022/plugin/designer/persistence/PairFileTransaction.class",
            "io/github/vgrytsenko2022/plugin/designer/persistence/PairFileTransactionBackend.class",
            "io/github/vgrytsenko2022/plugin/designer/persistence/PairFileTransactionIssue.class",
            "io/github/vgrytsenko2022/plugin/designer/persistence/PairFileTransactionIssueCode.class",
            "io/github/vgrytsenko2022/plugin/designer/persistence/PairFileTransactionRequest.class",
            "io/github/vgrytsenko2022/plugin/designer/persistence/PairFileTransactionResult.class",
            "io/github/vgrytsenko2022/plugin/designer/persistence/PairFileTransactionStatus.class");
    private static final String DESIGNER_CODEC_CLASS
            = "io/github/vgrytsenko2022/designer/codec/FdDocumentCodec.class";
    private static final String DESIGNER_SCHEMA_V1
            = "META-INF/netbeans-flutter-designer/schema/fd-v1.schema.json";
    private static final String DESIGNER_SCHEMA_V2
            = "META-INF/netbeans-flutter-designer/schema/fd-v2.schema.json";
    private static final String DESIGNER_SCHEMA_V3
            = "META-INF/netbeans-flutter-designer/schema/fd-v3.schema.json";
    private static final String DESIGNER_SCHEMA_V4
            = "META-INF/netbeans-flutter-designer/schema/fd-v4.schema.json";
    private static final Set<String> DESIGNER_PUBLIC_PACKAGES = Set.of(
            "io.github.vgrytsenko2022.designer.catalog.*",
            "io.github.vgrytsenko2022.designer.model.*",
            "io.github.vgrytsenko2022.plugin.designer.canvas.spi.*");
    private static final Set<String> DESIGNER_RUNTIME_ENTRIES = Set.of(
            DESIGNER_CODEC_CLASS,
            DESIGNER_SCHEMA_V1,
            DESIGNER_SCHEMA_V2,
            DESIGNER_SCHEMA_V3,
            DESIGNER_SCHEMA_V4,
            "META-INF/netbeans-flutter-designer/schema/fd-v15.schema.json",
            "META-INF/netbeans-flutter-designer/schema/fd-v16.schema.json",
            "META-INF/netbeans-flutter-designer/schema/fd-v17.schema.json",
            "io/github/vgrytsenko2022/designer/model/PropertyValue$GradientValue.class",
            "io/github/vgrytsenko2022/designer/catalog/ShaderMaskWidgetPropertySchema.class",
            "io/github/vgrytsenko2022/designer/catalog/GestureDetectorWidgetPropertySchema.class",
            "io/github/vgrytsenko2022/designer/model/PropertyValue$PointerDeviceKindSetValue.class",
            "io/github/vgrytsenko2022/designer/model/StateBinding.class",
            "io/github/vgrytsenko2022/designer/model/StatePropertyBinding.class",
            "io/github/vgrytsenko2022/designer/command/CreateStateBinding.class",
            "io/github/vgrytsenko2022/designer/command/BindPropertyToState.class",
            "io/github/vgrytsenko2022/designer/command/RenameStateField.class",
            "io/github/vgrytsenko2022/designer/events/DartEventHandlerSource.class",
            "io/github/vgrytsenko2022/designer/transition/DartUserSourceProjection.class",
            "io/github/vgrytsenko2022/designer/catalog/MaterialIconRegistry.class",
            "io/github/vgrytsenko2022/designer/catalog/material-icons-3.44.8.tsv",
            "io/github/vgrytsenko2022/designer/generation/DartRegionGenerator.class",
            "io/github/vgrytsenko2022/designer/generation/DartGenerationResult.class",
            "io/github/vgrytsenko2022/designer/generation/DartGenerationLimits.class",
            "io/github/vgrytsenko2022/designer/generation/DartGenerationDiagnostic.class",
            "io/github/vgrytsenko2022/designer/generation/DartGenerationDiagnosticCode.class",
            "io/github/vgrytsenko2022/designer/generation/DartManagedRegionId.class",
            "io/github/vgrytsenko2022/designer/generation/DartImportPlan.class",
            "io/github/vgrytsenko2022/designer/generation/DartImportDirective.class",
            "io/github/vgrytsenko2022/designer/generation/GeneratedDartRegions.class",
            "io/github/vgrytsenko2022/designer/generation/GeneratedDartRegion.class",
            "io/github/vgrytsenko2022/designer/generation/GeneratedDartSymbolOccurrence.class",
            "io/github/vgrytsenko2022/designer/command/DesignerCommandSession.class",
            "io/github/vgrytsenko2022/designer/command/DesignerCommandRevision.class",
            "io/github/vgrytsenko2022/designer/command/DesignerCommandEdit.class",
            "io/github/vgrytsenko2022/designer/command/DesignerRevisionPersistenceKind.class",
            "io/github/vgrytsenko2022/designer/command/AddWidget.class",
            "io/github/vgrytsenko2022/designer/command/RemoveWidget.class",
            "io/github/vgrytsenko2022/designer/command/MoveWidget.class",
            "io/github/vgrytsenko2022/designer/command/WrapWidget.class",
            "io/github/vgrytsenko2022/designer/command/SetProperty.class",
            "io/github/vgrytsenko2022/designer/command/ResetProperty.class",
            "io/github/vgrytsenko2022/designer/source/DartSourceIntegrityScanner.class",
            "io/github/vgrytsenko2022/designer/source/DartSourceIntegrityResult.class",
            "io/github/vgrytsenko2022/designer/source/DartDesignerSuperclassOccurrence.class",
            "io/github/vgrytsenko2022/designer/source/DartSourceIntegrityStatus.class",
            "io/github/vgrytsenko2022/designer/source/DartSourceIntegrityLimits.class",
            "io/github/vgrytsenko2022/designer/source/DartSourceIntegrityDiagnostic.class",
            "io/github/vgrytsenko2022/designer/source/DartSourceIntegrityDiagnosticCode.class",
            "io/github/vgrytsenko2022/designer/source/DartManagedRegionHashing.class",
            "io/github/vgrytsenko2022/designer/source/DartManagedRegionSnapshot.class",
            "io/github/vgrytsenko2022/designer/source/OriginalDartBytes.class",
            "io/github/vgrytsenko2022/designer/source/DartThreeWayIntegrityGate.class",
            "io/github/vgrytsenko2022/designer/source/DartThreeWayIntegrityResult.class",
            "io/github/vgrytsenko2022/designer/source/DartThreeWayIntegrityStatus.class",
            "io/github/vgrytsenko2022/designer/source/DartThreeWayIntegrityDiagnostic.class",
            "io/github/vgrytsenko2022/designer/source/DartThreeWayIntegrityDiagnosticCode.class",
            "io/github/vgrytsenko2022/designer/source/DartThreeWayRegionComparison.class",
            "io/github/vgrytsenko2022/designer/transition/DartSourceTransitionPlanner.class",
            "io/github/vgrytsenko2022/designer/transition/DartSourceTransitionPlan.class",
            "io/github/vgrytsenko2022/designer/transition/DartSourceTransitionResult.class",
            "io/github/vgrytsenko2022/designer/transition/DartSourceTransitionStatus.class",
            "io/github/vgrytsenko2022/designer/transition/DartSourceTransitionDiagnostic.class",
            "io/github/vgrytsenko2022/designer/transition/DartSourceTransitionDiagnosticCode.class",
            "io/github/vgrytsenko2022/designer/pair/DesignerPairPreparationPlanner.class",
            "io/github/vgrytsenko2022/designer/pair/DesignerPairPreparationResult.class",
            "io/github/vgrytsenko2022/designer/pair/DesignerPairPreparationStatus.class",
            "io/github/vgrytsenko2022/designer/pair/DesignerPairPreparationDiagnostic.class",
            "io/github/vgrytsenko2022/designer/pair/DesignerPairPreparationDiagnosticCode.class",
            "io/github/vgrytsenko2022/designer/pair/PreparedDesignerPair.class",
            "io/github/vgrytsenko2022/designer/copy/DesignerPairCopyPlan.class",
            "io/github/vgrytsenko2022/designer/copy/DesignerPairCopyPlanner.class",
            "io/github/vgrytsenko2022/designer/copy/DesignerPairCopyResult.class",
            "io/github/vgrytsenko2022/designer/copy/DesignerPairCopyResult$Code.class",
            "io/github/vgrytsenko2022/designer/copy/DesignerPairCopyResult$Ready.class",
            "io/github/vgrytsenko2022/designer/copy/DesignerPairCopyResult$Rejected.class",
            "io/github/vgrytsenko2022/designer/rename/DesignerPairRenamePlan.class",
            "io/github/vgrytsenko2022/designer/rename/DesignerPairRenamePlanner.class",
            "io/github/vgrytsenko2022/designer/rename/DesignerPairRenameResult.class",
            "io/github/vgrytsenko2022/designer/rename/DesignerPairRenameResult$Code.class",
            "io/github/vgrytsenko2022/designer/rename/DesignerPairRenameResult$Ready.class",
            "io/github/vgrytsenko2022/designer/rename/DesignerPairRenameResult$Rejected.class",
            "io/github/vgrytsenko2022/designer/catalog/WidgetCatalogContributor.class",
            "io/github/vgrytsenko2022/designer/catalog/WidgetDefinition.class",
            "io/github/vgrytsenko2022/designer/model/WidgetTypeId.class",
            "io/github/vgrytsenko2022/designer/model/PropertyName.class",
            "io/github/vgrytsenko2022/designer/model/PropertyValue.class",
            "io/github/vgrytsenko2022/designer/model/PropertyValueKind.class",
            "io/github/vgrytsenko2022/designer/model/SlotName.class",
            "io/github/vgrytsenko2022/designer/model/SlotCardinality.class");
    private static final Set<String> ANALYSIS_RUNTIME_ENTRIES = Set.of(
            "io/github/vgrytsenko2022/dart/DartCandidateAnalyzer.class",
            "io/github/vgrytsenko2022/dart/DartAnalyzerProtocolSession.class",
            "io/github/vgrytsenko2022/dart/DartCandidateAnalysisRequest.class",
            "io/github/vgrytsenko2022/dart/DartCandidateAnalysisOperation.class",
            "io/github/vgrytsenko2022/dart/DartCandidateAnalysisResult.class",
            "io/github/vgrytsenko2022/dart/DartCandidateAnalysisStatus.class",
            "io/github/vgrytsenko2022/dart/DartCandidateAnalysisLimits.class",
            "io/github/vgrytsenko2022/dart/DartCandidateAnalysisIssue.class",
            "io/github/vgrytsenko2022/dart/DartCandidateAnalysisIssueCode.class",
            "io/github/vgrytsenko2022/dart/DartCandidateHashes.class",
            "io/github/vgrytsenko2022/dart/DartCandidateDiagnostic.class",
            "io/github/vgrytsenko2022/dart/DartCandidateDiagnosticSeverity.class",
            "io/github/vgrytsenko2022/dart/DartCandidateSnapshot.class",
            "io/github/vgrytsenko2022/dart/DartCandidateWarningPolicy.class",
            "io/github/vgrytsenko2022/dart/DartNavigationTarget.class",
            "io/github/vgrytsenko2022/dart/DartSymbolEvidence.class",
            "io/github/vgrytsenko2022/dart/DartSymbolProbe.class");
    private static final String NAME = "Flutter and Dart Support";
    private static final String CATEGORY = "Flutter";
    private static final String SHORT_DESCRIPTION
            = "Develop Dart and Flutter applications in Apache NetBeans.";
    private static final String LONG_DESCRIPTION_INTRO
            = "Adds Dart editing, analysis, completion, navigation and formatting together with "
            + "Flutter project creation, building, execution, device management, testing and debugging "
            + "to Apache NetBeans.";
    private static final String MODULE_BUNDLE
            = "io/github/vgrytsenko2022/plugin/Bundle.properties";
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
    void descriptionRemovesThePluginManagerHeadingGapWithoutClippingItsFirstLine() throws Exception {
        String description = firstElement(readInfo(requiredPath("nbm.file")).getDocumentElement(), "manifest")
                .getAttribute(LONG_DESCRIPTION_KEY);
        String uncompensatedDescription = description
                .replaceFirst("(?i)^<html>\\s*<div\\b[^>]*>", "<html>")
                .replaceFirst("(?i)</div>\\s*</html>$", "</html>");
        assertFalse(description.equals(uncompensatedDescription),
                "The negative control must remove the description's outer spacing compensation");
        // UnitDetails supplies <h3>Plugin Description</h3> and strips only the opening <html>.
        // Its HTMLEditorKitEx delegates text layout unchanged to Swing's HTML factory.
        SwingUtilities.invokeAndWait(() -> {
            for (int fontSize : new int[]{12, 16}) {
                for (int width : new int[]{300, 460}) {
                    for (boolean dark : new boolean[]{false, true}) {
                        String context = fontSize + "pt, " + width + "px, dark=" + dark;
                        DescriptionRendering baseline = renderDescription(
                                uncompensatedDescription, fontSize, width, dark);
                        DescriptionRendering actual = renderDescription(description, fontSize, width, dark);
                        assertEquals(10.0, baseline.leadingGap(), 0.01,
                                "The full unstyled description must expose the host's remaining margin: " + context);
                        assertEquals(0.0, actual.leadingGap(), 0.01,
                                "Description must start immediately below the host heading: " + context);
                        // Align each crop with its own first-character model rectangle. Comparing painted
                        // pixels catches a negative margin that moves the text but clips its first line.
                        assertEquals(baseline.firstLineHeight(), actual.firstLineHeight(),
                                "Spacing compensation must preserve the first line's height: " + context);
                        assertArrayEquals(baseline.firstLinePixels(), actual.firstLinePixels(),
                                "Spacing compensation must paint the complete, unchanged first line: " + context);
                    }
                }
            }
        });
    }

    private static DescriptionRendering renderDescription(String description, int fontSize, int width, boolean dark) {
        JTextPane pane = new JTextPane();
        HTMLEditorKit kit = new HTMLEditorKit();
        StyleSheet css = new StyleSheet();
        css.addRule("body { font-family: Dialog; font-size: " + fontSize + "pt; }");
        css.addStyleSheet(kit.getStyleSheet());
        kit.setStyleSheet(css);
        pane.setEditorKit(kit);
        pane.putClientProperty(JTextPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        pane.setFont(new Font(Font.DIALOG, Font.PLAIN, fontSize));
        pane.setForeground(dark ? Color.LIGHT_GRAY : Color.BLACK);
        pane.setBackground(dark ? Color.DARK_GRAY : Color.WHITE);
        pane.setEditable(false);
        pane.setBorder(BorderFactory.createEmptyBorder(3, 3, 0, 0));
        pane.setText("<html><body><b>Version:</b> 0.1.3<br><br><h3>Plugin Description</h3>"
                + description.substring("<html>".length()));
        pane.setSize(width, 500);
        try {
            String text = pane.getDocument().getText(0, pane.getDocument().getLength());
            Rectangle2D heading = pane.modelToView2D(text.indexOf("Plugin Description"));
            Rectangle2D intro = pane.modelToView2D(text.indexOf("Adds Dart editing"));
            BufferedImage painted = new BufferedImage(width, pane.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = painted.createGraphics();
            try {
                pane.paint(graphics);
            } finally {
                graphics.dispose();
            }
            int firstLineY = (int) Math.floor(intro.getY());
            int firstLineHeight = (int) Math.ceil(intro.getMaxY()) - firstLineY;
            int[] firstLinePixels = painted.getRGB(0, firstLineY, width, firstLineHeight, null, 0, width);
            boolean hasPaintedText = false;
            for (int pixel : firstLinePixels) {
                if (pixel != pane.getBackground().getRGB()) {
                    hasPaintedText = true;
                    break;
                }
            }
            assertTrue(hasPaintedText, "The first intro line must contain visible painted text");
            return new DescriptionRendering(intro.getY() - heading.getMaxY(), firstLineHeight, firstLinePixels);
        } catch (BadLocationException exception) {
            throw new AssertionError("Cannot measure the rendered plugin description", exception);
        }
    }

    private record DescriptionRendering(double leadingGap, int firstLineHeight, int[] firstLinePixels) {

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

    @Test
    void packagedRuntimeContainsOnlyTheNewJavaAndResourceNamespace() throws Exception {
        Path nbm = requiredPath("nbm.file");
        try (ZipFile zip = new ZipFile(nbm.toFile())) {
            for (var entry : java.util.Collections.list(zip.entries())) {
                if (!entry.getName().endsWith(".jar")) {
                    continue;
                }
                try (ZipInputStream jar = new ZipInputStream(zip.getInputStream(entry))) {
                    ZipEntry nested;
                    while ((nested = jar.getNextEntry()) != null) {
                        assertFalse(nested.getName().startsWith("dev/flutter/netbeans/"),
                                entry.getName() + " contains a stale pre-migration entry: " + nested.getName());
                    }
                }
            }
        }
        assertNestedJarContains(nbm, MODULE_JAR, "namespace migration", Set.of(
                "io/github/vgrytsenko2022/plugin/settings/ModulePreferences.class",
                "io/github/vgrytsenko2022/plugin/project/ProjectPreferenceNamespaceMigration.class",
                "io/github/vgrytsenko2022/plugin/layer.xml",
                "io/github/vgrytsenko2022/plugin/Bundle.properties"));
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
                builder.setEntityResolver((publicId, systemId)
                        -> new InputSource(new StringReader("")));
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
