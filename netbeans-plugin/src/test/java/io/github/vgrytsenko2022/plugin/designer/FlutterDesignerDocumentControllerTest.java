package io.github.vgrytsenko2022.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vgrytsenko2022.designer.catalog.BuiltInWidgetCatalog;
import io.github.vgrytsenko2022.designer.catalog.CatalogBuildResult;
import io.github.vgrytsenko2022.designer.catalog.CatalogDiagnosticCode;
import io.github.vgrytsenko2022.designer.catalog.PaletteMetadata;
import io.github.vgrytsenko2022.designer.catalog.WidgetCatalog;
import io.github.vgrytsenko2022.designer.catalog.WidgetCatalogComposition;
import io.github.vgrytsenko2022.designer.catalog.WidgetCatalogContributor;
import io.github.vgrytsenko2022.designer.catalog.WidgetDefinition;
import io.github.vgrytsenko2022.designer.codec.FdCodecLimits;
import io.github.vgrytsenko2022.designer.codec.FdDocumentCodec;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import io.github.vgrytsenko2022.designer.source.DartManagedRegionHashing;
import io.github.vgrytsenko2022.designer.source.DartSourceIntegrityDiagnosticCode;
import io.github.vgrytsenko2022.designer.source.DartSourceIntegrityLimits;
import io.github.vgrytsenko2022.designer.source.DartSourceIntegrityScanner;
import io.github.vgrytsenko2022.designer.source.DartThreeWayIntegrityStatus;
import io.github.vgrytsenko2022.designer.source.DartThreeWayIntegrityDiagnosticCode;
import io.github.vgrytsenko2022.designer.validation.WidgetTreeValidator;
import java.awt.EventQueue;
import java.beans.PropertyChangeEvent;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;

class FlutterDesignerDocumentControllerTest {

    @Test
    void loadsCurrentModelWithoutChangingItsExactByteBaseline() throws Exception {
        Pair pair = pair("home_page", validDocument(
                "home_page.dart", "flutter.widgets.SizedBox"));

        FlutterDesignerDocumentState.Current current = assertInstanceOf(
                FlutterDesignerDocumentState.Current.class,
                load(pair));

        assertTrue(current.validation().valid());
        assertTrue(current.catalogDiagnostics().isEmpty());
        assertTrue(current.contextIssues().isEmpty());
        assertTrue(current.sourceIntegrity().orElseThrow().onDiskDeclaredMatch(),
                () -> current.sourceIntegrity().orElseThrow().diagnostics().toString());
        assertTrue(current.threeWayIntegrity().orElseThrow().onDiskThreeWayMatch(),
                () -> current.threeWayIntegrity().orElseThrow().diagnostics().toString());
        assertEquals("HomePage", current.decoded().document().source().className());
        assertEquals("flutter.widgets.SizedBox",
                current.decoded().document().root().type().value());
        assertArrayEquals(
                pair.modelText().getBytes(StandardCharsets.UTF_8),
                current.decoded().original().copyBytes(),
                "opening Design must retain the exact .fd save baseline");
        assertArrayEquals(
                pair.dartText().getBytes(StandardCharsets.UTF_8),
                current.sourceIntegrity().orElseThrow().original().orElseThrow().copyBytes(),
                "opening Design must retain the exact Dart source baseline");
    }

    @Test
    void keepsCodecValidPairMismatchReadOnlyWithExactContextIssue() throws Exception {
        Pair pair = pair("home_page", validDocument(
                "another_page.dart", "flutter.widgets.SizedBox"));

        FlutterDesignerDocumentState.Current current = assertInstanceOf(
                FlutterDesignerDocumentState.Current.class,
                load(pair));

        assertTrue(current.validation().valid());
        assertEquals(1, current.contextIssues().size());
        assertEquals(
                FlutterDesignerDocumentController.SOURCE_FILE_MISMATCH,
                current.contextIssues().get(0).code());
        assertEquals("/source/dartFile", current.contextIssues().get(0).path());
        assertTrue(current.contextIssues().get(0).message().contains("another_page.dart"));
        assertTrue(current.contextIssues().get(0).message().contains("home_page.dart"));
        assertTrue(current.sourceIntegrity().isEmpty(),
                "a filename mismatch must not scan the wrong Dart file");
        assertTrue(current.threeWayIntegrity().isEmpty(),
                "a filename mismatch must not publish three-way evidence");
    }

