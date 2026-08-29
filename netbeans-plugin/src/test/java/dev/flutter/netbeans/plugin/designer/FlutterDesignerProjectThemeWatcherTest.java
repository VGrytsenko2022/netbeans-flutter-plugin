package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;

class FlutterDesignerProjectThemeWatcherTest {

    @Test
    void pathFilterIsBoundedToThemePairAndItsAtomicTemporaryFiles() {
        assertTrue(FlutterDesignerProjectThemeWatcher.affectsTheme(
                ".fd_templates/project.fdtheme"));
        assertTrue(FlutterDesignerProjectThemeWatcher.affectsTheme(
                "lib/theme/app_theme.dart"));
        assertTrue(FlutterDesignerProjectThemeWatcher.affectsTheme(
                ".fd_templates/.project.fdtheme.123.tmp"));
        assertTrue(FlutterDesignerProjectThemeWatcher.affectsTheme(
                "lib/theme/.app_theme.dart.123.tmp"));

        assertFalse(FlutterDesignerProjectThemeWatcher.affectsTheme(
                ".fd_templates/screens/home.fd"));
        assertFalse(FlutterDesignerProjectThemeWatcher.affectsTheme(
                "lib/main.dart"));
        assertFalse(FlutterDesignerProjectThemeWatcher.affectsTheme(
                "lib/theme/custom_theme.dart"));
    }

    @Test
    void recursiveWatcherRefreshesForBothThemeFilesAndStopsAfterClose()
            throws Exception {
        FileSystem memory = FileUtil.createMemoryFileSystem();
        FileObject root = memory.getRoot();
        FileObject templates = FileUtil.createFolder(root, ".fd_templates");
        FileObject forms = FileUtil.createFolder(templates, "screens");
        FileObject themeFolder = FileUtil.createFolder(root, "lib/theme");
        CountingScheduler scheduler = new CountingScheduler();
        FlutterDesignerProjectThemeWatcher watcher =
                new FlutterDesignerProjectThemeWatcher(root, scheduler);
        watcher.open();

        write(forms.createData("home.fd"), new byte[]{1});
        assertEquals(0, scheduler.requests.get(),
                "ordinary .fd edits must not invalidate the project theme");

        FileObject descriptor = templates.createData("project.fdtheme");
        write(descriptor, new byte[]{2});
        int afterDescriptor = scheduler.requests.get();
        assertTrue(afterDescriptor > 0);

        FileObject dart = themeFolder.createData("app_theme.dart");
        write(dart, new byte[]{3});
        assertTrue(scheduler.requests.get() > afterDescriptor);

        int beforeClose = scheduler.requests.get();
        watcher.close();
        write(descriptor, new byte[]{4});
        assertEquals(beforeClose, scheduler.requests.get());
        assertTrue(scheduler.closed);
    }

    private static void write(FileObject file, byte[] bytes) throws Exception {
        try (OutputStream output = file.getOutputStream()) {
            output.write(bytes);
        }
    }

    private static final class CountingScheduler
            implements FlutterDesignerProjectThemeWatcher.RefreshScheduler {
        private final AtomicInteger requests = new AtomicInteger();
        private boolean closed;

        @Override
        public void request() {
            requests.incrementAndGet();
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
