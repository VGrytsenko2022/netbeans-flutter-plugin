package io.github.vgrytsenko2022.designer.canvas;

/** Reviewed compressed image formats accepted by the Designer asset bridge. */
public enum CanvasImageFormat {
    PNG("image/png"),
    JPEG("image/jpeg"),
    GIF("image/gif"),
    WEBP("image/webp");

    private final String mediaType;

    CanvasImageFormat(String mediaType) {
        this.mediaType = mediaType;
    }

    public String mediaType() {
        return mediaType;
    }
}
