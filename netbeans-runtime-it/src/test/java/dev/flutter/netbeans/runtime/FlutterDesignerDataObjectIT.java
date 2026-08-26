package dev.flutter.netbeans.runtime;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import junit.framework.TestFailure;
import junit.framework.TestResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.netbeans.api.editor.guards.GuardedSectionManager;
import org.netbeans.api.editor.guards.SimpleSection;
import org.netbeans.core.api.multiview.MultiViewHandler;
import org.netbeans.core.api.multiview.MultiViewPerspective;
import org.netbeans.core.api.multiview.MultiViews;
import org.netbeans.junit.NbModuleSuite;
import org.netbeans.junit.NbTestCase;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.SaveAsCapable;
import org.openide.modules.ModuleInfo;
import org.openide.text.CloneableEditorSupport;
import org.openide.text.NbDocument;
import org.openide.util.Lookup;
import org.openide.windows.CloneableTopComponent;

/** Runtime gate for Matisse-style ownership of a paired .dart/.fd form. */
final class FlutterDesignerDataObjectIT {
    private static final String FLUTTER_MODULE = "dev.flutter.netbeans.netbeans.plugin";

    @Test
    void designerLoaderOwnsOnlyPairedDartAndFdFiles() {
        junit.framework.Test suite = NbModuleSuite.createConfiguration(
                        FlutterDesignerDataObjectRuntimeCase.class)
                .clusters("ide|harness|extra")
                .enableModules("extra", Pattern.quote(FLUTTER_MODULE))
                .enableClasspathModules(false)
                .honorAutoloadEager(true)
                .failOnMessage(Level.SEVERE)
                .failOnException(Level.SEVERE)
                .gui(false)
                .suite();

        TestResult result = new TestResult();
        suite.run(result);
        if (!result.wasSuccessful()) {
            Assertions.fail(runtimeFailures(result));
        }
        Assertions.assertEquals(
                1,
                result.runCount(),
                "The Flutter Designer DataObject runtime test did not run exactly once");
    }

    private static String runtimeFailures(TestResult result) {
        StringBuilder message = new StringBuilder(
                "Flutter Designer DataObject runtime gate failed");
        appendFailures(message, "error", result.errors());
        appendFailures(message, "failure", result.failures());
        return message.toString();
    }

    private static void appendFailures(
            StringBuilder message,
            String kind,
            Enumeration<TestFailure> failures) {
        while (failures.hasMoreElements()) {
            TestFailure failure = failures.nextElement();
            message.append(System.lineSeparator())
                    .append(kind)
                    .append(": ")
                    .append(failure.trace());
        }
    }

    public static final class FlutterDesignerDataObjectRuntimeCase extends NbTestCase {
        private static final String DESIGNER_DATA_OBJECT =
                "dev.flutter.netbeans.plugin.designer.FlutterDesignerDataObject";
        private static final String DESIGNER_MIME = "text/x-flutter-designer";
        private static final String DART_MIME = "text/x-dart";

        public FlutterDesignerDataObjectRuntimeCase(String name) {
            super(name);
        }

        public void testPairedFilesShareOneDataObjectAndOrphanDartIsUnclaimed()
                throws Exception {
            clearWorkDir();
            Class<?> designerType = Class.forName(
                    DESIGNER_DATA_OBJECT, true, flutterModule().getClassLoader());
            Path root = Files.createDirectories(getWorkDir().toPath().resolve("forms"));

            // This ordering is intentional: the pair-aware loader must win even when
            // NetBeans is asked for the technical Dart primary before the .fd sidecar.
            Pair dartFirstPair = createPair(root, "dart_first");
            DataObject dartFirst = DataObject.find(dartFirstPair.dart());
            DataObject fdSecond = DataObject.find(dartFirstPair.fd());
            assertDesignerPair(designerType, dartFirstPair, dartFirst, fdSecond);
            assertGuardedSourceEditor(dartFirstPair, dartFirst);
            assertDesignerMultiView(dartFirst, dartFirstPair);

            Pair fdFirstPair = createPair(root, "fd_first");
            DataObject fdFirst = DataObject.find(fdFirstPair.fd());
            DataObject dartSecond = DataObject.find(fdFirstPair.dart());
            assertDesignerPair(designerType, fdFirstPair, fdFirst, dartSecond);

            FileObject orphanDart = createFile(root.resolve("ordinary.dart"),
                    "void main() {}\n");
            assertEquals("An orphan .dart file must retain the Dart MIME type",
                    DART_MIME, FileUtil.getMIMEType(orphanDart));
            DataObject ordinary = DataObject.find(orphanDart);
            assertFalse("The Flutter Designer loader must not claim an orphan .dart file",
                    designerType.isInstance(ordinary));
            assertEquals("An orphan Dart DataObject must keep its own file as primary",
                    orphanDart, ordinary.getPrimaryFile());

            assertUnsavedOrphanIsNotUpgraded(root, designerType);

            FileObject lateDart = createFile(root.resolve("late_pair.dart"),
                    "void main() {}\n");
            DataObject cachedOrdinary = DataObject.find(lateDart);
            assertFalse("The pre-sidecar Dart file unexpectedly used the designer loader",
                    designerType.isInstance(cachedOrdinary));
            FileObject lateFd = createFile(root.resolve("late_pair.fd"), """
                    {
                      "format": "netbeans-flutter-designer",
                      "schemaVersion": 1,
                      "source": {"dartFile": "late_pair.dart"}
                    }
                    """);
            DataObject upgraded = DataObject.find(lateFd);
            assertTrue("An unmodified cached Dart file was not safely upgraded after its .fd appeared",
                    designerType.isInstance(upgraded));
            assertSame("The late .dart/.fd pair did not converge on one DataObject",
                    upgraded, DataObject.find(lateDart));
        }

