package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.CatalogDiagnostic;
import dev.flutter.netbeans.designer.catalog.CatalogDiagnosticCode;
import dev.flutter.netbeans.designer.codec.FdDecodeResult;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.generation.DartGenerationDiagnostic;
import dev.flutter.netbeans.designer.generation.DartGenerationDiagnosticCode;
import dev.flutter.netbeans.designer.generation.DartGenerationResult;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.source.DartManagedRegionHashing;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityResult;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityScanner;
import dev.flutter.netbeans.designer.source.DartThreeWayIntegrityGate;
import dev.flutter.netbeans.designer.validation.ValidationResult;
import java.awt.Component;
import java.awt.Container;
import java.beans.PropertyChangeEvent;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javax.accessibility.AccessibleContext;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.openide.util.Lookup;

class FlutterDesignerMultiViewDesignAccessibilityTest {

    @Test
    void namesTheDesignSurfaceAndAssociatesStatusWithLoadingProgress() throws Exception {
        onEdt(() -> {
            FlutterDesignerMultiViewDesign design =
                    new FlutterDesignerMultiViewDesign(Lookup.EMPTY);
            JPanel visual = (JPanel) design.getVisualRepresentation();
            JToolBar toolbar = (JToolBar) design.getToolbarRepresentation();
            JLabel status = findNamed(
                    visual, JLabel.class, "Flutter Designer status");
            JProgressBar progress = findNamed(
                    visual,
                    JProgressBar.class,
                    "Flutter Designer model loading progress");

            assertEquals("Flutter Designer design view",
                    visual.getAccessibleContext().getAccessibleName());
            assertEquals("Flutter Designer toolbar",
                    toolbar.getAccessibleContext().getAccessibleName());
            assertSame(progress, status.getLabelFor());
            assertFalse(progress.isVisible());
            assertNotNull(progress.getAccessibleContext().getAccessibleDescription());
        });
    }

    @Test
    void firstReopenedViewNeverPresentsARetainedCurrentState() throws Exception {
        FlutterDesignerDocumentState.Current retained = currentState(List.of());

        FlutterDesignerDocumentState first = FlutterDesignerMultiViewDesign.openingState(
                true, retained, "home_page.fd");
        FlutterDesignerDocumentState clone = FlutterDesignerMultiViewDesign.openingState(
                false, retained, "home_page.fd");

        FlutterDesignerDocumentState.Loading loading = assertInstanceOf(
                FlutterDesignerDocumentState.Loading.class, first);
        assertEquals("home_page.fd", loading.modelFileName());
        assertSame(retained, clone);
    }

    @Test
    void refreshesAccessibleDescriptionsWithEveryRenderedState() throws Exception {
        onEdt(() -> {
            FlutterDesignerMultiViewDesign design =
                    new FlutterDesignerMultiViewDesign(Lookup.EMPTY);
            JPanel visual = (JPanel) design.getVisualRepresentation();
            JProgressBar progress = findNamed(
                    visual,
                    JProgressBar.class,
                    "Flutter Designer model loading progress");

            publish(design, new FlutterDesignerDocumentState.Loading("sample.fd"));

            String loadingDetail =
                    "Reading bounded UTF-8 .fd and paired Dart snapshots, validating "
                    + "the model and source structure, generating both managed payloads "
                    + "in memory, and comparing actual, declared and generated SHA-256 "
                    + "values. No files are written.";
            assertTrue(progress.isVisible());
            assertEquals(loadingDetail,
                    progress.getAccessibleContext().getAccessibleDescription());
            assertEquals(
                    "Loading Flutter Designer model... Model: the .fd model. "
                    + "Source: the paired Dart source. "
                    + loadingDetail,
                    visual.getAccessibleContext().getAccessibleDescription());

            publish(design, new FlutterDesignerDocumentState.Failure(
                    "Read Flutter Designer model",
                    "sample.fd",
                    "The file is no longer available"));

            assertFalse(progress.isVisible());
            assertEquals(
                    "Read Flutter Designer model failed. Target: sample.fd. "
                    + "Reason: The file is no longer available",
                    visual.getAccessibleContext().getAccessibleDescription());
            assertEquals(
                    "Reason: The file is no longer available",
                    progress.getAccessibleContext().getAccessibleDescription());
        });
    }

