package io.github.vgrytsenko2022.plugin.designer.persistence;

import java.io.IOException;
import java.nio.file.Path;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;

/** Package-private seam for deterministic I/O-failure and ordering tests. */
interface PairFileTransactionBackend {
    Path localPath(FileObject file) throws IOException;

    FileLock lock(FileObject file) throws IOException;

    byte[] read(FileObject file) throws IOException;

    void write(FileObject file, FileLock lock, byte[] bytes) throws IOException;

    void runAtomicAction(
            FileObject stableFirst,
            FileObject stableSecond,
            FileSystem.AtomicAction action) throws IOException;
}
