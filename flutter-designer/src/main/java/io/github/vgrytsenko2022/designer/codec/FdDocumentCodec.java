package io.github.vgrytsenko2022.designer.codec;

import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import java.util.Objects;

/** Public facade for bounded version dispatch and canonical {@code .fd} encoding. */
public final class FdDocumentCodec {
    private final FdCodecLimits limits;
    private final FdJsonDecoder decoder;
    private final FdJsonEncoder encoder;

    public FdDocumentCodec() {
        this(FdCodecLimits.defaults());
    }

    public FdDocumentCodec(FdCodecLimits limits) {
        this.limits = Objects.requireNonNull(limits, "limits");
        this.decoder = new FdJsonDecoder(limits);
        this.encoder = new FdJsonEncoder(limits);
    }

    public FdCodecLimits limits() {
        return limits;
    }

    /** Checks the raw size before taking the immutable byte snapshot. */
    public FdDecodeResult decode(byte[] bytes) throws FdInputLimitException {
        return decode(OriginalFdBytes.copyOf(bytes, limits));
    }

    /** Never throws for bounded user content; failures are returned as {@link FdDecodeResult.Invalid}. */
    public FdDecodeResult decode(OriginalFdBytes original) {
        return decoder.decode(Objects.requireNonNull(original, "original"));
    }

    /** Encodes current model data using canonical UTF-8 JSON with one final LF. */
    public OriginalFdBytes encode(DesignerDocument document) throws FdEncodeException {
        return encoder.encode(Objects.requireNonNull(document, "document"));
    }
}