    @Test
    void exposesConcreteAccessibleReadOnlyAndInputFailureStates() throws Exception {
        onEdt(() -> {
            FlutterDesignerMultiViewDesign design =
                    new FlutterDesignerMultiViewDesign(Lookup.EMPTY);
            JPanel visual = (JPanel) design.getVisualRepresentation();
            JLabel status = findNamed(
                    visual, JLabel.class, "Flutter Designer status");
            JLabel detail = findNamed(
                    visual, JLabel.class, "Flutter Designer status details");
            JProgressBar progress = findNamed(
                    visual,
                    JProgressBar.class,
                    "Flutter Designer model loading progress");
            FdDocumentCodec codec = new FdDocumentCodec();
            FlutterDesignerDocumentState.Current current = currentState(List.of());
            publish(design, current);
            assertEquals("Flutter Designer on-disk three-way match verified.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertEquals(
                    "The on-disk imports and build regions, the SHA-256 values "
                    + "recorded in the .fd model, and the deterministic generated "
                    + "payloads agree. The native Flutter Canvas host is installed, "
                    + "but validated model publication, Palette, tree, properties, "
                    + "selection, drag-and-drop and Designer mutation remain disabled "
                    + "until their staged pair-aware workflows are complete.",
                    detail.getAccessibleContext().getAccessibleDescription());
            assertFalse(progress.isVisible());

            publish(design, new FlutterDesignerDocumentState.Current(
                    current.decoded(),
                    current.validation(),
                    current.catalog(),
                    List.of(),
                    List.of(),
                    current.sourceIntegrity(),
                    Optional.empty()));
            assertEquals("Flutter Designer three-way comparison is unavailable.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertEquals(
                    "No bounded deterministic Dart-generation comparison is "
                    + "available for this loaded model.",
                    detail.getAccessibleContext().getAccessibleDescription());

            FdDecodeResult.Current driftDecoded = assertInstanceOf(
                    FdDecodeResult.Current.class,
                    codec.decode(validDocument()
                            .replace("flutter.widgets.SizedBox", "flutter.widgets.Center")
                            .getBytes(StandardCharsets.UTF_8)));
            var driftGeneration = new DartRegionGenerator().generate(
                    driftDecoded.document(), BuiltInWidgetCatalog.getDefault());
            var driftThreeWay = new DartThreeWayIntegrityGate().evaluate(
                    current.sourceIntegrity().orElseThrow(),
                    driftDecoded.document().source(),
                    driftGeneration);
            publish(design, new FlutterDesignerDocumentState.Current(
                    driftDecoded,
                    driftGeneration.modelValidation(),
                    current.catalog(),
                    List.of(),
                    List.of(),
                    current.sourceIntegrity(),
                    Optional.of(driftThreeWay)));
            assertEquals("Flutter Designer generated Dart conflict.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertTrue(detail.getAccessibleContext().getAccessibleDescription()
                    .contains("GENERATED_REGION_HASH_MISMATCH at "
                            + "/source/managedRegions/build/sha256; region build"));

            DartGenerationResult unsupportedGeneration = generationFailure(
                    current.validation(),
                    DartGenerationDiagnosticCode.DART_EXPRESSION_UNSUPPORTED,
                    "/root/properties/value",
                    "Opaque Dart expressions are not generated.");
            var unsupportedThreeWay = new DartThreeWayIntegrityGate().evaluate(
                    current.sourceIntegrity().orElseThrow(),
                    current.decoded().document().source(),
                    unsupportedGeneration);
            publish(design, new FlutterDesignerDocumentState.Current(
                    current.decoded(),
                    current.validation(),
                    current.catalog(),
                    List.of(),
                    List.of(),
                    current.sourceIntegrity(),
                    Optional.of(unsupportedThreeWay)));
            assertEquals("Flutter Designer Dart generation is unsupported.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertTrue(detail.getAccessibleContext().getAccessibleDescription()
                    .contains("DART_EXPRESSION_UNSUPPORTED at /root/properties/value"));

            DartGenerationResult unavailableGeneration = generationFailure(
                    current.validation(),
                    DartGenerationDiagnosticCode.OUTPUT_SIZE_LIMIT,
                    "/source/managedRegions",
                    "Generated payload exceeds the configured limit.");
            var unavailableThreeWay = new DartThreeWayIntegrityGate().evaluate(
                    current.sourceIntegrity().orElseThrow(),
                    current.decoded().document().source(),
                    unavailableGeneration);
            publish(design, new FlutterDesignerDocumentState.Current(
                    current.decoded(),
                    current.validation(),
                    current.catalog(),
                    List.of(),
                    List.of(),
                    current.sourceIntegrity(),
                    Optional.of(unavailableThreeWay)));
            assertEquals("Flutter Designer three-way comparison is unavailable.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertTrue(detail.getAccessibleContext().getAccessibleDescription()
                    .contains("OUTPUT_SIZE_LIMIT at /source/managedRegions"));

            DartSourceIntegrityResult conflict = new DartSourceIntegrityScanner().scan(
                    validDartSource().replace("const SizedBox", "const Text")
                            .getBytes(StandardCharsets.UTF_8),
                    current.decoded().document().source());
            publish(design, new FlutterDesignerDocumentState.Current(
                    current.decoded(),
                    current.validation(),
                    current.catalog(),
                    List.of(),
                    List.of(),
                    Optional.of(conflict),
                    Optional.empty()));
            assertEquals("Flutter Designer Dart source conflict.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertTrue(detail.getAccessibleContext().getAccessibleDescription()
                    .contains("REGION_HASH_MISMATCH at "
                            + "/source/managedRegions/build/sha256; region build"));
            assertFalse(progress.isVisible());

            FdDecodeResult.Current statefulDecoded = assertInstanceOf(
                    FdDecodeResult.Current.class,
                    codec.decode(validDocument()
                            .replace("\"stateless\"", "\"stateful\"")
                            .getBytes(StandardCharsets.UTF_8)));
            DartSourceIntegrityResult mixedUnsupported =
                    new DartSourceIntegrityScanner().scan(
                            validDartSource()
                                    .replace("StatelessWidget", "StatefulWidget")
                                    .replace("const SizedBox", "const Text")
                                    .getBytes(StandardCharsets.UTF_8),
                            statefulDecoded.document().source());
            publish(design, new FlutterDesignerDocumentState.Current(
                    statefulDecoded,
                    new ValidationResult(List.of()),
                    current.catalog(),
                    List.of(),
                    List.of(),
                    Optional.of(mixedUnsupported),
                    Optional.empty()));
            assertEquals("Flutter Designer Dart source shape is unsupported.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertTrue(detail.getAccessibleContext().getAccessibleDescription()
                    .contains("STATEFUL_SOURCE_BINDING_UNSUPPORTED at /source/widgetKind"));
            assertTrue(detail.getAccessibleContext().getAccessibleDescription()
                    .contains("Additional source diagnostics: REGION_HASH_MISMATCH at "
                            + "/source/managedRegions/build/sha256; region build"),
                    "known conflicts must remain visible behind the primary unsupported cause");

            publish(design, new FlutterDesignerDocumentState.Current(
                    current.decoded(),
                    current.validation(),
                    current.catalog(),
                    List.of(),
                    List.of(),
                    Optional.of(DartSourceIntegrityResult.readFailure("Access denied")),
                    Optional.empty()));
            assertEquals("Flutter Designer Dart source is unavailable.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertTrue(detail.getAccessibleContext().getAccessibleDescription()
                    .contains("SOURCE_READ_FAILED at /source/dartFile: "
                            + "The paired Dart source could not be read: Access denied"));

            CatalogDiagnostic catalogDiagnostic = new CatalogDiagnostic(
                    CatalogDiagnosticCode.INVALID_CONTRIBUTOR,
                    "com.example.widgets",
                    List.of("com.example"),
                    "The widget contributor could not be loaded.");
            publish(design, new FlutterDesignerDocumentState.Current(
                    current.decoded(),
                    current.validation(),
                    current.catalog(),
                    List.of(catalogDiagnostic),
                    List.of(),
                    current.sourceIntegrity(),
                    current.threeWayIntegrity()));
            assertEquals("Flutter Designer model opened read-only.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertEquals(
                    "INVALID_CONTRIBUTOR for com.example.widgets: "
                    + "The widget contributor could not be loaded.",
                    detail.getAccessibleContext().getAccessibleDescription());
            assertFalse(progress.isVisible());

            FdDecodeResult.UnsupportedNewer future = assertInstanceOf(
                    FdDecodeResult.UnsupportedNewer.class,
                    codec.decode(("{\"format\":\"netbeans-flutter-designer\","
                            + "\"schemaVersion\":2}")
                            .getBytes(StandardCharsets.UTF_8)));
            publish(design, new FlutterDesignerDocumentState.UnsupportedNewer(future));
            assertEquals("Flutter Designer model opened read-only.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertEquals("Schema version 2 is newer than supported version 1.",
                    detail.getAccessibleContext().getAccessibleDescription());
            assertFalse(progress.isVisible());

            FdDecodeResult.Invalid invalid = assertInstanceOf(
                    FdDecodeResult.Invalid.class,
                    codec.decode("{".getBytes(StandardCharsets.UTF_8)));
            publish(design, new FlutterDesignerDocumentState.Invalid(invalid));
            assertEquals("Cannot load Flutter Designer model.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertTrue(detail.getAccessibleContext().getAccessibleDescription()
                    .contains(invalid.diagnostics().get(0).code().toString()));
            assertFalse(progress.isVisible());

            publish(design, new FlutterDesignerDocumentState.InputTooLarge(
                    "sample.fd", 257, 256));
            assertEquals("Cannot read Flutter Designer model.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertEquals("File size 257 bytes exceeds the 256 byte safety limit.",
                    detail.getAccessibleContext().getAccessibleDescription());
            assertFalse(progress.isVisible());
        });
    }