    @Test
    void reportsManagedDartPayloadChangesWithoutWritingEitherFile() throws Exception {
        Pair pair = pair("home_page", validDocument(
                "home_page.dart", "flutter.widgets.SizedBox"));
        String changedDart = pair.dartText().replace(
                "return const SizedBox();", "return const Text('external');");
        write(pair.dartFile(), changedDart);

        FlutterDesignerDocumentState.Current current = assertInstanceOf(
                FlutterDesignerDocumentState.Current.class,
                load(pair));

        assertFalse(current.sourceIntegrity().orElseThrow().onDiskDeclaredMatch());
        assertEquals(DartThreeWayIntegrityStatus.CONFLICT,
                current.threeWayIntegrity().orElseThrow().status());
        assertEquals(
                DartSourceIntegrityDiagnosticCode.REGION_HASH_MISMATCH,
                current.sourceIntegrity().orElseThrow().diagnostics().getFirst().code());
        assertEquals(
                "/source/managedRegions/build/sha256",
                current.sourceIntegrity().orElseThrow().diagnostics().getFirst().path());
        assertArrayEquals(changedDart.getBytes(StandardCharsets.UTF_8),
                current.sourceIntegrity().orElseThrow().original().orElseThrow().copyBytes());
        assertArrayEquals(pair.modelText().getBytes(StandardCharsets.UTF_8),
                pair.modelFile().asBytes());
        assertArrayEquals(changedDart.getBytes(StandardCharsets.UTF_8),
                pair.dartFile().asBytes());
    }

