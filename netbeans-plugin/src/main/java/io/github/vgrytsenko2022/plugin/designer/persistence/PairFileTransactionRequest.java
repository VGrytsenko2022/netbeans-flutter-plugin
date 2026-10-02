package io.github.vgrytsenko2022.plugin.designer.persistence;

import java.util.Objects;
import org.openide.filesystems.FileObject;

/**
 * Exact old and new byte snapshots for one Dart/designer-file commit attempt.
 * Arrays are defensively copied on input and output.
 */
public final class PairFileTransactionRequest {
    private final FileObject dartFile;
    private final FileObject designerFile;
    private final byte[] expectedDartBytes;
    private final byte[] expectedDesignerBytes;
    private final byte[] newDartBytes;
    private final byte[] newDesignerBytes;

    public PairFileTransactionRequest(
            FileObject dartFile,
            FileObject designerFile,
            byte[] expectedDartBytes,
            byte[] expectedDesignerBytes,
            byte[] newDartBytes,
            byte[] newDesignerBytes) {
        this.dartFile = Objects.requireNonNull(dartFile, "dartFile");
        this.designerFile = Objects.requireNonNull(designerFile, "designerFile");
        this.expectedDartBytes = copy(expectedDartBytes, "expectedDartBytes");
        this.expectedDesignerBytes = copy(expectedDesignerBytes, "expectedDesignerBytes");
        this.newDartBytes = copy(newDartBytes, "newDartBytes");
        this.newDesignerBytes = copy(newDesignerBytes, "newDesignerBytes");
    }

    public FileObject dartFile() {
        return dartFile;
    }

    public FileObject designerFile() {
        return designerFile;
    }

    public byte[] expectedDartBytes() {
        return expectedDartBytes.clone();
    }

    public byte[] expectedDesignerBytes() {
        return expectedDesignerBytes.clone();
    }

    public byte[] newDartBytes() {
        return newDartBytes.clone();
    }

    public byte[] newDesignerBytes() {
        return newDesignerBytes.clone();
    }

    private static byte[] copy(byte[] bytes, String name) {
        return Objects.requireNonNull(bytes, name).clone();
    }
}