    private static String validDocument() {
        return """
                {
                  "format": "netbeans-flutter-designer",
                  "schemaVersion": 1,
                  "documentId": "2f04ce87-876a-4f35-8a7c-2fba3e135c7e",
                  "source": {
                    "dartFile": "home_page.dart",
                    "className": "HomePage",
                    "widgetKind": "stateless",
                    "managedRegions": {
                      "imports": {
                        "sha256": "%s"
                      },
                      "build": {
                        "sha256": "%s"
                      }
                    }
                  },
                  "root": {
                    "id": "35ca8ca5-c5ec-4fe1-8982-dfc036e3c6ce",
                    "type": "flutter.widgets.SizedBox",
                    "properties": {},
                    "slots": {}
                  }
                }
                """.formatted(
                DartManagedRegionHashing.normalizedSha256(importsPayload()),
                DartManagedRegionHashing.normalizedSha256(buildPayload()));
    }

    private static FlutterDesignerDocumentState.Current currentState(
            List<CatalogDiagnostic> catalogDiagnostics) throws Exception {
        FdDecodeResult.Current decodedCurrent = assertInstanceOf(
                FdDecodeResult.Current.class,
                new FdDocumentCodec().decode(
                        validDocument().getBytes(StandardCharsets.UTF_8)));
        DartSourceIntegrityScanner sourceScanner = new DartSourceIntegrityScanner();
        DartSourceIntegrityResult sourceIntegrity = sourceScanner.scan(
                validDartSource().getBytes(StandardCharsets.UTF_8),
                decodedCurrent.document().source());
        var generation = new DartRegionGenerator().generate(
                decodedCurrent.document(), BuiltInWidgetCatalog.getDefault());
        var threeWayIntegrity = new DartThreeWayIntegrityGate(sourceScanner).evaluate(
                sourceIntegrity, decodedCurrent.document().source(), generation);
        return new FlutterDesignerDocumentState.Current(
                decodedCurrent,
                new ValidationResult(List.of()),
                BuiltInWidgetCatalog.getDefault(),
                catalogDiagnostics,
                List.of(),
                Optional.of(sourceIntegrity),
                Optional.of(threeWayIntegrity));
    }

