package dev.flutter.netbeans.designer.model;

import java.util.Objects;
import java.util.Optional;

/** Immutable, current-version semantic representation of one {@code .fd} document. */
public record DesignerDocument(
        Optional<String> schemaReference,
        StableId documentId,
        DartSourceDescriptor source,
        Optional<CanvasPreferences> canvas,
        WidgetNode root,
        Extensions extensions) {

    public static final String FORMAT = "netbeans-flutter-designer";
    public static final int SCHEMA_VERSION = 8;

    public DesignerDocument {
        Objects.requireNonNull(schemaReference, "schemaReference");
        Objects.requireNonNull(documentId, "documentId");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(canvas, "canvas");
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(extensions, "extensions");
    }

    public DesignerDocument(
            StableId documentId,
            DartSourceDescriptor source,
            WidgetNode root) {
        this(Optional.empty(), documentId, source, Optional.empty(), root, Extensions.empty());
    }

    public String format() {
        return FORMAT;
    }

    public int schemaVersion() {
        return SCHEMA_VERSION;
    }
}
