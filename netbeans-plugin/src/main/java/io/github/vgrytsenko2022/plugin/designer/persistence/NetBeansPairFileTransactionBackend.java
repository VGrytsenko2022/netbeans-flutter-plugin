package io.github.vgrytsenko2022.plugin.designer.persistence;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Objects;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;

/** Production NetBeans file-system adapter. */
final class NetBeansPairFileTransactionBackend
        implements PairFileTransactionBackend {

    @Override
    public Path localPath(FileObject file) throws IOException {
        File local = FileUtil.toFile(file);
        if (local == null) {
            return null;
        }
        return local.getCanonicalFile().toPath();
    }

    @Override
    public FileLock lock(FileObject file) throws IOException {
        return file.lock();
    }

    @Override
    public byte[] read(FileObject file) throws IOException {
        return file.asBytes();
    }

    @Override
    public void write(FileObject file, FileLock lock, byte[] bytes)
            throws IOException {
        Objects.requireNonNull(lock, "lock");
        try (OutputStream output = file.getOutputStream(lock)) {
            output.write(bytes);
            output.flush();
        }
    }

    @Override
    public void runAtomicAction(
            FileObject stableFirst,
            FileObject stableSecond,
            FileSystem.AtomicAction action) throws IOException {
        FileSystem firstSystem = stableFirst.getFileSystem();
        FileSystem secondSystem = stableSecond.getFileSystem();
        if (firstSystem == secondSystem) {
            firstSystem.runAtomicAction(action);
            return;
        }
        firstSystem.runAtomicAction(
                () -> secondSystem.runAtomicAction(action));
    }
}
