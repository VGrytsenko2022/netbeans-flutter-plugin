package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;

/** Fast pairing rules independent of the NetBeans runtime container. */
class FlutterDesignerDataLoaderTest {
    private static final FlutterDesignerDataLoader LOADER =
            org.openide.loaders.DataLoader.getLoader(FlutterDesignerDataLoader.class);

    @Test
    void resolvesBothMembersToTheDartTechnicalPrimary() throws Exception {
        FileSystem fileSystem = FileUtil.createMemoryFileSystem();
        FileObject folder = fileSystem.getRoot().createFolder("lib");
        FileObject dart = folder.createData("home_page.dart");
        FileObject model = folder.createData("home_page.fd");

        assertSame(dart, LOADER.findPrimaryFile(dart));
        assertSame(dart, LOADER.findPrimaryFile(model));
    }

    @Test
    void doesNotClaimOrphanOrMismatchedFiles() throws Exception {
        FileSystem fileSystem = FileUtil.createMemoryFileSystem();
        FileObject folder = fileSystem.getRoot().createFolder("lib");
        FileObject orphanDart = folder.createData("ordinary.dart");
        FileObject orphanModel = folder.createData("model_only.fd");
        FileObject mismatchedDart = folder.createData("account.dart");
        FileObject mismatchedModel = folder.createData("profile.fd");
        FileObject unrelated = folder.createData("account.json");

        assertNull(LOADER.findPrimaryFile(orphanDart));
        assertNull(LOADER.findPrimaryFile(orphanModel));
        assertNull(LOADER.findPrimaryFile(mismatchedDart));
        assertNull(LOADER.findPrimaryFile(mismatchedModel));
        assertNull(LOADER.findPrimaryFile(unrelated));
        assertNull(LOADER.findPrimaryFile(folder));
        assertNull(LOADER.findPrimaryFile(null));
    }

    @Test
    void neverPairsFilesFromDifferentDirectories() throws Exception {
        FileSystem fileSystem = FileUtil.createMemoryFileSystem();
        FileObject first = fileSystem.getRoot().createFolder("first");
        FileObject second = fileSystem.getRoot().createFolder("second");
        FileObject dart = first.createData("shared.dart");
        FileObject model = second.createData("shared.fd");

        assertNull(LOADER.findPrimaryFile(dart));
        assertNull(LOADER.findPrimaryFile(model));
    }

    @Test
    void requiresAnExactBasenameMatch() throws Exception {
        FileSystem fileSystem = FileUtil.createMemoryFileSystem();
        FileObject folder = fileSystem.getRoot().createFolder("lib");
        FileObject lowerDart = folder.createData("home_page.dart");
        FileObject upperModel = folder.createData("Home_Page.fd");

        assertNull(LOADER.findPrimaryFile(lowerDart));
        assertNull(LOADER.findPrimaryFile(upperModel));
    }
}