        private void assertUnsavedOrphanIsNotUpgraded(Path root, Class<?> designerType)
                throws Exception {
            String diskSource = "void main() {}\n";
            String unsavedEdit = "// unsaved user edit\n";
            FileObject dart = createFile(root.resolve("buffered.dart"), diskSource);
            DataObject originalDataObject = DataObject.find(dart);
            assertFalse("The buffered orphan unexpectedly used the designer loader",
                    designerType.isInstance(originalDataObject));

            EditorCookie editor = originalDataObject.getLookup().lookup(EditorCookie.class);
            assertNotNull("The orphan Dart file has no EditorCookie", editor);
            StyledDocument document = editor.openDocument();
            try {
                NbDocument.runAtomicAsUser(document, () -> {
                    try {
                        document.insertString(document.getLength(), unsavedEdit, null);
                    } catch (BadLocationException ex) {
                        throw new AssertionError(ex);
                    }
                });
                assertTrue("The orphan editor did not retain its unsaved state",
                        editor.isModified());
                assertEquals("The orphan editor lost the user buffer before pairing",
                        diskSource + unsavedEdit,
                        document.getText(0, document.getLength()));

                FileObject model = createFile(root.resolve("buffered.fd"), """
                        {
                          "format": "netbeans-flutter-designer",
                          "schemaVersion": 1,
                          "source": {"dartFile": "buffered.dart"}
                        }
                        """);

                assertEquals("The late sidecar did not receive the designer MIME",
                        DESIGNER_MIME, FileUtil.getMIMEType(model));
                assertTrue("Creating .fd invalidated the DataObject with an unsaved Dart buffer",
                        originalDataObject.isValid());
                assertSame("Creating .fd replaced the DataObject with an unsaved Dart buffer",
                        originalDataObject, DataObject.find(dart));
                assertFalse("The unsaved orphan was silently converted to a designer DataObject",
                        designerType.isInstance(originalDataObject));
                assertSame("Creating .fd replaced the existing orphan EditorCookie",
                        editor,
                        originalDataObject.getLookup().lookup(EditorCookie.class));
                assertSame("Creating .fd replaced the open orphan document",
                        document, editor.openDocument());
                assertTrue("Creating .fd cleared the unsaved editor state",
                        editor.isModified());
                assertEquals("Creating .fd discarded or rewrote the unsaved user edit",
                        diskSource + unsavedEdit,
                        document.getText(0, document.getLength()));
                assertEquals("The unsaved editor buffer leaked to disk during late pairing",
                        diskSource, dart.asText(StandardCharsets.UTF_8.name()));
            } finally {
                // Discard the synthetic edit without invoking a headless save prompt.
                originalDataObject.setModified(false);
                assertTrue("The orphan editor did not close after discarding the test edit",
                        editor.close());
            }
        }