    private static String validDartSource() {
        return "// <netbeans-flutter-designer region=\"imports\">\n"
                + importsPayload()
                + "// </netbeans-flutter-designer>\n\n"
                + "class HomePage extends StatelessWidget {\n"
                + "  // <netbeans-flutter-designer region=\"build\">\n"
                + buildPayload()
                + "  // </netbeans-flutter-designer>\n"
                + "}\n";
    }

    private static DartGenerationResult generationFailure(
            ValidationResult validation,
            DartGenerationDiagnosticCode code,
            String path,
            String message) {
        return new DartGenerationResult(
                validation,
                Optional.empty(),
                List.of(new DartGenerationDiagnostic(
                        code,
                        path,
                        Optional.empty(),
                        Optional.empty(),
                        message)));
    }

    private static String importsPayload() {
        return "import 'package:flutter/widgets.dart';\n";
    }

    private static String buildPayload() {
        return "  @override\n"
                + "  Widget build(BuildContext context) {\n"
                + "    return const SizedBox();\n"
                + "  }\n";
    }

    private static void publish(
            FlutterDesignerMultiViewDesign design,
            FlutterDesignerDocumentState state) {
        design.propertyChange(new PropertyChangeEvent(
                design,
                FlutterDesignerDocumentController.PROP_STATE,
                null,
                state));
    }

    private static <T extends Component> T findNamed(
            Container root,
            Class<T> type,
            String accessibleName) {
        for (Component component : root.getComponents()) {
            AccessibleContext context = component.getAccessibleContext();
            if (type.isInstance(component)
                    && context != null
                    && accessibleName.equals(context.getAccessibleName())) {
                return type.cast(component);
            }
            if (component instanceof Container child) {
                T found = findNamedOrNull(child, type, accessibleName);
                if (found != null) {
                    return found;
                }
            }
        }
        throw new AssertionError("No " + type.getSimpleName()
                + " named '" + accessibleName + "'");
    }

    private static <T extends Component> T findNamedOrNull(
            Container root,
            Class<T> type,
            String accessibleName) {
        for (Component component : root.getComponents()) {
            AccessibleContext context = component.getAccessibleContext();
            if (type.isInstance(component)
                    && context != null
                    && accessibleName.equals(context.getAccessibleName())) {
                return type.cast(component);
            }
            if (component instanceof Container child) {
                T found = findNamedOrNull(child, type, accessibleName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void onEdt(ThrowingRunnable runnable) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                runnable.run();
            } catch (Throwable thrown) {
                failure.set(thrown);
            }
        });
        if (failure.get() != null) {
            throw new AssertionError("EDT assertion failed", failure.get());
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