    @Test
    void reportsModelGeneratedDriftEvenWhenOnDiskSourceMatchesDeclaredHashes()
            throws Exception {
        Pair pair = pair("home_page", validDocument(
                "home_page.dart", "flutter.widgets.Center"));

        FlutterDesignerDocumentState.Current current = assertInstanceOf(
                FlutterDesignerDocumentState.Current.class,
                load(pair));

        assertTrue(current.validation().valid());
        assertTrue(current.sourceIntegrity().orElseThrow().onDiskDeclaredMatch());
        assertEquals(DartThreeWayIntegrityStatus.CONFLICT,
                current.threeWayIntegrity().orElseThrow().status());
        assertTrue(current.threeWayIntegrity().orElseThrow().diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code()
                        == DartThreeWayIntegrityDiagnosticCode.GENERATED_REGION_HASH_MISMATCH
                        && diagnostic.regionId().orElse("").equals("build")));
        assertArrayEquals(pair.modelText().getBytes(StandardCharsets.UTF_8),
                pair.modelFile().asBytes());
        assertArrayEquals(pair.dartText().getBytes(StandardCharsets.UTF_8),
                pair.dartFile().asBytes());
    }

    @Test
    void keepsCurrentModelWhenThePairedDartSourceExceedsItsIndependentLimit()
            throws Exception {
        Pair pair = pair("home_page", validDocument(
                "home_page.dart", "flutter.widgets.SizedBox"));
        DartSourceIntegrityScanner sourceScanner = new DartSourceIntegrityScanner(
                new DartSourceIntegrityLimits(64, 16, 16));

        FlutterDesignerDocumentState.Current current = assertInstanceOf(
                FlutterDesignerDocumentState.Current.class,
                FlutterDesignerDocumentController.load(
                        pair.dartFile(),
                        pair.modelFile(),
                        new FdDocumentCodec(),
                        new WidgetTreeValidator(),
                        new CatalogBuildResult(BuiltInWidgetCatalog.getDefault(), List.of()),
                        sourceScanner));

        assertTrue(current.validation().valid());
        assertFalse(current.sourceIntegrity().orElseThrow().onDiskDeclaredMatch());
        assertTrue(current.sourceIntegrity().orElseThrow().original().isEmpty());
        assertEquals(
                DartSourceIntegrityDiagnosticCode.SOURCE_TOO_LARGE,
                current.sourceIntegrity().orElseThrow().diagnostics().getFirst().code());
        assertEquals(DartThreeWayIntegrityStatus.UNAVAILABLE,
                current.threeWayIntegrity().orElseThrow().status());
    }

    @Test
    void keepsTheDecodedModelCurrentWhenThePairedDartSourceCannotBeRead(
            @TempDir Path directory)
            throws Exception {
        Path dartPath = directory.resolve("home_page.dart");
        Path modelPath = directory.resolve("home_page.fd");
        String modelText = validDocument(
                "home_page.dart", "flutter.widgets.SizedBox");
        Files.writeString(dartPath, validDartSource(), StandardCharsets.UTF_8);
        Files.writeString(modelPath, modelText, StandardCharsets.UTF_8);
        FileObject dartFile = FileUtil.toFileObject(dartPath.toFile());
        FileObject modelFile = FileUtil.toFileObject(modelPath.toFile());
        assertTrue(dartFile != null && modelFile != null);
        Files.delete(dartPath);
        Pair pair = new Pair(dartFile, modelFile, modelText, validDartSource());

        FlutterDesignerDocumentState.Current current = assertInstanceOf(
                FlutterDesignerDocumentState.Current.class,
                load(pair));

        assertTrue(current.validation().valid());
        assertEquals(
                DartSourceIntegrityDiagnosticCode.SOURCE_READ_FAILED,
                current.sourceIntegrity().orElseThrow().primaryDiagnostic()
                        .orElseThrow().code());
        assertTrue(current.sourceIntegrity().orElseThrow().original().isEmpty());
        assertEquals(DartThreeWayIntegrityStatus.UNAVAILABLE,
                current.threeWayIntegrity().orElseThrow().status());
        assertArrayEquals(pair.modelText().getBytes(StandardCharsets.UTF_8),
                pair.modelFile().asBytes(),
                "a paired-source read failure must not discard or rewrite the decoded model");
    }

    @Test
    void separatesCatalogValidationFromCodecValidity() throws Exception {
        Pair pair = pair("home_page", validDocument(
                "home_page.dart", "example.widgets.Unknown"));

        FlutterDesignerDocumentState.Current current = assertInstanceOf(
                FlutterDesignerDocumentState.Current.class,
                load(pair));

        assertFalse(current.validation().valid());
        assertEquals(
                WidgetTreeValidator.UNKNOWN_WIDGET_TYPE,
                current.validation().errors().get(0).code());
    }

    @Test
    void validatesWithComposedExtensionCatalogAndSurfacesRejectedContributor()
            throws Exception {
        String extensionType = "com.example.ExampleCard";
        Pair pair = pair("home_page", validDocument(
                "home_page.dart", extensionType));
        WidgetCatalogContributor valid = contributor(
                "com.example", definition(extensionType));
        WidgetCatalogContributor broken = new WidgetCatalogContributor() {
            @Override
            public String contributorId() {
                return "com.broken";
            }

            @Override
            public int apiVersion() {
                return WidgetCatalog.API_VERSION;
            }

            @Override
            public Collection<WidgetDefinition> definitions() {
                throw new IllegalStateException("environment-specific failure");
            }
        };
        CatalogBuildResult catalogBuild = WidgetCatalogComposition.compose(
                BuiltInWidgetCatalog.getDefault(), List.of(valid, broken));

        FlutterDesignerDocumentState.Current current = assertInstanceOf(
                FlutterDesignerDocumentState.Current.class,
                FlutterDesignerDocumentController.load(
                        pair.dartFile(),
                        pair.modelFile(),
                        new FdDocumentCodec(),
                        new WidgetTreeValidator(),
                        catalogBuild));

        assertTrue(current.validation().valid(),
                "the extension widget must be validated by the composed snapshot");
        assertEquals(1, current.catalogDiagnostics().size());
        assertEquals(CatalogDiagnosticCode.INVALID_DEFINITION,
                current.catalogDiagnostics().get(0).code());
        assertEquals("com.broken", current.catalogDiagnostics().get(0).subject());
    }

    @Test
    void exposesFutureSchemaOnlyAsReadOnlyRawResult() throws Exception {
        int futureVersion = DesignerDocument.SCHEMA_VERSION + 1;
        String future = """
                {"format":"netbeans-flutter-designer","schemaVersion":%d,"future":true}
                """.formatted(futureVersion);
        Pair pair = pair("home_page", future);

        FlutterDesignerDocumentState.UnsupportedNewer newer = assertInstanceOf(
                FlutterDesignerDocumentState.UnsupportedNewer.class,
                load(pair));

        assertEquals(Integer.toString(futureVersion), newer.decoded().declaredSchemaVersion().toString());
        assertArrayEquals(
                future.getBytes(StandardCharsets.UTF_8),
                newer.decoded().original().copyBytes());
    }

    @Test
    void exposesStrictCodecDiagnosticsForInvalidCurrentDocument() throws Exception {
        Pair pair = pair("home_page", """
                {
                  "format": "netbeans-flutter-designer",
                  "schemaVersion": 1
                }
                """);

        FlutterDesignerDocumentState.Invalid invalid = assertInstanceOf(
                FlutterDesignerDocumentState.Invalid.class,
                load(pair));

        assertFalse(invalid.decoded().diagnostics().isEmpty());
        assertArrayEquals(
                pair.modelText().getBytes(StandardCharsets.UTF_8),
                invalid.decoded().original().copyBytes());
    }

    @Test
    void rejectsOversizedInputBeforeCodecSnapshotAllocation() throws Exception {
        Pair pair = pair("home_page", "x".repeat(257));
        FdCodecLimits limits = withMaximumDocumentBytes(256);

        FlutterDesignerDocumentState.InputTooLarge tooLarge = assertInstanceOf(
                FlutterDesignerDocumentState.InputTooLarge.class,
                FlutterDesignerDocumentController.load(
                        pair.dartFile(),
                        pair.modelFile(),
                        new FdDocumentCodec(limits),
                        new WidgetTreeValidator(),
                        BuiltInWidgetCatalog.getDefault()));

        assertEquals(257, tooLarge.observedBytes());
        assertEquals(256, tooLarge.maximumBytes());
        assertEquals("home_page.fd", tooLarge.modelFileName());
    }

    @Test
    void publishesBackgroundLifecycleChangesOnTheEventDispatchThread()
            throws Exception {
        Pair pair = pair("home_page", validDocument(
                "home_page.dart", "flutter.widgets.SizedBox"));
        FlutterDesignerDocumentController controller = controller(pair);
        CountDownLatch currentPublished = new CountDownLatch(1);
        AtomicBoolean callbacksOnEdt = new AtomicBoolean(true);

        controller.addPropertyChangeListener(event -> {
            callbacksOnEdt.compareAndSet(true, EventQueue.isDispatchThread());
            if (event.getNewValue() instanceof FlutterDesignerDocumentState.Current) {
                currentPublished.countDown();
            }
        });
        controller.viewOpened();

        assertTrue(currentPublished.await(5, TimeUnit.SECONDS));
        assertTrue(callbacksOnEdt.get());
        assertInstanceOf(FlutterDesignerDocumentState.Current.class, controller.state());
    }

    @Test
    void exactCurrentAdoptionIsCallbackFreeAndPublishesOnceOnEdt()
            throws Exception {
        Pair pair = pair("adopt_current", validDocument(
                "adopt_current.dart", "flutter.widgets.SizedBox"));
        Pair savedPair = pair("adopt_current", validDocument(
                "adopt_current.dart", "flutter.widgets.Center"));
        FlutterDesignerDocumentController controller = controller(pair);
        CountDownLatch initialPublished = new CountDownLatch(1);
        controller.addPropertyChangeListener(event -> {
            if (event.getNewValue() instanceof FlutterDesignerDocumentState.Current) {
                initialPublished.countDown();
            }
        });
        controller.viewOpened();
        assertTrue(initialPublished.await(5, TimeUnit.SECONDS));

        FlutterDesignerDocumentState.Current expected = assertInstanceOf(
                FlutterDesignerDocumentState.Current.class,
                controller.state());
        FlutterDesignerDocumentState.Current saved = assertInstanceOf(
                FlutterDesignerDocumentState.Current.class,
                load(savedPair));
        FlutterDesignerDocumentController.CurrentAdoptionTicket ticket =
                controller.currentAdoptionTicket(expected);
        CountDownLatch adoptionPublished = new CountDownLatch(1);
        AtomicInteger adoptionCallbacks = new AtomicInteger();
        AtomicBoolean callbackOnEdt = new AtomicBoolean();
        AtomicReference<PropertyChangeEvent> exactEvent = new AtomicReference<>();
        controller.addPropertyChangeListener(event -> {
            if (event.getNewValue() == saved) {
                adoptionCallbacks.incrementAndGet();
                callbackOnEdt.set(EventQueue.isDispatchThread());
                exactEvent.set(event);
                adoptionPublished.countDown();
            }
        });

        FlutterDesignerDocumentController.DeferredCurrentEffects effects =
                controller.adoptCurrentDeferred(ticket, saved);

        assertSame(saved, controller.state());
        assertEquals(0, adoptionCallbacks.get(),
                "monitor-only adoption must not run property listeners");
        effects.publish();
        effects.publish();
        assertTrue(adoptionPublished.await(5, TimeUnit.SECONDS));
        EventQueue.invokeAndWait(() -> { });

        assertEquals(1, adoptionCallbacks.get());
        assertTrue(callbackOnEdt.get());
        assertEquals(FlutterDesignerDocumentController.PROP_STATE,
                exactEvent.get().getPropertyName());
        assertSame(expected, exactEvent.get().getOldValue());
        assertSame(saved, exactEvent.get().getNewValue());
        controller.viewClosed();
    }

    @Test
    void adoptionRejectsATicketAfterACompetingReloadRequest()
            throws Exception {
        Pair pair = pair("stale_adoption", validDocument(
                "stale_adoption.dart", "flutter.widgets.SizedBox"));
        Pair savedPair = pair("stale_adoption", validDocument(
                "stale_adoption.dart", "flutter.widgets.Center"));
        FlutterDesignerDocumentController controller = controller(pair);
        CountDownLatch initialPublished = new CountDownLatch(1);
        controller.addPropertyChangeListener(event -> {
            if (event.getNewValue() instanceof FlutterDesignerDocumentState.Current) {
                initialPublished.countDown();
            }
        });
        controller.viewOpened();
        assertTrue(initialPublished.await(5, TimeUnit.SECONDS));
        FlutterDesignerDocumentState.Current expected = assertInstanceOf(
                FlutterDesignerDocumentState.Current.class,
                controller.state());
        FlutterDesignerDocumentState.Current saved = assertInstanceOf(
                FlutterDesignerDocumentState.Current.class,
                load(savedPair));
        FlutterDesignerDocumentController.CurrentAdoptionTicket ticket =
                controller.currentAdoptionTicket(expected);

        controller.reload();

        assertThrows(IllegalStateException.class,
                () -> controller.adoptCurrentDeferred(ticket, saved));
        assertFalse(controller.state() == saved,
                "a stale generation ticket must perform no state mutation");
        controller.viewClosed();
    }

    @Test
    void openViewAdoptionTicketRejectsAClosedViewEvenWhenCurrentIsRetained()
            throws Exception {
        Pair pair = pair("closed_view_adoption", validDocument(
                "closed_view_adoption.dart", "flutter.widgets.SizedBox"));
        FlutterDesignerDocumentController controller = controller(pair);
        CountDownLatch initialPublished = new CountDownLatch(1);
        controller.addPropertyChangeListener(event -> {
            if (event.getNewValue() instanceof FlutterDesignerDocumentState.Current) {
                initialPublished.countDown();
            }
        });
        controller.viewOpened();
        assertTrue(initialPublished.await(5, TimeUnit.SECONDS));
        FlutterDesignerDocumentState.Current retained = assertInstanceOf(
                FlutterDesignerDocumentState.Current.class,
                controller.state());

        controller.viewClosed();

        assertSame(retained, controller.state(),
                "closing the view retains presentation state only");
        assertThrows(IllegalStateException.class,
                () -> controller.openViewCurrentAdoptionTicket(retained));
    }

    @Test
    void lastViewCloseInvalidatesAnOpenViewAdoptionTicket()
            throws Exception {
        Pair pair = pair("last_view_adoption", validDocument(
                "last_view_adoption.dart", "flutter.widgets.SizedBox"));
        Pair savedPair = pair("last_view_adoption", validDocument(
                "last_view_adoption.dart", "flutter.widgets.Center"));
        FlutterDesignerDocumentController controller = controller(pair);
        CountDownLatch initialPublished = new CountDownLatch(1);
        controller.addPropertyChangeListener(event -> {
            if (event.getNewValue() instanceof FlutterDesignerDocumentState.Current) {
                initialPublished.countDown();
            }
        });
        controller.viewOpened();
        assertTrue(initialPublished.await(5, TimeUnit.SECONDS));
        FlutterDesignerDocumentState.Current expected = assertInstanceOf(
                FlutterDesignerDocumentState.Current.class,
                controller.state());
        FlutterDesignerDocumentState.Current saved = assertInstanceOf(
                FlutterDesignerDocumentState.Current.class,
                load(savedPair));
        FlutterDesignerDocumentController.CurrentAdoptionTicket ticket =
                controller.openViewCurrentAdoptionTicket(expected);

        controller.viewClosed();

        assertThrows(IllegalStateException.class,
                () -> controller.adoptCurrentDeferred(ticket, saved));
        assertSame(expected, controller.state(),
                "a ticket invalidated by the last close must not mutate state");
    }

    @Test
    void adoptedCurrentCannotBeOverwrittenByWorkerThatPassedAdvisoryChecks()
            throws Exception {
        Pair pair = pair("adoption_worker_race", validDocument(
                "adoption_worker_race.dart", "flutter.widgets.SizedBox"));
        Pair savedPair = pair("adoption_worker_race", validDocument(
                "adoption_worker_race.dart", "flutter.widgets.Center"));
        FlutterDesignerDocumentState.Current initial = assertInstanceOf(
                FlutterDesignerDocumentState.Current.class, load(pair));
        FlutterDesignerDocumentState.Current saved = assertInstanceOf(
                FlutterDesignerDocumentState.Current.class, load(savedPair));
        FlutterDesignerDocumentState stale = new FlutterDesignerDocumentState.Failure(
                "Load stale model", pair.modelFile().getPath(),
                "Superseded by exact Current adoption.");
        CountDownLatch secondLoadStarted = new CountDownLatch(1);
        CountDownLatch releaseSecondLoad = new CountDownLatch(1);
        CountDownLatch secondLoadReturned = new CountDownLatch(1);
        AtomicInteger loads = new AtomicInteger();
        FlutterDesignerDocumentController controller =
                new FlutterDesignerDocumentController(
                        pair.dartFile(), pair.modelFile(), () -> {
                            if (loads.incrementAndGet() == 1) {
                                return initial;
                            }
                            secondLoadStarted.countDown();
                            awaitUninterruptibly(releaseSecondLoad);
                            secondLoadReturned.countDown();
                            return stale;
                        });
        CountDownLatch initialPublished = new CountDownLatch(1);
        AtomicInteger adoptionCallbacks = new AtomicInteger();
        AtomicInteger staleCallbacks = new AtomicInteger();
        controller.addPropertyChangeListener(event -> {
            if (event.getNewValue() == initial) {
                initialPublished.countDown();
            } else if (event.getNewValue() == saved) {
                adoptionCallbacks.incrementAndGet();
            } else if (event.getNewValue() == stale) {
                staleCallbacks.incrementAndGet();
            }
        });
        controller.viewOpened();
        assertTrue(initialPublished.await(5, TimeUnit.SECONDS));

        EventQueue.invokeAndWait(() -> {
            controller.reload();
            assertTrue(await(secondLoadStarted, 5, TimeUnit.SECONDS),
                    "the worker must pass its advisory checks and enter the stale load");
            FlutterDesignerDocumentController.CurrentAdoptionTicket ticket =
                    controller.currentAdoptionTicket(initial);
            FlutterDesignerDocumentController.DeferredCurrentEffects effects =
                    controller.adoptCurrentDeferred(ticket, saved);
            assertSame(saved, controller.state());
            effects.publish();
        });
        releaseSecondLoad.countDown();
        assertTrue(secondLoadReturned.await(5, TimeUnit.SECONDS));
        EventQueue.invokeAndWait(() -> { });

        assertSame(saved, controller.state());
        assertEquals(1, adoptionCallbacks.get());
        assertEquals(0, staleCallbacks.get());
        assertEquals(2, loads.get(),
                "adoption invalidation must not request an unsolicited follow-up load");
        controller.viewClosed();
    }

    @Test
    void reloadsAfterAnExternalModelChange() throws Exception {
        Pair pair = pair("home_page", validDocument(
                "home_page.dart", "flutter.widgets.SizedBox"));
        FlutterDesignerDocumentController controller = controller(pair);
        CountDownLatch currentPublished = new CountDownLatch(1);
        CountDownLatch futurePublished = new CountDownLatch(1);
        controller.addPropertyChangeListener(event -> {
            if (event.getNewValue() instanceof FlutterDesignerDocumentState.Current) {
                currentPublished.countDown();
            } else if (event.getNewValue()
                    instanceof FlutterDesignerDocumentState.UnsupportedNewer) {
                futurePublished.countDown();
            }
        });
        controller.viewOpened();
        assertTrue(currentPublished.await(5, TimeUnit.SECONDS));

        write(pair.modelFile(), """
                {"format":"netbeans-flutter-designer","schemaVersion":%d}
                """.formatted(DesignerDocument.SCHEMA_VERSION + 1));

        assertTrue(futurePublished.await(5, TimeUnit.SECONDS));
        assertInstanceOf(
                FlutterDesignerDocumentState.UnsupportedNewer.class,
                controller.state());
        controller.viewClosed();
    }

    @Test
    void reloadsAfterAnExternalDartChangeAndPublishesTheSourceConflict()
            throws Exception {
        Pair pair = pair("home_page", validDocument(
                "home_page.dart", "flutter.widgets.SizedBox"));
        FlutterDesignerDocumentController controller = controller(pair);
        CountDownLatch initial = new CountDownLatch(1);
        CountDownLatch changed = new CountDownLatch(1);
        AtomicInteger currentStates = new AtomicInteger();
        controller.addPropertyChangeListener(event -> {
            if (event.getNewValue() instanceof FlutterDesignerDocumentState.Current) {
                if (currentStates.incrementAndGet() == 1) {
                    initial.countDown();
                } else {
                    changed.countDown();
                }
            }
        });
        controller.viewOpened();
        assertTrue(initial.await(5, TimeUnit.SECONDS));

        write(pair.dartFile(), pair.dartText().replace(
                "return const SizedBox();", "return const Text('external');"));

        assertTrue(changed.await(5, TimeUnit.SECONDS));
        FlutterDesignerDocumentState.Current state = assertInstanceOf(
                FlutterDesignerDocumentState.Current.class, controller.state());
        assertEquals(
                DartSourceIntegrityDiagnosticCode.REGION_HASH_MISMATCH,
                state.sourceIntegrity().orElseThrow().diagnostics().getFirst().code());
        controller.viewClosed();
    }

    @Test
    void countsClonedViewsAndStopsWorkAfterTheLastViewCloses() throws Exception {
        Pair pair = pair("home_page", validDocument(
                "home_page.dart", "flutter.widgets.SizedBox"));
        AtomicInteger loads = new AtomicInteger();
        CountDownLatch thirdLoad = new CountDownLatch(1);
        FlutterDesignerDocumentController controller =
                new FlutterDesignerDocumentController(
                        pair.dartFile(),
                        pair.modelFile(),
                        () -> {
                            int invocation = loads.incrementAndGet();
                            if (invocation == 3) {
                                thirdLoad.countDown();
                            }
                            return load(pair);
                        });

        assertTrue(controller.viewOpened());
        assertFalse(controller.viewOpened());
        awaitLoadCount(loads, 1);

        controller.viewClosed();
        write(pair.modelFile(), """
                {"format":"netbeans-flutter-designer","schemaVersion":2}
                """);
        awaitLoadCount(loads, 2);

        controller.viewClosed();
        write(pair.dartFile(), pair.dartText().replace(
                "return const SizedBox();", "return const Text('closed');"));
        write(pair.modelFile(), validDocument(
                "home_page.dart", "flutter.widgets.SizedBox"));
        assertFalse(thirdLoad.await(500, TimeUnit.MILLISECONDS),
                "a model change after the last close must not start a load");
        assertEquals(2, loads.get(), "a closed Design session must not schedule work");

        assertTrue(controller.viewOpened());
        assertTrue(thirdLoad.await(5, TimeUnit.SECONDS));
        assertEquals(3, loads.get(), "reopening must perform one fresh load");
        controller.viewClosed();
    }

    @Test
    void coalescesReloadStormAndPublishesOnlyTheNewestLoad() throws Exception {
        Pair pair = pair("home_page", validDocument(
                "home_page.dart", "flutter.widgets.SizedBox"));
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch currentPublished = new CountDownLatch(1);
        AtomicInteger loads = new AtomicInteger();
        AtomicInteger staleFailuresPublished = new AtomicInteger();
        FlutterDesignerDocumentState stale = new FlutterDesignerDocumentState.Failure(
                "Load old model", pair.modelFile().getPath(), "Superseded test load.");
        FlutterDesignerDocumentController controller =
                new FlutterDesignerDocumentController(
                        pair.dartFile(),
                        pair.modelFile(),
                        () -> {
                            int invocation = loads.incrementAndGet();
                            if (invocation == 1) {
                                firstStarted.countDown();
                                awaitUninterruptibly(releaseFirst);
                                return stale;
                            }
                            return load(pair);
                        });
        controller.addPropertyChangeListener(event -> {
            if (event.getNewValue() == stale) {
                staleFailuresPublished.incrementAndGet();
            } else if (event.getNewValue() instanceof FlutterDesignerDocumentState.Current) {
                currentPublished.countDown();
            }
        });

        controller.viewOpened();
        assertTrue(firstStarted.await(5, TimeUnit.SECONDS));
        for (int index = 0; index < 500; index++) {
            controller.reload();
        }
        releaseFirst.countDown();

        assertTrue(currentPublished.await(5, TimeUnit.SECONDS));
        assertEquals(2, loads.get(), "a reload storm must collapse to one follow-up load");
        assertEquals(0, staleFailuresPublished.get());
        assertInstanceOf(FlutterDesignerDocumentState.Current.class, controller.state());
        controller.viewClosed();
    }

    @Test
    void closingLastViewSuppressesAnInFlightCompletion() throws Exception {
        Pair pair = pair("home_page", validDocument(
                "home_page.dart", "flutter.widgets.SizedBox"));
        CountDownLatch loadStarted = new CountDownLatch(1);
        CountDownLatch releaseLoad = new CountDownLatch(1);
        CountDownLatch loadReturned = new CountDownLatch(1);
        AtomicInteger currentPublications = new AtomicInteger();
        FlutterDesignerDocumentController controller =
                new FlutterDesignerDocumentController(
                        pair.dartFile(),
                        pair.modelFile(),
                        () -> {
                            loadStarted.countDown();
                            awaitUninterruptibly(releaseLoad);
                            FlutterDesignerDocumentState result = load(pair);
                            loadReturned.countDown();
                            return result;
                        });
        controller.addPropertyChangeListener(event -> {
            if (event.getNewValue() instanceof FlutterDesignerDocumentState.Current) {
                currentPublications.incrementAndGet();
            }
        });

        controller.viewOpened();
        assertTrue(loadStarted.await(5, TimeUnit.SECONDS));
        controller.viewClosed();
        releaseLoad.countDown();
        assertTrue(loadReturned.await(5, TimeUnit.SECONDS));
        EventQueue.invokeAndWait(() -> {
        });

        assertEquals(0, currentPublications.get());
    }

    @Test
    void recoversAfterANonFatalBinaryLinkageFailure() throws Exception {
        Pair pair = pair("home_page", validDocument(
                "home_page.dart", "flutter.widgets.SizedBox"));
        AtomicInteger loads = new AtomicInteger();
        CountDownLatch failurePublished = new CountDownLatch(1);
        CountDownLatch currentPublished = new CountDownLatch(1);
        FlutterDesignerDocumentController controller =
                new FlutterDesignerDocumentController(
                        pair.dartFile(),
                        pair.modelFile(),
                        () -> {
                            if (loads.incrementAndGet() == 1) {
                                throw new NoClassDefFoundError("synthetic contributor mismatch");
                            }
                            return load(pair);
                        });
        controller.addPropertyChangeListener(event -> {
            if (event.getNewValue() instanceof FlutterDesignerDocumentState.Failure) {
                failurePublished.countDown();
            } else if (event.getNewValue() instanceof FlutterDesignerDocumentState.Current) {
                currentPublished.countDown();
            }
        });

        controller.viewOpened();
        assertTrue(failurePublished.await(5, TimeUnit.SECONDS));
        controller.reload();

        assertTrue(currentPublished.await(5, TimeUnit.SECONDS));
        assertEquals(2, loads.get());
        assertInstanceOf(FlutterDesignerDocumentState.Current.class, controller.state());
        controller.viewClosed();
    }

    @Test
    void anAssertionErrorDoesNotWedgeALaterReload() throws Exception {
        Pair pair = pair("home_page", validDocument(
                "home_page.dart", "flutter.widgets.SizedBox"));
        AtomicInteger loads = new AtomicInteger();
        CountDownLatch firstAttempted = new CountDownLatch(1);
        CountDownLatch currentPublished = new CountDownLatch(1);
        FlutterDesignerDocumentController controller =
                new FlutterDesignerDocumentController(
                        pair.dartFile(),
                        pair.modelFile(),
                        () -> {
                            if (loads.incrementAndGet() == 1) {
                                firstAttempted.countDown();
                                throw new AssertionError("synthetic fatal load failure");
                            }
                            return load(pair);
                        });
        controller.addPropertyChangeListener(event -> {
            if (event.getNewValue() instanceof FlutterDesignerDocumentState.Current) {
                currentPublished.countDown();
            }
        });

        controller.viewOpened();
        assertTrue(firstAttempted.await(5, TimeUnit.SECONDS));
        controller.reload();

        assertTrue(currentPublished.await(5, TimeUnit.SECONDS));
        assertEquals(2, loads.get());
        assertInstanceOf(FlutterDesignerDocumentState.Current.class, controller.state());
        controller.viewClosed();
    }

    private static FlutterDesignerDocumentState load(Pair pair) {
        return FlutterDesignerDocumentController.load(
                pair.dartFile(),
                pair.modelFile(),
                new FdDocumentCodec(),
                new WidgetTreeValidator(),
                BuiltInWidgetCatalog.getDefault());
    }

    private static FlutterDesignerDocumentController controller(Pair pair) {
        return new FlutterDesignerDocumentController(
                pair.dartFile(),
                pair.modelFile(),
                new FdDocumentCodec(),
                new WidgetTreeValidator(),
                BuiltInWidgetCatalog.getDefault());
    }

    private static Pair pair(String baseName, String modelText) throws Exception {
        FileSystem fileSystem = FileUtil.createMemoryFileSystem();
        FileObject folder = fileSystem.getRoot().createFolder("lib");
        FileObject dart = folder.createData(baseName + ".dart");
        FileObject model = folder.createData(baseName + ".fd");
        String dartText = validDartSource();
        write(dart, dartText);
        write(model, modelText);
        return new Pair(dart, model, modelText, dartText);
    }

    private static void write(FileObject file, String value) throws Exception {
        try (FileLock lock = file.lock();
                OutputStream output = file.getOutputStream(lock)) {
            output.write(value.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void awaitLoadCount(AtomicInteger loads, int expected) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (loads.get() < expected && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }
        assertEquals(expected, loads.get());
    }

    private static void awaitUninterruptibly(CountDownLatch latch) {
        boolean interrupted = false;
        while (true) {
            try {
                latch.await();
                break;
            } catch (InterruptedException ex) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean await(
            CountDownLatch latch,
            long timeout,
            TimeUnit unit) {
        try {
            return latch.await(timeout, unit);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }

    private static String validDocument(String dartFile, String rootType) {
        return """
                {
                  "format": "netbeans-flutter-designer",
                  "schemaVersion": 1,
                  "documentId": "2f04ce87-876a-4f35-8a7c-2fba3e135c7e",
                  "source": {
                    "dartFile": "%s",
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
                    "type": "%s",
                    "properties": {},
                    "slots": {}
                  }
                }
                """.formatted(
                dartFile,
                DartManagedRegionHashing.normalizedSha256(importsPayload()),
                DartManagedRegionHashing.normalizedSha256(buildPayload()),
                rootType);
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

    private static String importsPayload() {
        return "import 'package:flutter/widgets.dart';\n";
    }

    private static String buildPayload() {
        return "  @override\n"
                + "  Widget build(BuildContext context) {\n"
                + "    return const SizedBox();\n"
                + "  }\n";
    }

    private static FdCodecLimits withMaximumDocumentBytes(int maximumBytes) {
        FdCodecLimits defaults = FdCodecLimits.defaults();
        return new FdCodecLimits(
                maximumBytes,
                defaults.maxJsonNestingDepth(),
                defaults.maxJsonTokens(),
                defaults.maxFieldNameUtf16Units(),
                defaults.maxStringUtf16Units(),
                defaults.maxStringCodePoints(),
                defaults.maxNumberCharacters(),
                defaults.maxAbsoluteDecimalScale(),
                defaults.maxWidgetDepth(),
                defaults.maxWidgetNodes(),
                defaults.maxPropertiesPerWidget(),
                defaults.maxSlotsPerWidget(),
                defaults.maxListChildren(),
                defaults.maxExtensionKeysPerBag(),
                defaults.maxExtensionNestingDepth(),
                defaults.maxExtensionValues(),
                defaults.maxJsonObjectFields(),
                defaults.maxJsonArrayElements(),
                defaults.maxDiagnostics());
    }

    private static WidgetCatalogContributor contributor(
            String id, WidgetDefinition... definitions) {
        return new WidgetCatalogContributor() {
            @Override
            public String contributorId() {
                return id;
            }

            @Override
            public int apiVersion() {
                return WidgetCatalog.API_VERSION;
            }

            @Override
            public Collection<WidgetDefinition> definitions() {
                return List.of(definitions);
            }
        };
    }

    private static WidgetDefinition definition(String typeId) {
        String className = typeId.substring(typeId.lastIndexOf('.') + 1);
        return new WidgetDefinition(
                new WidgetTypeId(typeId),
                className,
                Optional.empty(),
                false,
                "package:example/widgets.dart",
                List.of("package:example/widgets.dart"),
                Set.of(),
                new PaletteMetadata("example", 500, 10, className),
                List.of(),
                List.of());
    }

    private record Pair(
            FileObject dartFile,
            FileObject modelFile,
            String modelText,
            String dartText) {
    }
}