        private void assertGuardedSourceEditor(Pair pair, DataObject dataObject)
                throws Exception {
            assertNull("Dart-only Save As must not be exposed for a paired designer form",
                    dataObject.getLookup().lookup(SaveAsCapable.class));

            EditorCookie editor = dataObject.getLookup().lookup(EditorCookie.class);
            assertNotNull("The paired Dart source has no EditorCookie", editor);
            assertTrue("The paired source editor is not a CloneableEditorSupport",
                    editor instanceof CloneableEditorSupport);

            StyledDocument document = editor.openDocument();
            assertEquals("The paired source did not use the Dart EditorKit",
                    DART_MIME, document.getProperty("mimeType"));
            GuardedSectionManager manager = GuardedSectionManager.getInstance(document);
            assertNotNull("The paired Dart document has no guarded-section manager", manager);
            SimpleSection section = manager.findSimpleSection("build");
            assertNotNull("The generated build region was not restored as a guard", section);

            AtomicReference<BadLocationException> guardedFailure = new AtomicReference<>();
            NbDocument.runAtomicAsUser(document, () -> {
                try {
                    document.insertString(
                            section.getStartPosition().getOffset() + 1,
                            "X",
                            null);
                } catch (BadLocationException ex) {
                    guardedFailure.set(ex);
                }
            });
            assertNotNull("A user edit inside the generated build region was accepted",
                    guardedFailure.get());

            NbDocument.runAtomicAsUser(document, () -> {
                try {
                    document.insertString(document.getLength(), "// manual change\n", null);
                } catch (BadLocationException ex) {
                    throw new AssertionError(ex);
                }
            });
            SaveCookie save = dataObject.getLookup().lookup(SaveCookie.class);
            assertNotNull("Editing user-owned Dart did not publish a SaveCookie", save);
            save.save();

            String persisted = pair.dart().asText(StandardCharsets.UTF_8.name());
            assertTrue("Saving Source lost the opening designer marker",
                    persisted.contains("// <netbeans-flutter-designer region=\"build\">"));
            assertTrue("Saving Source lost the closing designer marker",
                    persisted.contains("// </netbeans-flutter-designer>"));
            assertTrue("Saving Source lost the user-owned edit",
                    persisted.endsWith("// manual change\n"));
            editor.close();
        }

