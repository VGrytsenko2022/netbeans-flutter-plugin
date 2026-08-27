package dev.flutter.netbeans.runtime;

import java.awt.Component;
import java.awt.Container;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.beans.BeanInfo;
import java.net.URL;
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
import javax.accessibility.AccessibleContext;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import junit.framework.TestFailure;
import junit.framework.TestResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.netbeans.api.editor.guards.GuardedSectionManager;
import org.netbeans.api.editor.guards.SimpleSection;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.core.api.multiview.MultiViewHandler;
import org.netbeans.core.api.multiview.MultiViewPerspective;
import org.netbeans.core.api.multiview.MultiViews;
import org.netbeans.junit.NbModuleSuite;
import org.netbeans.junit.NbTestCase;
import org.openide.awt.UndoRedo;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.OpenCookie;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.SaveAsCapable;
import org.openide.modules.ModuleInfo;
import org.openide.nodes.Node;
import org.openide.text.CloneableEditorSupport;
import org.openide.text.NbDocument;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.datatransfer.ExTransferable;
import org.openide.util.datatransfer.PasteType;
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
        private static final String DESIGNER_MODEL_DATA_OBJECT =
                "dev.flutter.netbeans.plugin.designer.FlutterDesignerModelDataObject";
        private static final String DART_DATA_OBJECT =
                "dev.flutter.netbeans.plugin.dart.DartDataObject";
        private static final String DESIGNER_MIME = "text/x-flutter-designer";
        private static final String DART_MIME = "text/x-dart";

        public FlutterDesignerDataObjectRuntimeCase(String name) {
            super(name);
        }

        public void testPairedFilesExposeVisibleModelAndShareOneEditorOwner()
                throws Exception {
            clearWorkDir();
            Class<?> designerType = Class.forName(
                    DESIGNER_DATA_OBJECT, true, flutterModule().getClassLoader());
            Class<?> modelType = Class.forName(
                    DESIGNER_MODEL_DATA_OBJECT, true, flutterModule().getClassLoader());
            Class<?> dartType = Class.forName(
                    DART_DATA_OBJECT, true, flutterModule().getClassLoader());
            Path root = createFlutterProject(
                    getWorkDir().toPath().resolve("forms"));

            // This ordering is intentional: the pair-aware loader must win even when
            // NetBeans is asked for the technical Dart primary before the .fd sidecar.
            Pair dartFirstPair = createPair(root, "dart_first");
            DataObject dartFirst = DataObject.find(dartFirstPair.dart());
            DataObject fdSecond = DataObject.find(dartFirstPair.fd());
            assertDesignerPair(
                    designerType, modelType, dartFirstPair, dartFirst, fdSecond);
            assertGuardedSourceEditor(dartFirstPair, dartFirst);
            assertDesignerMultiView(dartFirst, dartFirstPair);

            Pair fdFirstPair = createPair(root, "fd_first");
            DataObject fdFirst = DataObject.find(fdFirstPair.fd());
            DataObject dartSecond = DataObject.find(fdFirstPair.dart());
            assertDesignerPair(
                    designerType, modelType, fdFirstPair, dartSecond, fdFirst);

            FileObject orphanDart = createFile(root.resolve("lib/ordinary.dart"),
                    "void main() {}\n");
            assertEquals("An orphan .dart file must retain the Dart MIME type",
                    DART_MIME, FileUtil.getMIMEType(orphanDart));
            DataObject ordinary = DataObject.find(orphanDart);
            assertFalse("The Flutter Designer loader must not claim an orphan .dart file",
                    designerType.isInstance(ordinary));
            assertTrue("An ordinary .dart file is not owned by DartDataObject: "
                    + ordinary.getClass().getName(), dartType.isInstance(ordinary));
            assertEquals("An orphan Dart DataObject must keep its own file as primary",
                    orphanDart, ordinary.getPrimaryFile());
            assertFileTypeIcons(ordinary, dartFirst, fdSecond);

            assertUnsavedOrphanIsNotUpgraded(root, designerType);

            FileObject lateDart = createFile(root.resolve("lib/late_pair.dart"),
                    "void main() {}\n");
            DataObject cachedOrdinary = DataObject.find(lateDart);
            assertFalse("The pre-sidecar Dart file unexpectedly used the designer loader",
                    designerType.isInstance(cachedOrdinary));
            FileObject lateFd = createFile(root.resolve(".fd_templates/late_pair.fd"), """
                    {
                      "format": "netbeans-flutter-designer",
                      "schemaVersion": 1,
                      "source": {"dartFile": "late_pair.dart"}
                    }
                    """);
            DataObject upgraded = DataObject.find(lateFd);
            assertTrue("The late .fd file did not receive its visible model DataObject",
                    modelType.isInstance(upgraded));
            assertTrue("An unmodified cached Dart file was not safely upgraded after its .fd appeared",
                    designerType.isInstance(DataObject.find(lateDart)));

            assertPairRenameFromEitherNode(root, designerType, modelType);
            assertPairDeleteFromEitherNode(root, designerType, modelType);
            assertPairCopyFromEitherNode(root, designerType, modelType);
            assertPairMoveFromEitherNode(root, designerType, modelType);
        }

        private void assertPairRenameFromEitherNode(
                Path root,
                Class<?> designerType,
                Class<?> modelType) throws Exception {
            assertPairRenameFromNode(
                    root, "rename_from_dart", "renamed_from_dart",
                    designerType, modelType, true);
            assertPairRenameFromNode(
                    root, "rename_from_fd", "renamed_from_fd",
                    designerType, modelType, false);
        }

        private void assertPairRenameFromNode(
                Path root,
                String originalName,
                String targetName,
                Class<?> designerType,
                Class<?> modelType,
                boolean initiateFromDart) throws Exception {
            Pair pair = createPair(root, originalName);
            Path oldDartPath = FileUtil.toFile(pair.dart()).toPath();
            Path oldModelPath = FileUtil.toFile(pair.fd()).toPath();
            Path targetDartPath = oldDartPath.resolveSibling(targetName + ".dart");
            Path targetModelPath = oldModelPath.resolveSibling(targetName + ".fd");
            byte[] exactDart = Files.readAllBytes(oldDartPath);
            DataObject dartObject = DataObject.find(pair.dart());
            DataObject modelObject = DataObject.find(pair.fd());
            assertTrue("Rename fixture has no paired Dart owner",
                    designerType.isInstance(dartObject));
            assertTrue("Rename fixture has no visible .fd owner",
                    modelType.isInstance(modelObject));

            EditorCookie openEditor = null;
            if (!initiateFromDart) {
                openEditor = dartObject.getLookup().lookup(EditorCookie.class);
                assertNotNull("The paired Dart owner has no editor", openEditor);
                openEditor.openDocument();
                assertFalse("Opening a clean paired editor unexpectedly marked it dirty",
                        openEditor.isModified());
            }
            Node dartNode = dartObject.getNodeDelegate();
            Node modelNode = modelObject.getNodeDelegate();
            assertTrue("The Dart node does not publish paired Rename",
                    dartNode.canRename());
            assertTrue("The .fd node does not publish paired Rename",
                    modelNode.canRename());
            assertNoWritableExtensionProperty(dartNode, "Dart");
            assertNoWritableExtensionProperty(modelNode, ".fd");

            (initiateFromDart ? dartNode : modelNode).setName(targetName);

            assertFalse("Paired Rename left the old Dart path on disk",
                    Files.exists(oldDartPath));
            assertFalse("Paired Rename left the old .fd path on disk",
                    Files.exists(oldModelPath));
            assertTrue("Paired Rename did not create the target Dart path",
                    Files.exists(targetDartPath));
            assertTrue("Paired Rename did not create the target .fd path",
                    Files.exists(targetModelPath));
            assertTrue("Paired Rename invalidated the retained Dart FileObject",
                    pair.dart().isValid());
            assertTrue("Paired Rename invalidated the retained .fd FileObject",
                    pair.fd().isValid());
            assertEquals("The retained Dart FileObject has the wrong target name",
                    targetName + ".dart", pair.dart().getNameExt());
            assertEquals("The retained .fd FileObject has the wrong target name",
                    targetName + ".fd", pair.fd().getNameExt());
            assertSame("Paired Rename replaced the Dart FileObject identity",
                    pair.dart(), dartObject.getPrimaryFile());
            assertSame("Paired Rename replaced the .fd FileObject identity",
                    pair.fd(), modelObject.getPrimaryFile());
            assertSame("The renamed Dart path no longer resolves to its cached DataObject",
                    dartObject, DataObject.find(pair.dart()));
            assertSame("The renamed .fd path no longer resolves to its cached DataObject",
                    modelObject, DataObject.find(pair.fd()));
            assertEquals("The cached Dart node did not publish its renamed basename",
                    targetName, dartNode.getName());
            assertEquals("The cached .fd node did not publish its renamed basename",
                    targetName, modelNode.getName());
            assertTrue("Paired Rename changed user-owned Dart bytes",
                    java.util.Arrays.equals(
                            exactDart, Files.readAllBytes(targetDartPath)));
            String renamedModel = Files.readString(
                    targetModelPath, StandardCharsets.UTF_8);
            assertTrue("The renamed .fd model does not name the target Dart file",
                    renamedModel.contains("\"dartFile\": \""
                            + targetName + ".dart\""));
            assertTrue("Paired Rename changed the Dart class contract",
                    renamedModel.contains("\"className\": \"SampleView\""));
            if (openEditor != null) {
                assertNull("Paired Rename did not close the clean shared editor",
                        openEditor.getDocument());
            }
            assertTrue("The renamed Dart owner entered a delayed external conflict",
                    dartObject.isRenameAllowed());
            assertTrue("The renamed .fd owner entered a delayed external conflict",
                    modelObject.isRenameAllowed());

            // Exercise the path-bound controller again, as a reopened Design
            // view would, and then prove that ordinary Source editing/saving
            // still targets the renamed Dart file.
            ClassLoader loader = flutterModule().getClassLoader();
            Class<?> controllerType = Class.forName(
                    "dev.flutter.netbeans.plugin.designer.FlutterDesignerDocumentController",
                    true,
                    loader);
            Object controller = dartObject.getLookup().lookup(controllerType);
            assertNotNull("The renamed form lost its designer controller", controller);
            controllerType.getMethod("viewOpened").invoke(controller);
            try {
                assertCurrentDesignerModelLoaded(dartObject, pair);
            } finally {
                controllerType.getMethod("viewClosed").invoke(controller);
            }

            EditorCookie renamedEditor = dartObject.getLookup()
                    .lookup(EditorCookie.class);
            assertNotNull("The renamed Dart owner lost its editor", renamedEditor);
            StyledDocument reopened = renamedEditor.openDocument();
            String postRenameEdit = "// source edit after paired rename\n";
            NbDocument.runAtomicAsUser(reopened, () -> {
                try {
                    reopened.insertString(
                            reopened.getLength(), postRenameEdit, null);
                } catch (BadLocationException ex) {
                    throw new AssertionError(ex);
                }
            });
            SaveCookie save = dartObject.getLookup().lookup(SaveCookie.class);
            assertNotNull("A post-Rename Dart edit exposed no SaveCookie", save);
            save.save();
            assertFalse("The post-Rename Dart save left the editor modified",
                    renamedEditor.isModified());
            assertTrue("The post-Rename Source save wrote to an obsolete path",
                    Files.readString(targetDartPath, StandardCharsets.UTF_8)
                            .endsWith(postRenameEdit));
            assertTrue("The pair is not rename-capable after post-Rename save",
                    dartObject.isRenameAllowed());
            assertTrue("The post-Rename editor refused to close",
                    renamedEditor.close());
            try (var paths = Files.walk(root)) {
                assertFalse("Successful paired Rename left a private tombstone",
                        paths.anyMatch(path -> path.getFileName().toString()
                                .startsWith(".nb-flutter-rename-")));
            }
        }

        private void assertNoWritableExtensionProperty(
                Node node,
                String description) {
            for (Node.PropertySet propertySet : node.getPropertySets()) {
                for (Node.Property<?> property : propertySet.getProperties()) {
                    assertFalse("The " + description
                            + " paired node exposes the one-file Extension editor",
                            "extension".equals(property.getName()));
                }
            }
        }

        private void assertPairDeleteFromEitherNode(
                Path root,
                Class<?> designerType,
                Class<?> modelType) throws Exception {
            assertPairDeleteFromNode(
                    root, "delete_from_dart", designerType, modelType, true);
            assertPairDeleteFromNode(
                    root, "delete_from_fd", designerType, modelType, false);
        }

        private void assertPairDeleteFromNode(
                Path root,
                String baseName,
                Class<?> designerType,
                Class<?> modelType,
                boolean initiateFromDart) throws Exception {
            Pair pair = createPair(root, baseName);
            Path dartPath = FileUtil.toFile(pair.dart()).toPath();
            Path modelPath = FileUtil.toFile(pair.fd()).toPath();
            DataObject dartObject = DataObject.find(pair.dart());
            DataObject modelObject = DataObject.find(pair.fd());
            assertTrue("Delete fixture has no paired Dart owner",
                    designerType.isInstance(dartObject));
            assertTrue("Delete fixture has no visible .fd owner",
                    modelType.isInstance(modelObject));
            EditorCookie openEditor = null;
            if (!initiateFromDart) {
                openEditor = dartObject.getLookup().lookup(EditorCookie.class);
                assertNotNull("The paired Dart owner has no editor", openEditor);
                openEditor.openDocument();
                assertFalse("Opening a clean paired editor unexpectedly marked it dirty",
                        openEditor.isModified());
            }
            Node dartNode = dartObject.getNodeDelegate();
            Node modelNode = modelObject.getNodeDelegate();
            assertTrue("The Dart node does not publish paired Delete",
                    dartNode.canDestroy());
            assertTrue("The .fd node does not publish paired Delete",
                    modelNode.canDestroy());

            (initiateFromDart ? dartNode : modelNode).destroy();

            assertFalse("Paired Delete left the canonical Dart file on disk",
                    Files.exists(dartPath));
            assertFalse("Paired Delete left the canonical .fd file on disk",
                    Files.exists(modelPath));
            assertFalse("Paired Delete left the Dart FileObject valid",
                    pair.dart().isValid());
            assertFalse("Paired Delete left the .fd FileObject valid",
                    pair.fd().isValid());
            assertFalse("Paired Delete left the Dart DataObject valid",
                    dartObject.isValid());
            assertFalse("Paired Delete left the .fd DataObject valid",
                    modelObject.isValid());
            if (openEditor != null) {
                assertNull("Paired Delete did not close the clean shared editor",
                        openEditor.getDocument());
            }
            try (var paths = Files.walk(root)) {
                assertFalse("Successful paired Delete left a private tombstone",
                        paths.anyMatch(path -> path.getFileName().toString()
                                .endsWith(".nbdelete")));
            }
        }

        private void assertPairCopyFromEitherNode(
                Path root,
                Class<?> designerType,
                Class<?> modelType) throws Exception {
            FileObject dartCollision = createFile(
                    root.resolve("lib/copy_from_dart_copy.dart"),
                    "// occupied only in the Dart tree\n");
            byte[] exactDartCollision = dartCollision.asBytes();
            assertPairCopyFromNode(
                    root,
                    "copy_from_dart",
                    "copy_from_dart_copy_2",
                    designerType,
                    modelType,
                    true);
            assertFalse("A Dart-only collision did not reserve the mirrored suffix",
                    Files.exists(root.resolve(
                            ".fd_templates/copy_from_dart_copy.fd")));
            assertTrue("Pair Copy changed the occupied Dart collision candidate",
                    java.util.Arrays.equals(
                            exactDartCollision, dartCollision.asBytes()));

            FileObject modelCollision = createFile(
                    root.resolve(".fd_templates/copy_from_fd_copy.fd"),
                    "occupied only in the model tree\n");
            byte[] exactModelCollision = modelCollision.asBytes();
            assertPairCopyFromNode(
                    root,
                    "copy_from_fd",
                    "copy_from_fd_copy_2",
                    designerType,
                    modelType,
                    false);
            assertFalse("An .fd-only collision did not reserve the mirrored suffix",
                    Files.exists(root.resolve("lib/copy_from_fd_copy.dart")));
            assertTrue("Pair Copy changed the occupied .fd collision candidate",
                    java.util.Arrays.equals(
                            exactModelCollision, modelCollision.asBytes()));

            try (var paths = Files.walk(root)) {
                assertFalse("Successful paired Copy left a private staging artifact",
                        paths.anyMatch(path -> path.getFileName().toString()
                                .startsWith(".nb-flutter-copy-")));
            }
        }

        private void assertPairCopyFromNode(
                Path root,
                String sourceStem,
                String targetStem,
                Class<?> designerType,
                Class<?> modelType,
                boolean initiateFromDart) throws Exception {
            assertFalse("The pair clipboard integration test must run off the EDT",
                    SwingUtilities.isEventDispatchThread());
            Pair source = createPair(root, sourceStem);
            DataObject sourceDartObject = DataObject.find(source.dart());
            DataObject sourceModelObject = DataObject.find(source.fd());
            assertTrue("Copy fixture has no paired Dart owner",
                    designerType.isInstance(sourceDartObject));
            assertTrue("Copy fixture has no visible .fd owner",
                    modelType.isInstance(sourceModelObject));

            EditorCookie sourceEditor = sourceDartObject.getLookup()
                    .lookup(EditorCookie.class);
            assertNotNull("The Copy fixture has no shared Dart editor", sourceEditor);
            StyledDocument sourceDocument = sourceEditor.openDocument();
            assertFalse("Opening the Copy fixture unexpectedly marked it dirty",
                    sourceEditor.isModified());
            byte[] exactDart = source.dart().asBytes();
            byte[] exactModel = source.fd().asBytes();
            Object originalDocument = decodeDesignerDocument(
                    source.fd(), flutterModule().getClassLoader());

            Node dartNode = sourceDartObject.getNodeDelegate();
            Node modelNode = sourceModelObject.getNodeDelegate();
            assertPairCopyNodeContract(dartNode, sourceDartObject, "Dart");
            assertPairCopyNodeContract(modelNode, sourceModelObject, ".fd");

            Node initiatingNode = initiateFromDart ? dartNode : modelNode;
            Transferable clipboard = initiatingNode.clipboardCopy();
            assertFalse("Pair Copy leaked the operating-system file-list flavor",
                    clipboard.isDataFlavorSupported(DataFlavor.javaFileListFlavor));

            FileObject physicalDestination = initiateFromDart
                    ? source.dart().getParent()
                    : source.fd().getParent();
            Node destinationFolderNode = DataFolder
                    .findFolder(physicalDestination)
                    .getNodeDelegate();
            PasteType pairPaste = pairCopyPasteType(
                    destinationFolderNode.getPasteTypes(clipboard));
            assertNotNull("The matching physical folder node exposed no pair Copy paste",
                    pairPaste);
            assertNull("The pair Copy PasteType must consume the clipboard payload",
                    pairPaste.paste());

            FileObject targetDart = source.dart().getParent()
                    .getFileObject(targetStem, "dart");
            FileObject targetModel = source.fd().getParent()
                    .getFileObject(targetStem, "fd");
            assertNotNull("Pair Copy did not publish the target Dart file", targetDart);
            assertNotNull("Pair Copy did not publish the target .fd file", targetModel);
            assertTrue("Pair Copy changed the exact user-owned Dart bytes",
                    java.util.Arrays.equals(exactDart, targetDart.asBytes()));

            DataObject targetDartObject = DataObject.find(targetDart);
            DataObject targetModelObject = DataObject.find(targetModel);
            assertTrue("The copied Dart file has no Designer DataObject",
                    designerType.isInstance(targetDartObject));
            assertTrue("The copied .fd file has no Designer model DataObject",
                    modelType.isInstance(targetModelObject));
            assertNotSame("Pair Copy reused the source Dart DataObject",
                    sourceDartObject, targetDartObject);
            assertNotSame("Pair Copy reused the source .fd DataObject",
                    sourceModelObject, targetModelObject);

            Object targetDocument = decodeDesignerDocument(
                    targetModel, flutterModule().getClassLoader());
            assertCopiedDocumentSemantics(
                    originalDocument, targetDocument, targetStem + ".dart");

            assertTrue("Pair Copy invalidated the original Dart FileObject",
                    source.dart().isValid());
            assertTrue("Pair Copy invalidated the original .fd FileObject",
                    source.fd().isValid());
            assertTrue("Pair Copy invalidated the original Dart DataObject",
                    sourceDartObject.isValid());
            assertTrue("Pair Copy invalidated the original .fd DataObject",
                    sourceModelObject.isValid());
            assertSame("Pair Copy replaced the original Dart DataObject",
                    sourceDartObject, DataObject.find(source.dart()));
            assertSame("Pair Copy replaced the original .fd DataObject",
                    sourceModelObject, DataObject.find(source.fd()));
            assertSame("Pair Copy closed or replaced the original source buffer",
                    sourceDocument, sourceEditor.getDocument());
            assertFalse("Pair Copy dirtied the original source buffer",
                    sourceEditor.isModified());
            assertTrue("Pair Copy changed the original Dart file",
                    java.util.Arrays.equals(exactDart, source.dart().asBytes()));
            assertTrue("Pair Copy changed the original .fd file",
                    java.util.Arrays.equals(exactModel, source.fd().asBytes()));
            assertTrue("The original source editor refused to close after Pair Copy",
                    sourceEditor.close());
        }

        private void assertPairCopyNodeContract(
                Node node,
                DataObject dataObject,
                String description) {
            assertTrue("The " + description + " node does not publish paired Copy",
                    node.canCopy());
            assertTrue("The " + description + " node does not publish paired Cut",
                    node.canCut());
            assertFalse("The " + description
                    + " DataObject unexpectedly enables generic one-file Copy",
                    dataObject.isCopyAllowed());
            assertFalse("The " + description
                    + " DataObject unexpectedly enables generic one-file Move",
                    dataObject.isMoveAllowed());
        }

        private PasteType pairCopyPasteType(PasteType[] pasteTypes) {
            PasteType found = null;
            for (PasteType pasteType : pasteTypes) {
                if ("Copy Flutter Designer Form".equals(pasteType.getName())) {
                    assertNull("The folder node exposed duplicate pair Copy PasteTypes",
                            found);
                    found = pasteType;
                }
            }
            return found;
        }

        private void assertPairMoveFromEitherNode(
                Path root,
                Class<?> designerType,
                Class<?> modelType) throws Exception {
            assertPairMoveFromNode(
                    root,
                    "move_from_dart",
                    "moved/from_dart",
                    designerType,
                    modelType,
                    true);
            assertPairMoveFromNode(
                    root,
                    "move_from_fd",
                    "moved/from_fd",
                    designerType,
                    modelType,
                    false);

            try (var paths = Files.walk(root)) {
                assertFalse("Successful paired Move left a private .nbmove artifact",
                        paths.anyMatch(path -> path.getFileName().toString()
                                .endsWith(".nbmove")));
            }
        }

        private void assertPairMoveFromNode(
                Path root,
                String sourceStem,
                String targetRelativeFolder,
                Class<?> designerType,
                Class<?> modelType,
                boolean initiateFromDart) throws Exception {
            assertFalse("The pair Cut/Move integration test must run off the EDT",
                    SwingUtilities.isEventDispatchThread());
            Pair source = createPair(root, sourceStem);
            Path oldDartPath = FileUtil.toFile(source.dart()).toPath();
            Path oldModelPath = FileUtil.toFile(source.fd()).toPath();
            byte[] exactDart = source.dart().asBytes();
            byte[] exactModel = source.fd().asBytes();

            DataObject oldDartObject = DataObject.find(source.dart());
            DataObject oldModelObject = DataObject.find(source.fd());
            assertTrue("Move fixture has no paired Dart owner",
                    designerType.isInstance(oldDartObject));
            assertTrue("Move fixture has no visible .fd owner",
                    modelType.isInstance(oldModelObject));
            assertFalse("The Dart DataObject unexpectedly enables generic one-file Move",
                    oldDartObject.isMoveAllowed());
            assertFalse("The .fd DataObject unexpectedly enables generic one-file Move",
                    oldModelObject.isMoveAllowed());

            EditorCookie oldEditor = oldDartObject.getLookup()
                    .lookup(EditorCookie.class);
            assertNotNull("The Move fixture has no shared Dart editor", oldEditor);
            oldEditor.openDocument();
            assertFalse("Opening the Move fixture unexpectedly marked it dirty",
                    oldEditor.isModified());

            Node dartNode = oldDartObject.getNodeDelegate();
            Node modelNode = oldModelObject.getNodeDelegate();
            assertTrue("The Dart node does not publish paired Cut", dartNode.canCut());
            assertTrue("The .fd node does not publish paired Cut", modelNode.canCut());
            Node initiatingNode = initiateFromDart ? dartNode : modelNode;
            Transferable clipboard = initiatingNode.clipboardCut();
            assertFalse("Pair Cut leaked the operating-system file-list flavor",
                    clipboard.isDataFlavorSupported(DataFlavor.javaFileListFlavor));

            Path targetDartFolderPath = root.resolve(
                    "lib/" + targetRelativeFolder);
            Path targetModelFolderPath = root.resolve(
                    ".fd_templates/" + targetRelativeFolder);
            Files.createDirectories(targetDartFolderPath);
            Files.createDirectories(targetModelFolderPath);
            FileUtil.refreshFor(
                    targetDartFolderPath.toFile(),
                    targetModelFolderPath.toFile());
            FileObject directTargetFolder = FileUtil.toFileObject(
                    (initiateFromDart
                            ? targetDartFolderPath
                            : targetModelFolderPath).toFile());
            assertNotNull("No direct physical destination folder FileObject",
                    directTargetFolder);
            Node destinationFolderNode = DataFolder
                    .findFolder(directTargetFolder)
                    .getNodeDelegate();
            PasteType pairPaste = pairMovePasteType(
                    destinationFolderNode.getPasteTypes(clipboard));
            assertNotNull("The direct destination folder exposed no pair Move paste",
                    pairPaste);
            Transferable consumed = pairPaste.paste();
            assertSame("Successful pair Move must consume its one-shot Cut payload",
                    ExTransferable.EMPTY, consumed);

            Path targetDartPath = targetDartFolderPath.resolve(
                    sourceStem + ".dart");
            Path targetModelPath = targetModelFolderPath.resolve(
                    sourceStem + ".fd");
            assertFalse("Pair Move left the old Dart path on disk",
                    Files.exists(oldDartPath));
            assertFalse("Pair Move left the old .fd path on disk",
                    Files.exists(oldModelPath));
            assertTrue("Pair Move did not publish the target Dart path",
                    Files.exists(targetDartPath));
            assertTrue("Pair Move did not publish the target .fd path",
                    Files.exists(targetModelPath));
            assertFalse("Pair Move left the old Dart FileObject valid",
                    source.dart().isValid());
            assertFalse("Pair Move left the old .fd FileObject valid",
                    source.fd().isValid());
            assertFalse("Pair Move left the old Dart DataObject valid",
                    oldDartObject.isValid());
            assertFalse("Pair Move left the old .fd DataObject valid",
                    oldModelObject.isValid());
            assertNull("Pair Move did not close the clean shared editor",
                    oldEditor.getDocument());

            FileUtil.refreshFor(
                    targetDartPath.toFile(),
                    targetModelPath.toFile());
            FileObject targetDart = FileUtil.toFileObject(targetDartPath.toFile());
            FileObject targetModel = FileUtil.toFileObject(targetModelPath.toFile());
            assertNotNull("No fresh target Dart FileObject", targetDart);
            assertNotNull("No fresh target .fd FileObject", targetModel);
            assertNotSame("Pair Move reused the retired Dart FileObject",
                    source.dart(), targetDart);
            assertNotSame("Pair Move reused the retired .fd FileObject",
                    source.fd(), targetModel);
            assertTrue("Pair Move changed the exact user-owned Dart bytes",
                    java.util.Arrays.equals(exactDart, targetDart.asBytes()));
            assertTrue("Pair Move changed the exact user-owned .fd bytes",
                    java.util.Arrays.equals(exactModel, targetModel.asBytes()));

            DataObject targetDartObject = DataObject.find(targetDart);
            DataObject targetModelObject = DataObject.find(targetModel);
            assertNotSame("Pair Move reused the retired Dart DataObject",
                    oldDartObject, targetDartObject);
            assertNotSame("Pair Move reused the retired .fd DataObject",
                    oldModelObject, targetModelObject);
            assertDesignerPair(
                    designerType,
                    modelType,
                    new Pair(targetDart, targetModel),
                    targetDartObject,
                    targetModelObject);
            assertFalse("Fresh Dart owner unexpectedly enables generic one-file Move",
                    targetDartObject.isMoveAllowed());
            assertFalse("Fresh .fd owner unexpectedly enables generic one-file Move",
                    targetModelObject.isMoveAllowed());

            assertNull("The consumed Cut payload still exposes pair Move",
                    pairMovePasteType(
                            destinationFolderNode.getPasteTypes(consumed)));
            assertNull("The stale original Cut payload exposes a second pair Move",
                    pairMovePasteType(
                            destinationFolderNode.getPasteTypes(clipboard)));
        }

        private PasteType pairMovePasteType(PasteType[] pasteTypes) {
            PasteType found = null;
            for (PasteType pasteType : pasteTypes) {
                if ("Move Flutter Designer Form".equals(pasteType.getName())) {
                    assertNull("The folder node exposed duplicate pair Move PasteTypes",
                            found);
                    found = pasteType;
                }
            }
            return found;
        }

        private Object decodeDesignerDocument(
                FileObject model,
                ClassLoader loader) throws Exception {
            Class<?> codecType = Class.forName(
                    "dev.flutter.netbeans.designer.codec.FdDocumentCodec",
                    true,
                    loader);
            Object codec = codecType.getConstructor().newInstance();
            Object decoded = codecType.getMethod("decode", byte[].class)
                    .invoke(codec, (Object) model.asBytes());
            assertEquals("The copied .fd file is not a current canonical model",
                    "Current", decoded.getClass().getSimpleName());
            assertEquals("Pair Copy unexpectedly migrated the current source model",
                    Boolean.FALSE,
                    decoded.getClass().getMethod("migrated").invoke(decoded));
            return decoded.getClass().getMethod("document").invoke(decoded);
        }

        private void assertCopiedDocumentSemantics(
                Object original,
                Object target,
                String expectedDartFile) throws Exception {
            assertEquals("Pair Copy changed the schema reference",
                    property(original, "schemaReference"),
                    property(target, "schemaReference"));
            assertEquals("Pair Copy changed the schema version",
                    property(original, "schemaVersion"),
                    property(target, "schemaVersion"));
            assertEquals("Pair Copy changed the format",
                    property(original, "format"), property(target, "format"));
            assertFalse("Pair Copy reused the source documentId",
                    property(original, "documentId")
                            .equals(property(target, "documentId")));
            assertEquals("Pair Copy changed the canvas preferences",
                    property(original, "canvas"), property(target, "canvas"));
            assertEquals("Pair Copy changed the widget tree or its stable IDs",
                    property(original, "root"), property(target, "root"));
            assertEquals("Pair Copy changed document extensions",
                    property(original, "extensions"),
                    property(target, "extensions"));

            Object originalSource = property(original, "source");
            Object targetSource = property(target, "source");
            assertEquals("Pair Copy did not retarget source.dartFile",
                    expectedDartFile, property(targetSource, "dartFile"));
            assertEquals("Pair Copy changed source.className",
                    property(originalSource, "className"),
                    property(targetSource, "className"));
            assertEquals("Pair Copy changed source.widgetKind",
                    property(originalSource, "widgetKind"),
                    property(targetSource, "widgetKind"));
            assertEquals("Pair Copy changed source.generatorVersion",
                    property(originalSource, "generatorVersion"),
                    property(targetSource, "generatorVersion"));
            assertEquals("Pair Copy changed source.managedRegions",
                    property(originalSource, "managedRegions"),
                    property(targetSource, "managedRegions"));
        }

        private Object property(Object owner, String name) throws Exception {
            return owner.getClass().getMethod(name).invoke(owner);
        }

        private void assertUnsavedOrphanIsNotUpgraded(Path root, Class<?> designerType)
                throws Exception {
            String diskSource = "void main() {}\n";
            String unsavedEdit = "// unsaved user edit\n";
            FileObject dart = createFile(root.resolve("lib/buffered.dart"), diskSource);
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

                FileObject model = createFile(root.resolve(".fd_templates/buffered.fd"), """
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
            AtomicReference<UndoRedo> sharedUndoRedo = new AtomicReference<>();
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
                    try {
                        assertPackagedMultiViewUndoIdentity(dataObject);
                    } catch (ReflectiveOperationException ex) {
                        throw new AssertionError(
                                "The packaged Design/Source Undo/Redo identity could not be inspected",
                                ex);
                    }
                    UndoRedo multiViewUndoRedo = multiView.getUndoRedo();
                    assertNotSame("The MultiView must expose a real paired Undo/Redo owner",
                            UndoRedo.NONE, multiViewUndoRedo);

                    MultiViewPerspective sourcePerspective = designerPerspectives.get(1);
                    handler.requestVisible(sourcePerspective);
                    assertPerspective(handler.getSelectedPerspective(),
                            "Source", "flutter.designer.source");
                    sharedUndoRedo.set(multiViewUndoRedo);

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
                assertSourceSavepointLifecycle(
                        dataObject,
                        pair,
                        pairedEditor,
                        pairedDocument,
                        openedMultiView.get(),
                        sharedUndoRedo.get());
            } finally {
                CloneableTopComponent multiView = openedMultiView.get();
                if (multiView != null) {
                    SwingUtilities.invokeAndWait(() ->
                            assertTrue("The designer MultiView did not close", multiView.close()));
                }
                pairedEditor.close();
            }
        }

        private void assertSourceSavepointLifecycle(
                DataObject dataObject,
                Pair pair,
                CloneableEditorSupport editor,
                StyledDocument document,
                CloneableTopComponent multiView,
                UndoRedo undoRedo) throws Exception {
            assertNotNull("The opened MultiView did not publish its Undo/Redo owner",
                    undoRedo);
            String cleanDocumentSource = document.getText(0, document.getLength());
            String cleanDiskSource = pair.dart().asText(StandardCharsets.UTF_8.name());
            byte[] cleanFd = pair.fd().asBytes();
            String sourceEdit = "// assembled-runtime source savepoint edit\n";
            String editedDocumentSource = cleanDocumentSource + sourceEdit;
            String editedDiskSource = cleanDiskSource + sourceEdit;

            SwingUtilities.invokeAndWait(() -> {
                try {
                    NbDocument.runAtomicAsUser(document, () -> {
                        try {
                            document.insertString(document.getLength(), sourceEdit, null);
                        } catch (BadLocationException ex) {
                            throw new AssertionError(ex);
                        }
                    });
                } catch (BadLocationException ex) {
                    throw new AssertionError(ex);
                }
            });

            assertEquals("The public Source edit was not applied exactly",
                    editedDocumentSource, document.getText(0, document.getLength()));
            assertTrue("A public Source edit did not mark the paired DataObject dirty",
                    dataObject.isModified());
            assertTrue("A public Source edit did not mark CES dirty", editor.isModified());
            SaveCookie stableSave = dataObject.getLookup().lookup(SaveCookie.class);
            assertNotNull("A public Source edit did not publish the pair SaveCookie",
                    stableSave);
            assertTrue("The shared Undo/Redo owner cannot undo the public Source edit",
                    undoRedo.canUndo());

            stableSave.save();

            assertFalse("Source Save left the paired DataObject dirty",
                    dataObject.isModified());
            assertFalse("Source Save left CES dirty", editor.isModified());
            assertNull("Source Save left a SaveCookie at the native savepoint",
                    dataObject.getLookup().lookup(SaveCookie.class));
            assertEquals("Source Save did not persist the exact public edit",
                    editedDiskSource, pair.dart().asText(StandardCharsets.UTF_8.name()));
            assertTrue("Source Save changed the canonical .fd bytes",
                    java.util.Arrays.equals(cleanFd, pair.fd().asBytes()));
            assertTrue("Source Save discarded the native Undo history",
                    undoRedo.canUndo());

            SwingUtilities.invokeAndWait(() -> {
                requestVisible(multiView, "Design", "flutter.designer.design");
                assertTrue("Design cannot see the saved Source edit in shared Undo/Redo",
                        undoRedo.canUndo());
                undoRedo.undo();
            });

            assertEquals("Undo did not restore the exact pre-save Source bytes",
                    cleanDocumentSource, document.getText(0, document.getLength()));
            assertTrue("Undo away from the native savepoint did not dirty the pair",
                    dataObject.isModified());
            assertTrue("Undo away from the native savepoint did not dirty CES",
                    editor.isModified());
            assertSame("Undo published a different pair SaveCookie instance",
                    stableSave, dataObject.getLookup().lookup(SaveCookie.class));
            assertTrue("Undo did not expose the matching native Redo",
                    undoRedo.canRedo());
            assertEquals("Undo unexpectedly rewrote the durable Dart source",
                    editedDiskSource, pair.dart().asText(StandardCharsets.UTF_8.name()));
            assertTrue("Undo unexpectedly changed the canonical .fd bytes",
                    java.util.Arrays.equals(cleanFd, pair.fd().asBytes()));

            SwingUtilities.invokeAndWait(() -> {
                requestVisible(multiView, "Source", "flutter.designer.source");
                assertTrue("Source cannot see the Redo exposed after Design Undo",
                        undoRedo.canRedo());
                undoRedo.redo();
            });

            assertEquals("Redo did not restore the exact saved Source bytes",
                    editedDocumentSource, document.getText(0, document.getLength()));
            assertFalse("Redo back to the native savepoint left the pair dirty",
                    dataObject.isModified());
            assertFalse("Redo back to the native savepoint left CES dirty",
                    editor.isModified());
            assertNull("Redo back to the native savepoint left a SaveCookie",
                    dataObject.getLookup().lookup(SaveCookie.class));
            assertEquals("Redo unexpectedly rewrote the durable Dart source",
                    editedDiskSource, pair.dart().asText(StandardCharsets.UTF_8.name()));
            assertTrue("Redo unexpectedly changed the canonical .fd bytes",
                    java.util.Arrays.equals(cleanFd, pair.fd().asBytes()));
        }

        private void assertPackagedMultiViewUndoIdentity(DataObject dataObject)
                throws ReflectiveOperationException {
            ClassLoader loader = flutterModule().getClassLoader();
            Class<?> designType = Class.forName(
                    "dev.flutter.netbeans.plugin.designer.FlutterDesignerMultiViewDesign",
                    true,
                    loader);
            Class<?> sourceType = Class.forName(
                    "dev.flutter.netbeans.plugin.designer.FlutterDesignerMultiViewSource",
                    true,
                    loader);
            Object design = designType.getConstructor(Lookup.class)
                    .newInstance(dataObject.getLookup());
            Object source = sourceType.getConstructor(Lookup.class)
                    .newInstance(dataObject.getLookup());
            try {
                UndoRedo designUndoRedo = (UndoRedo) designType.getMethod("getUndoRedo")
                        .invoke(design);
                UndoRedo sourceUndoRedo = (UndoRedo) sourceType.getMethod("getUndoRedo")
                        .invoke(source);

                assertNotSame("Packaged Design exposed no paired Undo/Redo owner",
                        UndoRedo.NONE, designUndoRedo);
                assertSame("Packaged Design and Source exposed different Undo/Redo identities",
                        designUndoRedo, sourceUndoRedo);
                Container toolbar = (Container) designType
                        .getMethod("getToolbarRepresentation")
                        .invoke(design);
                assertDesktopOnlyPreview(toolbar);
            } finally {
                designType.getMethod("componentClosed").invoke(design);
            }
        }

        private void assertDesktopOnlyPreview(Container toolbar) {
            JComboBox<?> previews = findNamedComponent(
                    toolbar,
                    JComboBox.class,
                    "Flutter Canvas preview target");
            assertEquals("A Windows-only Flutter project must expose one preview",
                    1, previews.getItemCount());
            assertEquals("The Windows-only Flutter project exposed a non-desktop preview",
                    "Windows Desktop", previews.getItemAt(0).toString());
            assertEquals("The Windows-only Flutter project did not select Desktop",
                    "Windows Desktop", previews.getSelectedItem().toString());
        }

        private <T extends Component> T findNamedComponent(
                Container root,
                Class<T> type,
                String accessibleName) {
            for (Component component : root.getComponents()) {
                AccessibleContext accessible = component.getAccessibleContext();
                if (type.isInstance(component)
                        && accessible != null
                        && accessibleName.equals(accessible.getAccessibleName())) {
                    return type.cast(component);
                }
                if (component instanceof Container child) {
                    T found = findNamedComponentOrNull(child, type, accessibleName);
                    if (found != null) {
                        return found;
                    }
                }
            }
            throw new AssertionError("No " + type.getSimpleName()
                    + " named '" + accessibleName + "' in the opened Designer MultiView");
        }

        private <T extends Component> T findNamedComponentOrNull(
                Container root,
                Class<T> type,
                String accessibleName) {
            for (Component component : root.getComponents()) {
                AccessibleContext accessible = component.getAccessibleContext();
                if (type.isInstance(component)
                        && accessible != null
                        && accessibleName.equals(accessible.getAccessibleName())) {
                    return type.cast(component);
                }
                if (component instanceof Container child) {
                    T found = findNamedComponentOrNull(child, type, accessibleName);
                    if (found != null) {
                        return found;
                    }
                }
            }
            return null;
        }

        private void requestVisible(
                CloneableTopComponent multiView,
                String displayName,
                String preferredId) {
            MultiViewHandler handler = MultiViews.findMultiViewHandler(multiView);
            assertNotNull("The opened designer has no MultiViewHandler", handler);
            MultiViewPerspective perspective = java.util.Arrays.stream(handler.getPerspectives())
                    .filter(candidate -> preferredId.equals(candidate.preferredID()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "Missing MultiView perspective " + preferredId));
            handler.requestVisible(perspective);
            assertPerspective(handler.getSelectedPerspective(), displayName, preferredId);
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
                Class<?> modelType,
                Pair pair,
                DataObject dartObject,
                DataObject modelObject) throws Exception {
            assertEquals("The .fd sidecar must resolve to the dedicated designer MIME",
                    DESIGNER_MIME, FileUtil.getMIMEType(pair.fd()));
            assertEquals("The technical primary must retain the Dart MIME type",
                    DART_MIME, FileUtil.getMIMEType(pair.dart()));
            assertNotSame("The visible .fd model must not be hidden as a Dart secondary entry",
                    dartObject, modelObject);
            assertTrue("The Dart editing session is not owned by FlutterDesignerDataObject: "
                    + dartObject.getClass().getName(), designerType.isInstance(dartObject));
            assertTrue("The .fd file has no visible FlutterDesignerModelDataObject: "
                    + modelObject.getClass().getName(), modelType.isInstance(modelObject));
            assertEquals("The .dart file must be the technical primary",
                    pair.dart(), dartObject.getPrimaryFile());
            assertEquals("The .fd file must remain a physical primary visible in Files",
                    pair.fd(), modelObject.getPrimaryFile());
            assertEquals("The Dart editing owner must not hide the model as a secondary entry",
                    Set.of(pair.dart()), dartObject.files());
            assertEquals("The visible model object must own exactly its physical .fd file",
                    Set.of(pair.fd()), modelObject.files());
            assertNotNull("The visible .fd model has no delegated Open action",
                    modelObject.getLookup().lookup(OpenCookie.class));
            DataFolder physicalFolder = DataFolder.findFolder(pair.fd().getParent());
            awaitPhysicalModelChild(physicalFolder, modelObject);
            assertPhysicalModelNode(physicalFolder, modelObject);
            assertTrue("The Dart owner must expose shared paired Rename",
                    dartObject.isRenameAllowed());
            assertTrue("The .fd owner must expose the same shared paired Rename",
                    modelObject.isRenameAllowed());
            assertFalse("Unsafe pair copy must remain disabled in the foundation slice",
                    dartObject.isCopyAllowed());
            assertFalse("Unsafe pair move must remain disabled in the foundation slice",
                    dartObject.isMoveAllowed());
            assertTrue("The Dart node must expose the shared paired Delete operation",
                    dartObject.isDeleteAllowed());
            assertTrue("The .fd node must expose the same shared paired Delete operation",
                    modelObject.isDeleteAllowed());
            assertTrue("The Dart node must publish NetBeans Delete",
                    dartObject.getNodeDelegate().canDestroy());
            assertTrue("The .fd node must publish NetBeans Delete",
                    modelObject.getNodeDelegate().canDestroy());
            assertTrue("The Dart node must publish NetBeans Rename",
                    dartObject.getNodeDelegate().canRename());
            assertTrue("The .fd node must publish NetBeans Rename",
                    modelObject.getNodeDelegate().canRename());
            assertTrue("The Dart node must publish paired NetBeans Cut",
                    dartObject.getNodeDelegate().canCut());
            assertTrue("The .fd node must publish paired NetBeans Cut",
                    modelObject.getNodeDelegate().canCut());
            assertNoWritableExtensionProperty(
                    dartObject.getNodeDelegate(), "Dart");
            assertNoWritableExtensionProperty(
                    modelObject.getNodeDelegate(), ".fd");
        }

        /**
         * Checks the real NetBeans Node icons and their originating resources.
         * Resource URLs avoid brittle comparisons of rendered SVG pixels while
         * still proving that Dart and Flutter Designer files stay visually distinct.
         */
        private void assertFileTypeIcons(
                DataObject ordinaryDart,
                DataObject pairedDart,
                DataObject designerModel) {
            Image ordinaryIcon = fileIcon(ordinaryDart, "ordinary Dart");
            Image pairedIcon = fileIcon(pairedDart, "paired Dart");
            Image designerIcon = fileIcon(designerModel, "Flutter Designer model");

            URL ordinaryResource = ImageUtilities.findImageBaseURL(ordinaryIcon);
            URL pairedResource = ImageUtilities.findImageBaseURL(pairedIcon);
            URL designerResource = ImageUtilities.findImageBaseURL(designerIcon);
            assertNotNull("The ordinary Dart node icon has no source resource URL",
                    ordinaryResource);
            assertNotNull("The paired Dart node icon has no source resource URL",
                    pairedResource);
            assertNotNull("The Flutter Designer node icon has no source resource URL",
                    designerResource);
            assertEquals("Ordinary and paired Dart nodes must use the same Dart file icon",
                    ordinaryResource, pairedResource);
            assertFalse("Dart and Flutter Designer nodes unexpectedly use the same file icon",
                    ordinaryResource.equals(designerResource));
        }

        private Image fileIcon(DataObject dataObject, String description) {
            Image icon = dataObject.getNodeDelegate()
                    .getIcon(BeanInfo.ICON_COLOR_16x16);
            assertNotNull("The " + description + " node has no 16x16 color icon", icon);
            return icon;
        }

        /**
         * FolderList processes file-created events on its request processor.
         * DataFolder.getChildren() is therefore allowed to expose the previous
         * snapshot briefly when another model is added to an already expanded
         * .fd_templates folder. Wait for that documented asynchronous edge;
         * never make production DataObject recognition block on UI refresh.
         */
        private void awaitPhysicalModelChild(
                DataFolder physicalFolder,
                DataObject modelObject) throws Exception {
            long deadline = System.nanoTime()
                    + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
            DataObject[] children;
            do {
                children = physicalFolder.getChildren();
                if (java.util.Arrays.asList(children).contains(modelObject)) {
                    return;
                }
                Thread.sleep(20);
            } while (System.nanoTime() < deadline);
            fail("The physical .fd model did not enter its Files folder after "
                    + "NetBeans finished processing file-created events. Children: "
                    + "modelValid=" + modelObject.isValid()
                    + ", modelPrimary=" + modelObject.getPrimaryFile().getPath()
                    + ", folderPrimary=" + physicalFolder.getPrimaryFile().getPath()
                    + ", children="
                    + java.util.Arrays.stream(children)
                            .map(child -> child.getPrimaryFile().getPath()
                                    + " [" + child.getClass().getName()
                                    + ", valid=" + child.isValid() + "]")
                            .collect(Collectors.joining(", ")));
        }

        /** Verifies the same Node path used by the Files view, not only DataFolder ownership. */
        private void assertPhysicalModelNode(
                DataFolder physicalFolder,
                DataObject modelObject) {
            Node[] nodes = physicalFolder.getNodeDelegate()
                    .getChildren()
                    .getNodes(true);
            assertTrue("The physical .fd model has no node in the NetBeans Files view",
                    java.util.Arrays.stream(nodes).anyMatch(node ->
                            modelObject.equals(node.getLookup().lookup(DataObject.class))));
        }

        private Pair createPair(Path directory, String baseName) throws Exception {
            FileObject dart = createFile(directory.resolve("lib/" + baseName + ".dart"), """
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
            FileObject fd = createFile(directory.resolve(
                    ".fd_templates/" + baseName + ".fd"), """
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
            Files.createDirectories(path.getParent());
            Files.writeString(path, content, StandardCharsets.UTF_8);
            FileUtil.refreshFor(path.toFile());
            FileObject file = FileUtil.toFileObject(path.toFile());
            assertNotNull("No FileObject for " + path, file);
            return file;
        }

        private Path createFlutterProject(Path root) throws Exception {
            Files.createDirectories(root.resolve("lib"));
            Files.createDirectories(root.resolve(".fd_templates"));
            Files.createDirectories(root.resolve(".dart_tool"));
            Files.createDirectories(root.resolve("windows"));
            Files.writeString(root.resolve("pubspec.yaml"), """
                    name: designer_runtime_app
                    dependencies:
                      flutter:
                        sdk: flutter
                    """, StandardCharsets.UTF_8);
            Files.writeString(root.resolve(".dart_tool/package_config.json"), """
                    {
                      "configVersion": 2,
                      "packages": [
                        {
                          "name": "designer_runtime_app",
                          "rootUri": "../",
                          "packageUri": "lib/",
                          "languageVersion": "3.0"
                        }
                      ]
                    }
                    """, StandardCharsets.UTF_8);
            FileUtil.refreshFor(root.toFile());
            FileObject projectDirectory = FileUtil.toFileObject(root.toFile());
            assertNotNull("No FileObject for Flutter project " + root, projectDirectory);
            assertNotNull("Runtime fixture was not recognized as a Flutter project",
                    ProjectManager.getDefault().findProject(projectDirectory));
            return root;
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
