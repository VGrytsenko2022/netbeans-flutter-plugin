package dev.flutter.netbeans.designer.template;

import dev.flutter.netbeans.designer.model.DesignerDocument;
import java.util.Arrays;
import java.util.Objects;

/** Exact initial Dart/{@code .fd} bytes for one new Flutter Designer form. */
public final class DesignerFormTemplate {
    private final DesignerDocument document;
    private final byte[] dartBytes;
    private final byte[] fdBytes;

    DesignerFormTemplate(
            DesignerDocument document,
            byte[] dartBytes,
            byte[] fdBytes) {
        this.document = Objects.requireNonNull(document, "document");
        this.dartBytes = Objects.requireNonNull(dartBytes, "dartBytes").clone();
        this.fdBytes = Objects.requireNonNull(fdBytes, "fdBytes").clone();
    }

    public DesignerDocument document() {
        return document;
    }

    public byte[] dartBytes() {
        return dartBytes.clone();
    }

    public byte[] fdBytes() {
        return fdBytes.clone();
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof DesignerFormTemplate that
                && document.equals(that.document)
                && Arrays.equals(dartBytes, that.dartBytes)
                && Arrays.equals(fdBytes, that.fdBytes);
    }

    @Override
    public int hashCode() {
        int result = document.hashCode();
        result = 31 * result + Arrays.hashCode(dartBytes);
        return 31 * result + Arrays.hashCode(fdBytes);
    }
}
