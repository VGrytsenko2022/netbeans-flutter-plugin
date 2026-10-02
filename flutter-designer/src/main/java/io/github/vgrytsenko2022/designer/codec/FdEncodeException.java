package io.github.vgrytsenko2022.designer.codec;

import java.util.Objects;

/** Checked failure to encode a model under the canonical writer contract. */
public final class FdEncodeException extends Exception {
    private final FdCodecDiagnostic diagnostic;

    public FdEncodeException(FdCodecDiagnostic diagnostic) {
        this(diagnostic, null);
    }

    public FdEncodeException(FdCodecDiagnostic diagnostic, Throwable cause) {
        super(Objects.requireNonNull(diagnostic, "diagnostic").message(), cause);
        this.diagnostic = diagnostic;
    }

    public FdCodecDiagnostic diagnostic() {
        return diagnostic;
    }
}