        private void assertDesignerMultiView(DataObject dataObject, Pair pair)
                throws Exception {
            byte[] modelBeforeOpen = pair.fd().asBytes();
            byte[] dartBeforeOpen = pair.dart().asBytes();
            CloneableEditorSupport pairedEditor = dataObject.getLookup()
                    .lookup(CloneableEditorSupport.class);
            assertNotNull("The paired form has no CloneableEditorSupport", pairedEditor);
            StyledDocument pairedDocument = pairedEditor.openDocument();

            AtomicReference<CloneableTopComponent> openedMultiView = new AtomicReference<>();
            try {
                SwingUtilities.invokeAndWait(() -> {
                    assertTrue("The designer MultiView must be created and opened on the EDT",
                            SwingUtilities.isEventDispatchThread());

                    CloneableTopComponent multiView = MultiViews.createCloneableMultiView(
                            DESIGNER_MIME,
                            dataObject);
                    openedMultiView.set(multiView);
                    multiView.open();
                    assertTrue("The designer MultiView did not open", multiView.isOpened());

                    MultiViewHandler handler = MultiViews.findMultiViewHandler(multiView);
                    assertNotNull("The opened designer has no MultiViewHandler", handler);
                    MultiViewPerspective[] perspectives = handler.getPerspectives();
                    String perspectiveSummary = java.util.Arrays.stream(perspectives)
                            .map(perspective -> perspective.getDisplayName()
                                    + " [" + perspective.preferredID() + "]")
                            .collect(Collectors.joining(", "));
                    List<MultiViewPerspective> designerPerspectives = new ArrayList<>();
                    Set<String> designerIds = new java.util.LinkedHashSet<>();
                    for (MultiViewPerspective perspective : perspectives) {
                        if (perspective.preferredID().startsWith("flutter.designer.")
                                && designerIds.add(perspective.preferredID())) {
                            designerPerspectives.add(perspective);
                        }
                    }
                    assertEquals("The designer must own exactly Design and Source; found "
                            + perspectiveSummary,
                            2, designerPerspectives.size());
                    assertPerspective(designerPerspectives.get(0),
                            "Design", "flutter.designer.design");
                    assertPerspective(designerPerspectives.get(1),
                            "Source", "flutter.designer.source");
                    assertPerspective(handler.getSelectedPerspective(),
                            "Design", "flutter.designer.design");

                    MultiViewPerspective sourcePerspective = designerPerspectives.get(1);
                    handler.requestVisible(sourcePerspective);
                    assertPerspective(handler.getSelectedPerspective(),
                            "Source", "flutter.designer.source");

                    CloneableEditorSupport sourceEditor = multiView.getLookup()
                            .lookup(CloneableEditorSupport.class);
                    assertSame("Source did not use the paired Dart editor support",
                            pairedEditor, sourceEditor);
                    try {
                        assertSame("Source opened a different Dart document",
                                pairedDocument, sourceEditor.openDocument());
                    } catch (Exception ex) {
                        throw new AssertionError("Source could not open the paired Dart document", ex);
                    }
                    assertEquals("The Source document lost the Dart MIME type",
                            DART_MIME, pairedDocument.getProperty("mimeType"));

                    assertEquals("The Source perspective is not editing the paired Dart file",
                            pair.dart(), dataObject.getPrimaryFile());
                });
                assertCurrentDesignerModelLoaded(dataObject, pair);
                assertFalse("Loading Design unexpectedly marked the pair modified",
                        dataObject.isModified());
                assertNull("Loading Design unexpectedly published a SaveCookie",
                        dataObject.getLookup().lookup(SaveCookie.class));
                assertTrue("Loading Design rewrote or canonicalized the .fd source",
                        java.util.Arrays.equals(modelBeforeOpen, pair.fd().asBytes()));
                assertTrue("Loading Design rewrote the paired Dart source",
                        java.util.Arrays.equals(dartBeforeOpen, pair.dart().asBytes()));
            } finally {
                CloneableTopComponent multiView = openedMultiView.get();
                if (multiView != null) {
                    SwingUtilities.invokeAndWait(() ->
                            assertTrue("The designer MultiView did not close", multiView.close()));
                }
                pairedEditor.close();
            }
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private void assertCurrentDesignerModelLoaded(DataObject dataObject, Pair pair)
                throws Exception {
            ClassLoader loader = flutterModule().getClassLoader();
            Class<?> controllerType = Class.forName(
                    "dev.flutter.netbeans.plugin.designer.FlutterDesignerDocumentController",
                    true,
                    loader);
            Object controller = dataObject.getLookup().lookup((Class) controllerType);
            assertNotNull("The paired DataObject does not publish its designer controller",
                    controller);

            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
            Object state;
            do {
                state = controllerType.getMethod("state").invoke(controller);
                if (state.getClass().getSimpleName().equals("Current")) {
                    List<?> contextIssues = (List<?>) state.getClass()
                            .getMethod("contextIssues").invoke(state);
                    assertTrue("The valid pair has context issues: " + contextIssues,
                            contextIssues.isEmpty());
                    java.util.Optional<?> integrity = (java.util.Optional<?>) state.getClass()
                            .getMethod("sourceIntegrity").invoke(state);
                    assertTrue("Current state did not retain Dart integrity facts",
                            integrity.isPresent());
                    Object result = integrity.orElseThrow();
                    assertEquals("The on-disk Dart source did not match declared hashes",
                            Boolean.TRUE,
                            result.getClass().getMethod("onDiskDeclaredMatch").invoke(result));
                    List<?> diagnostics = (List<?>) result.getClass()
                            .getMethod("diagnostics").invoke(result);
                    assertTrue("The valid Dart source has integrity diagnostics: "
                            + diagnostics, diagnostics.isEmpty());
                    java.util.Optional<?> original = (java.util.Optional<?>) result.getClass()
                            .getMethod("original").invoke(result);
                    assertTrue("The integrity result lost the exact Dart baseline",
                            original.isPresent());
                    byte[] baseline = (byte[]) original.orElseThrow().getClass()
                            .getMethod("copyBytes").invoke(original.orElseThrow());
                    assertTrue("The retained Dart baseline differs from the source file",
                            java.util.Arrays.equals(pair.dart().asBytes(), baseline));
                    java.util.Optional<?> threeWay = (java.util.Optional<?>) state.getClass()
                            .getMethod("threeWayIntegrity").invoke(state);
                    assertTrue("Current state did not retain three-way integrity facts",
                            threeWay.isPresent());
                    Object threeWayResult = threeWay.orElseThrow();
                    assertEquals("The valid pair did not reach an on-disk three-way match",
                            Boolean.TRUE,
                            threeWayResult.getClass()
                                    .getMethod("onDiskThreeWayMatch")
                                    .invoke(threeWayResult));
                    List<?> threeWayDiagnostics = (List<?>) threeWayResult.getClass()
                            .getMethod("diagnostics").invoke(threeWayResult);
                    assertTrue("The valid pair has three-way diagnostics: "
                            + threeWayDiagnostics, threeWayDiagnostics.isEmpty());
                    List<?> comparisons = (List<?>) threeWayResult.getClass()
                            .getMethod("comparisons").invoke(threeWayResult);
                    assertEquals("Three-way evidence must cover imports and build",
                            2, comparisons.size());
                    for (Object comparison : comparisons) {
                        assertEquals("A managed region did not match all three hash facts",
                                Boolean.TRUE,
                                comparison.getClass()
                                        .getMethod("allNormalizedHashesMatch")
                                        .invoke(comparison));
                    }
                    return;
                }
                if (!state.getClass().getSimpleName().equals("Idle")
                        && !state.getClass().getSimpleName().equals("Loading")) {
                    fail("The valid .fd model did not reach Current state: " + state);
                }
                Thread.sleep(20);
            } while (System.nanoTime() < deadline);
            fail("Timed out waiting for the valid .fd model; last state: " + state);
        }

        private void assertPerspective(
                MultiViewPerspective perspective,
                String displayName,
                String preferredId) {
            assertEquals("Unexpected MultiView perspective label",
                    displayName, perspective.getDisplayName());
            assertEquals("Unexpected MultiView perspective ID",
                    preferredId, perspective.preferredID());
        }

        private void assertDesignerPair(
                Class<?> designerType,
                Pair pair,
                DataObject first,
                DataObject second) {
            assertEquals("The .fd sidecar must resolve to the dedicated designer MIME",
                    DESIGNER_MIME, FileUtil.getMIMEType(pair.fd()));
            assertEquals("The technical primary must retain the Dart MIME type",
                    DART_MIME, FileUtil.getMIMEType(pair.dart()));
            assertSame("The paired .dart and .fd files must resolve to one DataObject",
                    first, second);
            assertTrue("The pair is not owned by FlutterDesignerDataObject: "
                    + first.getClass().getName(), designerType.isInstance(first));
            assertEquals("The .dart file must be the technical primary",
                    pair.dart(), first.getPrimaryFile());
            assertEquals("The designer DataObject must own exactly the pair",
                    Set.of(pair.dart(), pair.fd()), first.files());
            assertFalse("Unsafe pair rename must remain disabled in the foundation slice",
                    first.isRenameAllowed());
            assertFalse("Unsafe pair copy must remain disabled in the foundation slice",
                    first.isCopyAllowed());
            assertFalse("Unsafe pair move must remain disabled in the foundation slice",
                    first.isMoveAllowed());
            assertTrue("Deleting the complete pair should remain available",
                    first.isDeleteAllowed());
        }

        private Pair createPair(Path directory, String baseName) throws Exception {
            FileObject dart = createFile(directory.resolve(baseName + ".dart"), """
                    // <netbeans-flutter-designer region="imports">
                    import 'package:flutter/widgets.dart';
                    // </netbeans-flutter-designer>

                    class SampleView extends StatelessWidget {
                      const SampleView({super.key});

                      // <netbeans-flutter-designer region="build">
                      @override
                      Widget build(BuildContext context) {
                        return const SizedBox();
                      }
                      // </netbeans-flutter-designer>
                    }
                    """);
            FileObject fd = createFile(directory.resolve(baseName + ".fd"), """
                    {
                      "format": "netbeans-flutter-designer",
                      "schemaVersion": 1,
                      "documentId": "2f04ce87-876a-4f35-8a7c-2fba3e135c7e",
                      "source": {
                        "dartFile": "%s.dart",
                        "className": "SampleView",
                        "widgetKind": "stateless",
                        "managedRegions": {
                          "imports": {
                            "sha256": "2FC35DE54B0A58A3211FDB1CAC2ABE866DD32B3279FE2E5EF1C3282848D85BFB"
                          },
                          "build": {
                            "sha256": "57D0BB0F067B9DC678CD4DF00243350043286785B7B88DE1C247E10641DFB8F5"
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
                    """.formatted(baseName));
            return new Pair(dart, fd);
        }

        private FileObject createFile(Path path, String content) throws Exception {
            Files.writeString(path, content, StandardCharsets.UTF_8);
            FileUtil.refreshFor(path.toFile());
            FileObject file = FileUtil.toFileObject(path.toFile());
            assertNotNull("No FileObject for " + path, file);
            return file;
        }

        private ModuleInfo flutterModule() {
            Collection<? extends ModuleInfo> modules =
                    Lookup.getDefault().lookupAll(ModuleInfo.class);
            ModuleInfo module = modules.stream()
                    .filter(candidate -> FLUTTER_MODULE.equals(candidate.getCodeNameBase()))
                    .findFirst()
                    .orElse(null);
            assertNotNull("Flutter module is absent; available modules: "
                    + modules.stream()
                            .map(ModuleInfo::getCodeNameBase)
                            .sorted()
                            .collect(Collectors.joining(", ")), module);
            assertTrue("Flutter module must be enabled", module.isEnabled());
            return module;
        }

        private record Pair(FileObject dart, FileObject fd) {
        }
    }
}
