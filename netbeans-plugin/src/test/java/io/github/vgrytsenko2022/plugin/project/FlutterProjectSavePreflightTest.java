package io.github.vgrytsenko2022.plugin.project;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.text.StyledDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.netbeans.api.actions.Savable;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;

class FlutterProjectSavePreflightTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void savesOnlyTheRequestedProjectsOrdinaryDartDocument() throws Exception {
        Path firstRoot = flutterProject("first");
        Path secondRoot = flutterProject("second");
        Path firstPath = firstRoot.resolve("lib/main.dart");
        Path secondPath = secondRoot.resolve("lib/main.dart");
        FileUtil.refreshFor(firstRoot.toFile(), secondRoot.toFile());
        DataObject first = DataObject.find(requireFileObject(firstPath));
        DataObject second = DataObject.find(requireFileObject(secondPath));
        EditorCookie firstEditor = first.getLookup().lookup(EditorCookie.class);
        EditorCookie secondEditor = second.getLookup().lookup(EditorCookie.class);
        assertNotNull(firstEditor);
        assertNotNull(secondEditor);
        StyledDocument firstDocument = firstEditor.openDocument();
        StyledDocument secondDocument = secondEditor.openDocument();
        String firstEdit = "// first project pending Run edit\n";
        String secondEdit = "// unrelated project pending edit\n";
        firstDocument.insertString(firstDocument.getLength(), firstEdit, null);
        secondDocument.insertString(secondDocument.getLength(), secondEdit, null);
        try {
            assertTrue(first.isModified());
            assertTrue(second.isModified());
            assertNotNull(first.getLookup().lookup(Savable.class));

            FlutterProjectSavePreflight.save(firstRoot);

            assertFalse(first.isModified());
            assertTrue(Files.readString(firstPath, StandardCharsets.UTF_8)
                    .contains(firstEdit.trim()));
            assertTrue(second.isModified(),
                    "another project's dirty DataObject must remain untouched");
            assertFalse(Files.readString(secondPath, StandardCharsets.UTF_8)
                    .contains(secondEdit.trim()));
        } finally {
            Savable secondSavable = second.getLookup().lookup(Savable.class);
            if (secondSavable != null) {
                secondSavable.save();
            } else {
                SaveCookie secondSave = second.getLookup().lookup(SaveCookie.class);
                if (secondSave != null) {
                    secondSave.save();
                }
            }
            firstEditor.close();
            secondEditor.close();
        }
    }

    private Path flutterProject(String name) throws Exception {
        Path root = Files.createDirectory(temporaryDirectory.resolve(name));
        Path lib = Files.createDirectory(root.resolve("lib"));
        Files.writeString(root.resolve("pubspec.yaml"),
                "name: " + name + "\n", StandardCharsets.UTF_8);
        Files.writeString(lib.resolve("main.dart"),
                "void main() {}\n", StandardCharsets.UTF_8);
        return root;
    }

    private static FileObject requireFileObject(Path path) {
        FileObject file = FileUtil.toFileObject(path.toFile());
        assertNotNull(file, "NetBeans FileObject missing for " + path);
        return file;
    }
}
